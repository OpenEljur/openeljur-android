package org.openeljur.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.openeljur.app.BuildConfig
import org.openeljur.app.R
import org.openeljur.app.data.PrefsStore
import org.openeljur.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(authVm: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { PrefsStore(context) }
    val scope = rememberCoroutineScope()
    val language by prefs.language.collectAsStateWithLifecycle(initialValue = "ru")
    val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = "system")
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Language names are always in their native language
    val languages = listOf(
        "ru" to "Русский",
        "en" to "English",
        "uk" to "Українська",
        "ro" to "Română"
    )

    val themes = listOf(
        "system" to stringResource(R.string.settings_theme_system),
        "light" to stringResource(R.string.settings_theme_light),
        "dark" to stringResource(R.string.settings_theme_dark)
    )

    val appVersion = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—" }
        catch (e: Exception) { "—" }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings_title)) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {

            // Language section
            item {
                ListItem(headlineContent = {
                    Text(stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                })
            }
            items(languages.size) { i ->
                val (code, label) = languages[i]
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        if (language == code) Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { scope.launch { prefs.setLanguage(code) } }
                )
            }

            item { HorizontalDivider() }

            // Theme section
            item {
                ListItem(headlineContent = {
                    Text(stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary)
                })
            }
            items(themes.size) { i ->
                val (code, label) = themes[i]
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        if (theme == code) Icon(Icons.Default.Check, null,
                            tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.clickable { scope.launch { prefs.setTheme(code) } }
                )
            }

            item { HorizontalDivider() }

            // Info section
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_api_url)) },
                    supportingContent = { Text(BuildConfig.API_BASE_URL,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    trailingContent = { Text(appVersion, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                )
            }

            item { HorizontalDivider() }

            // Logout
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_logout),
                            color = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable { showLogoutDialog = true }
                )
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.settings_logout)) },
            text = { Text(stringResource(R.string.settings_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = { authVm.logout(); showLogoutDialog = false }) {
                    Text(stringResource(R.string.nav_logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}
