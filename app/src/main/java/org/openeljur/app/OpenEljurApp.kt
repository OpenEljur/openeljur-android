package org.openeljur.app

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openeljur.app.data.*

class OpenEljurApp : Application() {
    lateinit var prefs: PrefsStore
    lateinit var network: NetworkMonitor

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsStore(this)
        network = NetworkMonitor(this)
        CacheStore.init(this)

        CoroutineScope(Dispatchers.IO).launch {
            val token = prefs.token.first()
            val schoolId = prefs.schoolId.first()
            if (token.isNotBlank()) {
                val sp = getSharedPreferences("prefs_bootstrap", Context.MODE_PRIVATE)
                sp.edit()
                    .putString("token", token)
                    .putString("school_id", schoolId)
                    .apply()
            }
        }
    }

    override fun attachBaseContext(base: Context) {
        val lang = PrefsStore.readLanguageSync(base)
        super.attachBaseContext(LanguageManager.wrap(base, lang))
    }
}
