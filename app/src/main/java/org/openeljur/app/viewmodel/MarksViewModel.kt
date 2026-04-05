package org.openeljur.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openeljur.app.OpenEljurApp
import org.openeljur.app.data.*
import java.util.Calendar

class MarksViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val appCast = app as OpenEljurApp
    private val prefs = appCast.prefs
    private val network = appCast.network

    private val _lessons = MutableStateFlow<List<LessonMarks>>(emptyList())
    val lessons: StateFlow<List<LessonMarks>> = _lessons

    private val _studentName = MutableStateFlow("")
    val studentName: StateFlow<String> = _studentName

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _quarter = MutableStateFlow(detectQuarter())
    val quarter: StateFlow<Int> = _quarter

    fun setQuarter(q: Int) {
        _quarter.value = q
        load()
    }

    fun load() {
        viewModelScope.launch {
            val token = prefs.tokenReady()
            val schoolId = prefs.schoolIdReady()
            val range = quarterRange(_quarter.value)
            val cacheKey = "marks_${_quarter.value}"

            CacheStore.load(cacheKey)?.let { cached ->
                parseMarks(cached)?.let { (name, lessons) ->
                    _studentName.value = name
                    _lessons.value = lessons
                }
            }

            if (!network.isCurrentlyOnline()) {
                if (_lessons.value.isEmpty()) _error.value = ctx.getString(org.openeljur.app.R.string.common_no_network)
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            val result = apiPost<MarksRequest, Envelope<MarksData>>(
                "/v1/marks",
                MarksRequest(token, schoolId.ifBlank { null }, days = range)
            )
            _isLoading.value = false

            result.fold(
                onSuccess = { envelope ->
                    if (envelope.ok) {
                        val student = envelope.data?.students?.values?.firstOrNull()
                        _studentName.value = student?.title ?: student?.name ?: ""
                        _lessons.value = student?.lessons ?: emptyList()
                        val raw = ApiClient.jsonParser.encodeToString(Envelope.serializer(MarksData.serializer()), envelope)
                        CacheStore.save(cacheKey, raw)
                    } else {
                        _error.value = envelope.error?.message
                    }
                },
                onFailure = { if (_lessons.value.isEmpty()) _error.value =
                    if (it is EmptyResponseException) ctx.getString(org.openeljur.app.R.string.error_empty_response)
                    else it.message
                }
            )
        }
    }

    private fun parseMarks(json: String): Pair<String, List<LessonMarks>>? = try {
        val envelope = ApiClient.jsonParser.decodeFromString<Envelope<MarksData>>(json)
        val student = envelope.data?.students?.values?.firstOrNull()
        Pair(student?.title ?: student?.name ?: "", student?.lessons ?: emptyList())
    } catch (e: Exception) { null }

    private fun quarterRange(q: Int): String {
        val now = Calendar.getInstance()
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH) + 1
        val sy = if (month >= 9) year else year - 1
        return when (q) {
            1 -> "${sy}0901-${sy}1031"
            2 -> "${sy}1101-${sy}1231"
            3 -> "${sy + 1}0101-${sy + 1}0331"
            else -> "${sy + 1}0401-${sy + 1}0630"
        }
    }

    private fun detectQuarter(): Int {
        val month = Calendar.getInstance().get(Calendar.MONTH) + 1
        return when {
            month in 9..10 -> 1
            month in 11..12 -> 2
            month in 1..3 -> 3
            else -> 4
        }
    }
}
