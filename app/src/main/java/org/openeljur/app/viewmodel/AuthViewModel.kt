package org.openeljur.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.openeljur.app.BuildConfig
import org.openeljur.app.OpenEljurApp
import org.openeljur.app.data.*

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val prefs = (app as OpenEljurApp).prefs

    val token: StateFlow<String> = prefs.token.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val schoolId: StateFlow<String> = prefs.schoolId.stateIn(viewModelScope, SharingStarted.Eagerly, BuildConfig.DEFAULT_SCHOOL_ID)
    val isAuthenticated: StateFlow<Boolean> = token.map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // true когда DataStore отдал первое значение (не дефолт из stateIn)
    val isReady: StateFlow<Boolean> = prefs.token
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun login(username: String, password: String, schoolId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = apiPost<LoginRequest, Envelope<LoginData>>(
                "/v1/auth/login",
                LoginRequest(username, password, schoolId.ifBlank { null })
            )
            _isLoading.value = false
            result.fold(
                onSuccess = { envelope ->
                    if (envelope.ok && !envelope.data?.token.isNullOrBlank()) {
                        prefs.setToken(envelope.data!!.token!!)
                        prefs.setSchoolId(schoolId.ifBlank { BuildConfig.DEFAULT_SCHOOL_ID })
                    } else {
                        _error.value = envelope.error?.message ?: ctx.getString(org.openeljur.app.R.string.auth_error)
                    }
                },
                onFailure = { _error.value =
                    if (it is EmptyResponseException) ctx.getString(org.openeljur.app.R.string.error_empty_response)
                    else it.message
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch { prefs.logout() }
    }
}
