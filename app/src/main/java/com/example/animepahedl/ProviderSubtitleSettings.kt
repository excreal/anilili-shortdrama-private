package com.example.animepahedl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.anilili.ui.dialogs.*
import kotlinx.coroutines.launch

@Composable
internal fun OpenSubtitlesAccountDialog(
    loggedIn: Boolean,
    currentUsername: String?,
    busy: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
) {
    var username by remember(currentUsername, loggedIn) { mutableStateOf(currentUsername.orEmpty()) }
    var password by remember(loggedIn) { mutableStateOf("") }
    MidnightModal(onDismiss = { if (!busy) onDismiss() }) {
        MidnightModalTitle(
            kicker = "OpenSubtitles",
            title = if (loggedIn) "Connected as ${currentUsername ?: "your account"}" else "Sign in",
            body = if (loggedIn) {
                "Enter credentials below only to reconnect with another account."
            } else {
                "Use a free OpenSubtitles.com account. Credentials are encrypted on this device so " +
                    "the app can renew its short-lived API session automatically."
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                enabled = !busy,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                enabled = !busy,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        errorMessage?.let { MidnightInlineNotice(it, tone = MidnightNoticeTone.ERROR) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loggedIn) {
                MidnightQuietAction(
                    text = "Sign out",
                    onClick = onSignOut,
                    enabled = !busy,
                    destructive = true,
                )
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MidnightQuietAction(text = "Cancel", onClick = onDismiss, enabled = !busy)
                MidnightFilledAction(
                    text = when {
                        busy -> "Signing in…"
                        loggedIn -> "Reconnect"
                        else -> "Sign in"
                    },
                    onClick = { onSignIn(username, password) },
                    enabled = !busy && username.isNotBlank() && password.isNotBlank(),
                    destructive = false,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}


/** Account UI copied from Anilili; only the API-key configuration is supplied by the host. */
@Composable
internal fun ProviderSubtitleSettings() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app-settings", android.content.Context.MODE_PRIVATE) }
    var key by remember { mutableStateOf(prefs.getString("opensubtitles-api-key", "").orEmpty()) }
    var configured by remember { mutableStateOf(com.anilili.data.AppGraph.openSubtitles.isConfigured) }
    var account by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val loggedIn by com.anilili.data.auth.OpenSubtitlesAuthManager.loggedIn.collectAsState()
    val username by com.anilili.data.auth.OpenSubtitlesAuthManager.username.collectAsState()
    val scope = rememberCoroutineScope()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(vertical = 12.dp)) {
        Text("Provider subtitles", style = MaterialTheme.typography.titleMedium)
        Text("Available languages appear in the player's captions menu. Subtitles are saved with downloaded episodes for offline playback.")
        Text("Use the captions menu to import a file, adjust timing or change subtitle appearance.")
        HorizontalDivider()
        Text("OpenSubtitles", style = MaterialTheme.typography.titleMedium)
        Text("Optional subtitle search uses your OpenSubtitles API key and account.")
        OutlinedTextField(key, { key = it }, label = { Text("OpenSubtitles API key") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            prefs.edit().putString("opensubtitles-api-key", key.trim()).apply()
            com.anilili.data.AppGraph.configureOpenSubtitles(context)
            configured = com.anilili.data.AppGraph.openSubtitles.isConfigured
        }) { Text("Save API key") }
        OutlinedButton(onClick = { error = null; account = true }, enabled = configured) {
            Text(if (loggedIn) "Connected as ${username.orEmpty()}" else "Sign in to OpenSubtitles")
        }
    }
    if (account) OpenSubtitlesAccountDialog(loggedIn, username, busy, error, { account = false }, { user, password ->
        busy = true; error = null
        scope.launch {
            try { com.anilili.data.auth.OpenSubtitlesAuthManager.signIn(user, password); account = false }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not sign in. Try again." }
            finally { busy = false }
        }
    }, { com.anilili.data.auth.OpenSubtitlesAuthManager.logout(); account = false })
}
