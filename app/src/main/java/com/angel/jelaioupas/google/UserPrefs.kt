package com.angel.jelaioupas.google

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.googleDataStore by preferencesDataStore(name = "google_prefs")

/** Mémorise l'ID de la copie Google + les réglages (sons, vibrations). */
class UserPrefs(private val context: Context) {

    private val keySheetId = stringPreferencesKey("user_sheet_id")
    private val keySound = booleanPreferencesKey("pref_sound")
    private val keyVibration = booleanPreferencesKey("pref_vibration")

    val sheetIdFlow = context.googleDataStore.data.map { it[keySheetId] }
    val soundFlow = context.googleDataStore.data.map { it[keySound] ?: false }
    val vibrationFlow = context.googleDataStore.data.map { it[keyVibration] ?: true }

    suspend fun getSheetId(): String? = sheetIdFlow.first()

    suspend fun setSheetId(id: String) {
        context.googleDataStore.edit { it[keySheetId] = id }
    }

    suspend fun setSound(enabled: Boolean) {
        context.googleDataStore.edit { it[keySound] = enabled }
    }

    suspend fun setVibration(enabled: Boolean) {
        context.googleDataStore.edit { it[keyVibration] = enabled }
    }

    suspend fun clear() {
        context.googleDataStore.edit { it.remove(keySheetId) }
    }
}
