package org.openeljur.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.openeljur.app.BuildConfig

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "prefs")

object PrefsKeys {
    val TOKEN = stringPreferencesKey("auth_token")
    val SCHOOL_ID = stringPreferencesKey("school_id")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
}

private const val SP_NAME = "prefs_bootstrap"

class PrefsStore(private val context: Context) {
    private val sp: SharedPreferences
        get() = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

    private val systemLang: String get() {
        val lang = java.util.Locale.getDefault().language
        return if (lang in listOf("ru", "en", "uk", "ro")) lang else "en"
    }

    val token: Flow<String> = context.dataStore.data.map { it[PrefsKeys.TOKEN] ?: "" }
    val schoolId: Flow<String> = context.dataStore.data.map { it[PrefsKeys.SCHOOL_ID] ?: BuildConfig.DEFAULT_SCHOOL_ID }
    val language: Flow<String> = context.dataStore.data.map { it[PrefsKeys.LANGUAGE] ?: systemLang }
    val theme: Flow<String> = context.dataStore.data.map { it[PrefsKeys.THEME] ?: "system" }

    fun tokenSync(): String = sp.getString("token", "") ?: ""
    fun schoolIdSync(): String = sp.getString("school_id", BuildConfig.DEFAULT_SCHOOL_ID) ?: BuildConfig.DEFAULT_SCHOOL_ID

    suspend fun tokenReady(): String {
        val fast = tokenSync()
        if (fast.isNotBlank()) return fast
        val fromDs = token.first { it.isNotBlank() }
        sp.edit().putString("token", fromDs).apply()
        return fromDs
    }

    suspend fun schoolIdReady(): String {
        val fast = schoolIdSync()
        if (fast.isNotBlank()) return fast
        return schoolId.first()
    }

    suspend fun setToken(value: String) {
        context.dataStore.edit { it[PrefsKeys.TOKEN] = value }
        sp.edit().putString("token", value).apply()
    }

    suspend fun setSchoolId(value: String) {
        context.dataStore.edit { it[PrefsKeys.SCHOOL_ID] = value }
        sp.edit().putString("school_id", value).apply()
    }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[PrefsKeys.LANGUAGE] = value }
        sp.edit().putString("language", value).apply()
    }

    suspend fun setTheme(value: String) = context.dataStore.edit { it[PrefsKeys.THEME] = value }

    suspend fun logout() {
        context.dataStore.edit { it.remove(PrefsKeys.TOKEN) }
        sp.edit().remove("token").apply()
    }

    companion object {
        fun readLanguageSync(context: Context): String {
            val sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            val saved = sp.getString("language", null)
            if (saved != null) return saved
            val lang = java.util.Locale.getDefault().language
            return if (lang in listOf("ru", "en", "uk", "ro")) lang else "en"
        }
    }
}
