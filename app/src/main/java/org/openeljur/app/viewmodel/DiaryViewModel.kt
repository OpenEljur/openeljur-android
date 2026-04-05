package org.openeljur.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openeljur.app.OpenEljurApp
import org.openeljur.app.data.*
import java.text.SimpleDateFormat
import java.util.*

data class DayEntry(val key: String, val day: DiaryDay)

class DiaryViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val appCast = app as OpenEljurApp
    private val prefs = appCast.prefs
    private val network = appCast.network

    private val _days = MutableStateFlow<List<DayEntry>>(emptyList())
    val days: StateFlow<List<DayEntry>> = _days

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _weekStart = MutableStateFlow(startOfWeek(Date()))
    val weekStart: StateFlow<Date> = _weekStart

    val weekLabel: StateFlow<String> = _weekStart.map { start ->
        val fmt = SimpleDateFormat("dd.MM", Locale.getDefault())
        val end = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_YEAR, 6) }.time
        "${fmt.format(start)} – ${fmt.format(end)}"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "")

    init { load() }

    fun prevWeek() {
        _weekStart.value = Calendar.getInstance().apply {
            time = _weekStart.value; add(Calendar.DAY_OF_YEAR, -7)
        }.time
        load()
    }

    fun nextWeek() {
        _weekStart.value = Calendar.getInstance().apply {
            time = _weekStart.value; add(Calendar.DAY_OF_YEAR, 7)
        }.time
        load()
    }

    fun goToday() {
        _weekStart.value = startOfWeek(Date())
        load()
    }

    fun load() {
        viewModelScope.launch {
            val token = prefs.tokenReady()
            val schoolId = prefs.schoolIdReady()
            val range = weekRange()
            val cacheKey = "diary_$range"

            CacheStore.load(cacheKey)?.let { cached ->
                parseDays(cached)?.let { _days.value = it }
            }

            if (!network.isCurrentlyOnline()) {
                if (_days.value.isEmpty()) _error.value = ctx.getString(org.openeljur.app.R.string.common_no_network)
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            val result = apiPost<DiaryRequest, Envelope<DiaryData>>(
                "/v1/diary",
                DiaryRequest(token, schoolId.ifBlank { null }, days = range)
            )
            _isLoading.value = false

            result.fold(
                onSuccess = { envelope ->
                    if (envelope.ok) {
                        val student = envelope.data?.students?.values?.firstOrNull()
                        val sorted = (student?.days ?: emptyMap())
                            .entries.sortedBy { it.key }
                            .map { DayEntry(it.key, it.value) }
                        _days.value = sorted
                        val raw = ApiClient.jsonParser.encodeToString(Envelope.serializer(DiaryData.serializer()), envelope)
                        CacheStore.save(cacheKey, raw)
                    } else {
                        _error.value = envelope.error?.message
                    }
                },
                onFailure = {
                    if (_days.value.isEmpty()) _error.value =
                        if (it is EmptyResponseException) ctx.getString(org.openeljur.app.R.string.error_empty_response)
                        else it.message
                }
            )
        }
    }

    private fun parseDays(json: String): List<DayEntry>? = try {
        val envelope = ApiClient.jsonParser.decodeFromString<Envelope<DiaryData>>(json)
        val student = envelope.data?.students?.values?.firstOrNull()
        (student?.days ?: emptyMap()).entries.sortedBy { it.key }.map { DayEntry(it.key, it.value) }
    } catch (e: Exception) { null }

    private fun weekRange(): String {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val start = _weekStart.value
        val end = Calendar.getInstance().apply { time = start; add(Calendar.DAY_OF_YEAR, 6) }.time
        return "${fmt.format(start)}-${fmt.format(end)}"
    }

    companion object {
        fun startOfWeek(date: Date): Date {
            val cal = Calendar.getInstance().apply {
                time = date
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            return cal.time
        }
    }
}
