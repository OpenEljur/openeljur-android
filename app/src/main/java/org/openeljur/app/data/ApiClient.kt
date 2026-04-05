package org.openeljur.app.data

import org.openeljur.app.BuildConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object ApiClient {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    var baseUrl: String = BuildConfig.API_BASE_URL

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun post(path: String, bodyStr: String): Result<String> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl$path")
                    .post(bodyStr.toRequestBody(mediaType))
                    .build()
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(EmptyResponseException())
                Result.success(responseBody)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    val jsonParser get() = json
}

class EmptyResponseException : Exception()

suspend inline fun <reified Req, reified Res> apiPost(path: String, body: Req): Result<Res> {
    val bodyStr = ApiClient.json.encodeToString(body)
    return ApiClient.post(path, bodyStr).mapCatching { raw ->
        ApiClient.json.decodeFromString<Res>(raw)
    }
}
