package org.openeljur.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openeljur.app.OpenEljurApp
import org.openeljur.app.data.*

class MessagesViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val appCast = app as OpenEljurApp
    private val prefs = appCast.prefs
    private val network = appCast.network

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _selectedMessage = MutableStateFlow<Message?>(null)
    val selectedMessage: StateFlow<Message?> = _selectedMessage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _folder = MutableStateFlow("inbox")
    val folder: StateFlow<String> = _folder

    private val _unreadOnly = MutableStateFlow(false)
    val unreadOnly: StateFlow<Boolean> = _unreadOnly

    private var currentPage = 0
    private val pageSize = 20

    val unreadCount: StateFlow<Int> = _messages.map { msgs ->
        msgs.count { it.unread == true }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    init { load() }

    fun setFolder(f: String) { _folder.value = f; load() }
    fun setUnreadOnly(v: Boolean) { _unreadOnly.value = v; load() }

    fun load() {
        viewModelScope.launch {
            val token = prefs.tokenReady()
            val schoolId = prefs.schoolIdReady()
            currentPage = 0
            val cacheKey = "messages_${_folder.value}_${_unreadOnly.value}"

            CacheStore.load(cacheKey)?.let { cached ->
                parseMessages(cached)?.let { _messages.value = it }
            }

            if (!network.isCurrentlyOnline()) {
                if (_messages.value.isEmpty()) _error.value = ctx.getString(org.openeljur.app.R.string.common_no_network)
                return@launch
            }

            _isLoading.value = true
            _error.value = null

            fetch(token, schoolId, 0) { msgs ->
                _messages.value = msgs
                _hasMore.value = msgs.size >= pageSize
                val raw = ApiClient.jsonParser.encodeToString(
                    Envelope.serializer(MessagesData.serializer()),
                    Envelope(true, MessagesData(msgs))
                )
                CacheStore.save(cacheKey, raw)
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (!_hasMore.value || _isLoadingMore.value) return
        viewModelScope.launch {
            val token = prefs.tokenReady()
            val schoolId = prefs.schoolIdReady()
            _isLoadingMore.value = true
            fetch(token, schoolId, currentPage + 1) { msgs ->
                _messages.value = _messages.value + msgs
                currentPage++
                _hasMore.value = msgs.size >= pageSize
            }
            _isLoadingMore.value = false
        }
    }

    fun loadMessage(id: String) {
        viewModelScope.launch {
            val token = prefs.tokenReady()
            val schoolId = prefs.schoolIdReady()
            val result = apiPost<MessageInfoRequest, Envelope<MessageInfoData>>(
                "/v1/messages/info",
                MessageInfoRequest(token, schoolId.ifBlank { null }, id)
            )
            result.getOrNull()?.data?.message?.let { msg ->
                _selectedMessage.value = msg
                _messages.value = _messages.value.map {
                    if (it.id == id) it.copy(unread = false) else it
                }
            }
        }
    }

    fun clearSelected() { _selectedMessage.value = null }

    private suspend fun fetch(token: String, schoolId: String, page: Int, onSuccess: (List<Message>) -> Unit) {
        val result = apiPost<MessagesRequest, Envelope<MessagesData>>(
            "/v1/messages",
            MessagesRequest(
                token, schoolId.ifBlank { null },
                folder = _folder.value,
                unread_only = if (_unreadOnly.value) "yes" else "no",
                limit = pageSize, page = page
            )
        )
        result.fold(
            onSuccess = { envelope ->
                if (envelope.ok) onSuccess(envelope.data?.messages ?: emptyList())
                else _error.value = envelope.error?.message
            },
            onFailure = {
                if (_messages.value.isEmpty()) _error.value =
                    if (it is EmptyResponseException) ctx.getString(org.openeljur.app.R.string.error_empty_response)
                    else it.message
            }
        )
    }

    private fun parseMessages(json: String): List<Message>? = try {
        ApiClient.jsonParser.decodeFromString<Envelope<MessagesData>>(json).data?.messages
    } catch (e: Exception) { null }
}
