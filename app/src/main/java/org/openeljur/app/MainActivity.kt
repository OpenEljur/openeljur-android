package org.openeljur.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.openeljur.app.data.LanguageManager
import org.openeljur.app.data.PrefsStore
import org.openeljur.app.ui.AppNavHost
import org.openeljur.app.ui.theme.OpenEljurTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(base: Context) {
        val lang = PrefsStore.readLanguageSync(base)
        super.attachBaseContext(LanguageManager.wrap(base, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = PrefsStore(this)
        val initialLang = PrefsStore.readLanguageSync(this)

        setContent {
            val theme by prefs.theme.collectAsStateWithLifecycle(initialValue = "system")
            val language by prefs.language.collectAsStateWithLifecycle(initialValue = initialLang)

            val darkTheme = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Re-create activity when language changes to apply new locale
            var prevLang by remember { mutableStateOf(initialLang) }
            LaunchedEffect(language) {
                if (language != prevLang) {
                    prevLang = language
                    recreate()
                }
            }

            OpenEljurTheme(darkTheme = darkTheme) {
                AppNavHost()
            }
        }
    }
}
