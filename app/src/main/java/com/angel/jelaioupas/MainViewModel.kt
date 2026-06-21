package com.angel.jelaioupas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.angel.jelaioupas.data.Game
import com.angel.jelaioupas.data.SheetRepository
import com.angel.jelaioupas.google.DriveSheetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

sealed class ScanResult {
    data class Owned(val game: Game) : ScanResult()
    data class NotOwned(val game: Game) : ScanResult()
    data class Unknown(val ean: String) : ScanResult()
}

data class UiState(
    val sheetUrl: String = "",
    val tabs: String = "",
    val gameCount: Int = 0,
    val lastSync: Long = 0L,
    val syncing: Boolean = false,
    val syncError: String? = null,
    val ready: Boolean = false,
    val loadingGames: Boolean = false
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = SheetRepository(app)
    private val userPrefs = com.angel.jelaioupas.google.UserPrefs(app)
    private val authManager = com.angel.jelaioupas.google.GoogleAuthManager(app)
    private val webClientId = app.getString(R.string.google_web_client_id)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result

    // null = en cours de décision, true = aller à l'accueil, false = onboarding
    private val _autoLoggedIn = MutableStateFlow<Boolean?>(null)
    val autoLoggedIn: StateFlow<Boolean?> = _autoLoggedIn

    init {
        viewModelScope.launch {
            repo.loadCache()  // affiche la dernière base connue tout de suite
            tryAutoLogin()
        }
        viewModelScope.launch {
            repo.lastSyncFlow.collect { _state.value = _state.value.copy(lastSync = it) }
        }
    }

    /**
     * Au démarrage : si une copie est mémorisée, on va DIRECT à l'accueil
     * (avec le cache local), et on recharge depuis Google en arrière-plan.
     * Si aucune copie mémorisée -> onboarding.
     */
    private suspend fun tryAutoLogin() {
        val savedId = userPrefs.getSheetId()
        if (savedId == null) {
            _autoLoggedIn.value = false
            return
        }
        // On a déjà une install : accueil immédiat (cache déjà chargé)
        userSheetId = savedId
        _accountLinked.value = true
        _autoLoggedIn.value = true
        _state.value = _state.value.copy(gameCount = repo.gameCount, ready = true)

        // Rechargement Google en arrière-plan (non bloquant)
        backgroundRefresh(savedId)
    }

    /** Recharge la copie Google sans bloquer l'UI (indicateur loadingGames). */
    private fun backgroundRefresh(sheetId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingGames = true, syncError = null)
            try {
                val token = runCatching { authManager.silentToken(webClientId) }.getOrNull()
                if (token == null) {
                    // accès non renouvelable en silence -> on garde le cache, pas d'erreur bloquante
                    _state.value = _state.value.copy(loadingGames = false)
                    return@launch
                }
                googleAccessToken = token
                loadFromGoogle(DriveSheetService(token), sheetId)
                _state.value = _state.value.copy(loadingGames = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loadingGames = false, syncError = e.message)
            }
        }
    }

    fun saveConfigAndSync(url: String, tabs: String) {
        viewModelScope.launch {
            repo.saveConfig(url, tabs)
            _state.value = _state.value.copy(sheetUrl = url.trim(), tabs = tabs.trim())
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
                ready = _state.value.ready || repo.gameCount > 0,
                syncError = if (silent) null else res.exceptionOrNull()?.message
            )
        }
    }

    // --- Réglages (sons / vibrations) ---
    val soundEnabled = userPrefs.soundFlow.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)
    val vibrationEnabled = userPrefs.vibrationFlow.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, true)

    fun setSound(enabled: Boolean) { viewModelScope.launch { userPrefs.setSound(enabled) } }
    fun setVibration(enabled: Boolean) { viewModelScope.launch { userPrefs.setVibration(enabled) } }

    fun onBarcodeScanned(raw: String) {
        val game = repo.findByEan(raw)
        _result.value = when {
            game == null -> ScanResult.Unknown(raw)
            game.owned -> ScanResult.Owned(game)
            else -> ScanResult.NotOwned(game)
        }
        if (vibrationEnabled.value) vibrate()
    }

    // État d'écriture pour l'écran d'édition
    private val _updating = MutableStateFlow(false)
    val updating: StateFlow<Boolean> = _updating
    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError

    /**
     * Écrit les 4 champs du jeu dans la copie Google, puis met à jour
     * l'état local (collection/statut/notice/état) et l'écran résultat.
     */
    fun updateGame(
        game: Game,
        inCollection: Boolean,
        statut: String,
        notice: String,
        etat: String,
        onDone: () -> Unit
    ) {
        val token = googleAccessToken
        val id = userSheetId
        if (token == null || id == null || game.rowIndex <= 0 || game.tab.isBlank()) {
            _updateError.value = "Impossible de localiser le jeu dans le Sheet"
            return
        }
        _updating.value = true
        _updateError.value = null
        viewModelScope.launch {
            try {
                val service = DriveSheetService(token)
                val collection = if (inCollection) "Oui" else "Non"
                service.updateGameDetails(id, game.tab, game.rowIndex, collection, statut, notice, etat)
                // Mise à jour locale (sans recharger tout le Sheet)
                val updated = game.copy(owned = inCollection, statut = statut, notice = notice, etat = etat)
                repo.updateLocal(updated)
                _result.value = if (inCollection) ScanResult.Owned(updated) else ScanResult.NotOwned(updated)
                _state.value = _state.value.copy(gameCount = repo.gameCount)
                _updating.value = false
                onDone()
            } catch (e: Exception) {
                _updating.value = false
                _updateError.value = e.message ?: "Erreur lors de la mise à jour"
            }
        }
    }

    private fun vibrate() {
        val ctx = getApplication<Application>()
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
            val vm = ctx.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE)
                as android.os.VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        runCatching {
            vibrator.vibrate(
                android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun clearResult() { _result.value = null }

    // --- Connexion Google + copie du sheet maître ---
    @Volatile var googleAccessToken: String? = null
        private set
    @Volatile var userSheetId: String? = null
        private set
    private var masterIdCached: String? = null

    private val _accountLinked = MutableStateFlow(false)
    val accountLinked: StateFlow<Boolean> = _accountLinked
    private val _linking = MutableStateFlow(false)
    val linking: StateFlow<Boolean> = _linking
    private val _linkError = MutableStateFlow<String?>(null)
    val linkError: StateFlow<String?> = _linkError
    private val _existingChoice = MutableStateFlow(false)
    val existingChoice: StateFlow<Boolean> = _existingChoice
    private var pendingExistingId: String? = null
    private var pendingOnReady: (() -> Unit)? = null

    /** Étape 1 : token obtenu -> on passe à l'étape 2 (choix images). */
    fun onGoogleLinked(accessToken: String) {
        googleAccessToken = accessToken
        _linkError.value = null
        _accountLinked.value = true
    }

    /** Étape 2 "Finaliser" : détecte une copie existante puis crée/charge. */
    fun finishInstall(masterId: String, withImages: Boolean, onReady: () -> Unit) {
        val token = googleAccessToken ?: return
        masterIdCached = masterId
        pendingOnReady = onReady
        _linking.value = true
        _linkError.value = null
        viewModelScope.launch {
            try {
                val service = DriveSheetService(token)
                val existing = service.findExistingCopy()
                if (existing != null) {
                    pendingExistingId = existing
                    _linking.value = false
                    _existingChoice.value = true
                } else {
                    val id = service.createCopy(masterId)
                    userSheetId = id
                    userPrefs.setSheetId(id)
                    loadFromGoogle(service, id)
                    _linking.value = false
                    onReady()
                }
            } catch (e: Exception) {
                _linking.value = false
                _linkError.value = e.message ?: "Erreur lors de la copie du modèle"
            }
        }
    }

    /** Choix utilisateur : réutiliser la copie existante. */
    fun useExistingCopy() {
        val token = googleAccessToken ?: return
        val id = pendingExistingId ?: return
        _existingChoice.value = false
        _linking.value = true
        viewModelScope.launch {
            try {
                val service = DriveSheetService(token)
                userSheetId = id
                userPrefs.setSheetId(id)
                loadFromGoogle(service, id)
                _linking.value = false
                pendingOnReady?.invoke()
            } catch (e: Exception) {
                _linking.value = false
                _linkError.value = e.message
            }
        }
    }

    /** Choix utilisateur : supprimer l'existante et recréer une copie vierge. */
    fun recreateCopy() {
        val token = googleAccessToken ?: return
        val master = masterIdCached ?: return
        val old = pendingExistingId
        _existingChoice.value = false
        _linking.value = true
        viewModelScope.launch {
            try {
                val service = DriveSheetService(token)
                if (old != null) runCatching { service.deleteFile(old) }
                val id = service.createCopy(master)
                userSheetId = id
                userPrefs.setSheetId(id)
                loadFromGoogle(service, id)
                _linking.value = false
                pendingOnReady?.invoke()
            } catch (e: Exception) {
                _linking.value = false
                _linkError.value = e.message
            }
        }
    }

    /** Déconnexion : oublie le compte côté app. La révocation Google se fait
     *  côté écran via GoogleAuthManager.revokeAccess (pour reproposer le compte). */
    /**
     * Déconnexion complète et robuste :
     * 1) efface l'ID local (garanti, AVANT toute navigation)
     * 2) révoque l'accès Google en best-effort (sans bloquer ni crasher)
     * 3) appelle onDone sur le thread principal pour naviguer
     */
    fun disconnect(onDone: () -> Unit) {
        viewModelScope.launch {
            // 1) effacer le local d'abord (sinon on retombe "connecté" au redémarrage)
            runCatching { userPrefs.clear() }

            googleAccessToken = null
            userSheetId = null
            pendingExistingId = null
            _existingChoice.value = false
            _accountLinked.value = false
            _autoLoggedIn.value = false
            _state.value = _state.value.copy(gameCount = 0, ready = false, lastSync = 0L)

            // 2) révocation Google best-effort (on ignore tout échec)
            runCatching { authManager.revokeAccessSuspend() }

            // 3) navigation (on est déjà sur le main dispatcher via viewModelScope)
            onDone()
        }
    }

    /** Lit tous les onglets de la copie via l'API Sheets et charge la base. */
    private suspend fun loadFromGoogle(service: DriveSheetService, sheetId: String) {
        val tabs = service.listTabs(sheetId)
        // Lecture des onglets EN PARALLÈLE (beaucoup plus rapide que séquentiel)
        val data = kotlinx.coroutines.coroutineScope {
            tabs.map { tab ->
                async { tab to service.readTab(sheetId, tab) }
            }.awaitAll()
        }
        val count = repo.loadFromApiRows(data)
        _state.value = _state.value.copy(
            gameCount = count,
            ready = true,
            lastSync = System.currentTimeMillis()
        )
    }

    /** Re-synchronise depuis la copie Google de l'utilisateur (bouton Synchroniser). */
    fun syncFromGoogle() {
        val token = googleAccessToken
        val id = userSheetId
        if (token == null || id == null) {
            // Pas encore lié : on retombe sur l'ancienne sync (URL manuelle) si configurée
            sync()
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(syncing = true, syncError = null)
            try {
                loadFromGoogle(DriveSheetService(token), id)
                _state.value = _state.value.copy(syncing = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(syncing = false, syncError = e.message)
            }
        }
    }

    fun allGames(): List<Game> = repo.allGames()
}
