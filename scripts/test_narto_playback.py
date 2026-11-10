#!/usr/bin/env python3
"""Probe a live Narto episode through the app's URL parsing and source ordering.

Usage:
  python3 scripts/test_narto_playback.py
  python3 scripts/test_narto_playback.py --episode-url 'https://edge.narto-drama.com/detail/watch/slug/1?lang=en-US'

Uses the system ffmpeg/ffprobe, writes output only under a temporary directory, and
never prints signed media URLs or their query tokens.
"""

from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request
from pathlib import Path


DEFAULT_PAGE = "https://edge.narto-drama.com/detail/watch/our-temporary-embrace/1?lang=en-US"
APP_USER_AGENT = (
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
)
PLAYER_FIELDS = ("direct_play_url", "play_url", "schema_content_url")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--episode-url", default=DEFAULT_PAGE, help="Narto episode page URL")
    parser.add_argument("--episode", type=int, default=1, help="route_episode_number to probe")
    parser.add_argument("--seconds", type=float, default=8, help="maximum sample duration")
    parser.add_argument("--timeout", type=int, default=90, help="per-source FFmpeg timeout in seconds")
    return parser.parse_args()


def fetch_page(url: str) -> str:
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": APP_USER_AGENT,
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
            "Referer": urllib.parse.urlunsplit((*urllib.parse.urlsplit(url)[:2], "/", "", "")),
        },
    )
    with urllib.request.urlopen(request, timeout=35) as response:
        if response.status != 200:
            raise RuntimeError(f"Episode page returned HTTP {response.status}")
        return response.read(12 * 1024 * 1024 + 1).decode("utf-8", errors="replace")


def episode_entry(html: str, number: int) -> dict:
    marker = re.search(r"const\s+episodeItemsRaw\s*=\s*", html)
    if not marker:
        raise RuntimeError("Page did not expose episodeItemsRaw")
    try:
        items, _ = json.JSONDecoder().raw_decode(html[marker.end() :])
    except json.JSONDecodeError as error:
        raise RuntimeError("Could not parse episodeItemsRaw JSON") from error
    if not isinstance(items, list):
        raise RuntimeError("episodeItemsRaw is not an array")
    for item in items:
        if isinstance(item, dict) and int(item.get("route_episode_number", -1)) == number:
            return item
    raise RuntimeError(f"Episode {number} is absent from episodeItemsRaw")


def is_hls(url: str, field: str, entry: dict) -> bool:
    parsed = urllib.parse.urlsplit(url)
    return (
        parsed.path.lower().endswith(".m3u8")
        or (field == "play_url" and entry.get("browser_prefetch_mode") == "hls")
        or (field == "direct_play_url" and entry.get("direct_play_is_hls") is True)
    )


def candidates_for(entry: dict, page_url: str) -> list[dict]:
    candidates = []
    for field in PLAYER_FIELDS:  # Same candidate order as parseNartoVariants().
        raw = entry.get(field)
        if not isinstance(raw, str) or not raw.strip():
            continue
        url = urllib.parse.urljoin(page_url, raw.strip())
        parsed = urllib.parse.urlsplit(url)
        if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
            continue
        candidates.append({
            "field": field,
            "url": url,
            "host": parsed.hostname,
            "hls": is_hls(url, field, entry),
        })
    distinct = []
    seen = set()
    for candidate in candidates:
        if candidate["url"] not in seen:
            seen.add(candidate["url"])
            distinct.append(candidate)
    return distinct


def safe_stderr(value: str) -> str:
    value = re.sub(r"https?://\S+", "<redacted-url>", value)
    value = re.sub(r"(?i)(token|signature|sig|key)=\S+", r"\1=<redacted>", value)
    return value.strip()[-900:]


def probe_candidate(candidate: dict, page_url: str, output: Path, seconds: float, timeout: int) -> tuple[bool, dict | str]:
    ffmpeg = shutil.which("ffmpeg")
    ffprobe = shutil.which("ffprobe")
    if not ffmpeg or not ffprobe:
        raise RuntimeError("Install system ffmpeg and ffprobe first")

    result_file = output / f"episode-{candidate['field']}.mkv"
    referer = page_url.replace("'", "%27")
    page = urllib.parse.urlsplit(page_url)
    command = [
        ffmpeg,
        "-hide_banner", "-nostdin", "-loglevel", "error", "-y",
        "-user_agent", APP_USER_AGENT,
        # Match SiteDownloadRepository.resolveChoices(): the request includes the page
        # Referer but does not invent an Origin header or forward provider cookies to CDNs.
        "-headers", f"Referer: {referer}\r\n",
        "-protocol_whitelist", "http,https,tcp,tls,crypto,data",
        "-i", candidate["url"],
        "-t", str(seconds),
        "-map", "0:v:0",
        "-map", "0:a:0?",
        "-c", "copy",
        str(result_file),
    ]
    try:
        process = subprocess.run(command, capture_output=True, text=True, timeout=timeout)
    except subprocess.TimeoutExpired:
        result_file.unlink(missing_ok=True)
        return False, "FFmpeg timed out"
    if process.returncode != 0 or not result_file.exists() or result_file.stat().st_size == 0:
        result_file.unlink(missing_ok=True)
        return False, safe_stderr(process.stderr) or f"FFmpeg exited {process.returncode}"

    probe = subprocess.run(
        [ffprobe, "-v", "error", "-show_entries",
         "format=format_name,duration,size:stream=codec_type,codec_name,width,height,sample_rate,channels",
         "-of", "json", str(result_file)],
        capture_output=True, text=True, timeout=30,
    )
    if probe.returncode != 0:
        result_file.unlink(missing_ok=True)
        return False, safe_stderr(probe.stderr) or "ffprobe rejected downloaded sample"
    info = json.loads(probe.stdout)
    info["sample_path"] = str(result_file)
    return True, info


def main() -> int:
    args = parse_args()
    page = urllib.parse.urlsplit(args.episode_url)
    if page.scheme != "https" or page.hostname not in {
        "narto-drama.com", "www.narto-drama.com", "edge.narto-drama.com"
    }:
        print("Refusing non-Narto or non-HTTPS episode URL", file=sys.stderr)
        return 2
    if args.episode < 1 or args.seconds <= 0:
        print("Episode and sample duration must be positive", file=sys.stderr)
        return 2
    output = Path(tempfile.mkdtemp(prefix="narto-playback-probe-"))
    output.mkdir(parents=True, exist_ok=True)

    try:
        html = fetch_page(args.episode_url)
        entry = episode_entry(html, args.episode)
        candidates = candidates_for(entry, args.episode_url)
        if not candidates:
            print("Parser result: no supported HTTPS media URL")
            print(f"Episode playable flag: {entry.get('is_playable')!r}")
            print(f"Output directory: {output}")
            return 1
        print(f"Episode title: {entry.get('title', '(untitled)')}")
        print(f"Narto is_playable: {entry.get('is_playable')!r}; prefetch mode: {entry.get('browser_prefetch_mode')!r}")
        print("Candidate order: " + " -> ".join(c["field"] for c in candidates))
        passed = []
        for index, candidate in enumerate(candidates, start=1):
            print(f"[{index}] {candidate['field']}: host={candidate['host']} type={'HLS' if candidate['hls'] else 'file'}")
            ok, result = probe_candidate(candidate, args.episode_url, output, args.seconds, args.timeout)
            if ok:
                passed.append(index)
                print("    FFmpeg download: OK")
                print("    ffprobe: " + json.dumps(result, ensure_ascii=False))
            else:
                print("    FFmpeg download: FAILED — " + str(result))
        if passed:
            print(f"First working candidate: {passed[0]} ({candidates[passed[0] - 1]['field']})")
            if passed[0] > 1:
                print("Player failover: a player that advances to subsequent Narto candidate URLs should reach a working candidate.")
            else:
                print("Player default candidate: the first source should work on this network.")
            print(f"Output directory: {output}")
            return 0
        print("No source candidate downloaded a valid sample.")
        print(f"Output directory: {output}")
        return 1
    except Exception as error:
        print(f"Probe failed: {safe_stderr(str(error))}", file=sys.stderr)
        print(f"Output directory: {output}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
