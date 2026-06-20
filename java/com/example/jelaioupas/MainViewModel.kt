package com.example.jelaioupas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jelaioupas.data.Game
import com.example.jelaioupas.data.SheetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanResult {
    data class Owned(val game: Game) : ScanResult()
    data class NotOwned(val game: Game) : ScanResult()
    data class Unknown(val ean: String) : ScanResult()
}

data class UiState(
    val sheetUrl: String = "",
    val gameCount: Int = 0,
    val lastSync: Long = 0L,
    val syncing: Boolean = false,
    val syncError: String? = null,
    val ready: Boolean = false // true si une base est chargée (cache ou sync)
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = SheetRepository(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result

    init {
        viewModelScope.launch {
            val cached = repo.loadCache()
            val url = repo.getSheetUrl()
            _state.value = _state.value.copy(
                sheetUrl = url,
                gameCount = repo.gameCount,
                ready = cached
            )
            // resync silencieux au démarrage si une URL est configurée
            if (url.isNotBlank()) sync(silent = true)
        }
        viewModelScope.launch {
            repo.lastSyncFlow.collect { _state.value = _state.value.copy(lastSync = it) }
        }
    }

    fun saveUrlAndSync(url: String) {
        viewModelScope.launch {
            repo.saveSheetUrl(url)
            _state.value = _state.value.copy(sheetUrl = url.trim())
            sync()
        }
    }

    fun sync(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(syncing = true, syncError = null)
            val res = repo.sync()
            _state.value = _state.value.copy(
                syncing = false,
                gameCount = repo.gameCount,
                ready = _state.value.ready || res.isSuccess,
                syncError = if (silent) null else res.exceptionOrNull()?.message
            )
        }
    }

    fun onBarcodeScanned(raw: String) {
        val game = repo.findByEan(raw)
        _result.value = when {
            game == null -> ScanResult.Unknown(raw)
            game.owned -> ScanResult.Owned(game)
            else -> ScanResult.NotOwned(game)
        }
    }

    fun clearResult() { _result.value = null }
}
