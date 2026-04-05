package org.openeljur.app.data

import android.content.Context
import java.io.File

/** Simple JSON file cache — stores last successful API responses */
object CacheStore {
    private lateinit var cacheDir: File

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, "api_cache").also { it.mkdirs() }
    }

    fun save(key: String, json: String) {
        File(cacheDir, "$key.json").writeText(json)
    }

    fun load(key: String): String? {
        val f = File(cacheDir, "$key.json")
        return if (f.exists()) f.readText() else null
    }

    fun clear(key: String) {
        File(cacheDir, "$key.json").delete()
    }
}
