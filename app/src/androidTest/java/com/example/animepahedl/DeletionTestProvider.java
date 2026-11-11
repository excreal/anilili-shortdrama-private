package com.example.animepahedl;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Runs in the test APK's own process, which does not load the target Kotlin runtime. */
public class DeletionTestProvider extends ContentProvider {
    private final Set<String> removed = ConcurrentHashMap.newKeySet();
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "video/mp4"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (uri.getPath().contains("denied")) throw new SecurityException("Folder access revoked");
        // Real document providers can return null after a file was removed manually.
        if (uri.getPath().contains("nullcursor") || uri.getPath().contains("unavailable")) return null;
        MatrixCursor result = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME});
        if (!uri.getPath().contains("missing") && !removed.contains(uri.toString())) result.addRow(new Object[]{"fixture.mp4"});
        return result;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (uri.getPath().contains("unavailable")) throw new SecurityException("Provider unavailable");
        throw new FileNotFoundException("Removed test fixture");
    }
    @Override public int delete(Uri uri, String selection, String[] args) {
        if (uri.getPath().contains("refuse")) return 0;
        removed.add(uri.toString());
        return 1;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
