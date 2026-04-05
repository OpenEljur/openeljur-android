package org.openeljur.app.data

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {
    /** Wrap context with the saved locale so all string resources use it */
    fun wrap(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
