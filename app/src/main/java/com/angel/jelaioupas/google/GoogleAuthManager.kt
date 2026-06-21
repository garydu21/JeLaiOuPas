package com.angel.jelaioupas.google

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Connexion Google + autorisation Drive(file) + Sheets, via AuthorizationClient.
 *
 * Flux :
 *  - authorize() : si l'accès est déjà accordé -> Granted(token).
 *                  sinon -> NeedsConsent(pendingIntent) à lancer depuis l'UI.
 *  - tokenFromIntent() : après le retour de l'écran de consentement, donne le token.
 */
class GoogleAuthManager(private val context: Context) {

    private val scopes = listOf(
        Scope(DriveScopes.DRIVE_FILE),
        Scope(SheetsScopes.SPREADSHEETS)
    )

    sealed class AuthState {
        data class Granted(val accessToken: String) : AuthState()
        data class NeedsConsent(val pendingIntent: PendingIntent) : AuthState()
    }

    suspend fun authorize(webClientId: String): AuthState =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(scopes)
                .requestOfflineAccess(webClientId)
                .build()

            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result: AuthorizationResult ->
                    val pi = result.pendingIntent
                    when {
                        result.accessToken != null ->
                            cont.resume(AuthState.Granted(result.accessToken!!))
                        pi != null ->
                            cont.resume(AuthState.NeedsConsent(pi))
                        else ->
                            cont.resumeWithException(IllegalStateException("Aucun token ni consentement"))
                    }
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    /**
     * Tente d'obtenir un token SANS interaction (accès déjà accordé).
     * Renvoie le token si dispo, null si un consentement serait nécessaire.
     */
    suspend fun silentToken(webClientId: String): String? =
        suspendCancellableCoroutine { cont ->
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(scopes)
                .requestOfflineAccess(webClientId)
                .build()
            Identity.getAuthorizationClient(context)
                .authorize(request)
                .addOnSuccessListener { result ->
                    // Si un PendingIntent est nécessaire -> pas silencieux -> null
                    cont.resume(if (result.pendingIntent == null) result.accessToken else null)
                }
                .addOnFailureListener { cont.resume(null) }
        }

    /** Après le retour de l'écran de consentement. */
    fun tokenFromIntent(data: Intent?): String? =
        Identity.getAuthorizationClient(context)
            .getAuthorizationResultFromIntent(data)
            .accessToken

    /**
     * Révoque l'accès accordé. Après ça, la prochaine autorisation
     * redemandera le consentement (et le choix du compte).
     */
    fun revokeAccess(onComplete: () -> Unit) {
        val request = com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder()
            .setScopes(scopes)
            .build()
        Identity.getAuthorizationClient(context)
            .revokeAccess(request)
            .addOnCompleteListener { onComplete() }
    }

    /** Version coroutine qui n'échoue jamais (best-effort). */
    suspend fun revokeAccessSuspend() = suspendCancellableCoroutine<Unit> { cont ->
        try {
            val request = com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder()
                .setScopes(scopes)
                .build()
            Identity.getAuthorizationClient(context)
                .revokeAccess(request)
                .addOnCompleteListener { if (cont.isActive) cont.resume(Unit) }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(Unit)
        }
    }
}
