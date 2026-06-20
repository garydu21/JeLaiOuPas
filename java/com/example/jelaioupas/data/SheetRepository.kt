package com.example.jelaioupas.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "settings")

class SheetRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val keyUrl = stringPreferencesKey("sheet_url")
    private val keyLastSync = longPreferencesKey("last_sync")
    private val cacheFile get() = File(context.filesDir, "sheet_cache.csv")

    // ---- état en mémoire ----
    @Volatile private var gamesByEan: Map<String, Game> = emptyMap()
    @Volatile var gameCount: Int = 0; private set

    val sheetUrlFlow = context.dataStore.data.map { it[keyUrl] ?: "" }
    val lastSyncFlow = context.dataStore.data.map { it[keyLastSync] ?: 0L }

    suspend fun getSheetUrl(): String = sheetUrlFlow.first()

    suspend fun saveSheetUrl(url: String) {
        context.dataStore.edit { it[keyUrl] = url.trim() }
    }

    /**
     * Transforme une URL de Google Sheet en URL d'export CSV.
     * Garde le gid si présent (onglet précis), sinon premier onglet.
     */
    fun toCsvExportUrl(sheetUrl: String): String? {
        val idRegex = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
        val id = idRegex.find(sheetUrl)?.groupValues?.get(1) ?: return null
        val gid = Regex("[?#&]gid=(\\d+)").find(sheetUrl)?.groupValues?.get(1)
        val base = "https://docs.google.com/spreadsheets/d/$id/export?format=csv"
        return if (gid != null) "$base&gid=$gid" else base
    }

    /** Télécharge la sheet, met à jour le cache disque et l'index mémoire. */
    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        val url = getSheetUrl()
        val exportUrl = toCsvExportUrl(url)
            ?: return@withContext Result.failure(IllegalArgumentException("URL de sheet invalide"))
        try {
            val req = Request.Builder().url(exportUrl).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful)
                    return@withContext Result.failure(Exception("HTTP ${resp.code} — vérifie que la sheet est partagée \"tous ceux qui ont le lien\""))
                val body = resp.body?.string() ?: ""
                if (body.trimStart().startsWith("<"))
                    return@withContext Result.failure(Exception("La sheet n'est pas accessible publiquement"))
                cacheFile.writeText(body)
                loadFromText(body)
                context.dataStore.edit { it[keyLastSync] = System.currentTimeMillis() }
            }
            Result.success(gameCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Charge le cache disque au démarrage (offline-friendly). */
    suspend fun loadCache(): Boolean = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext false
        runCatching { loadFromText(cacheFile.readText()) }.isSuccess && gameCount > 0
    }

    private fun loadFromText(csv: String) {
        val rows = Csv.parse(csv)
        if (rows.isEmpty()) { gamesByEan = emptyMap(); gameCount = 0; return }

        val header = rows.first().map { it.trim().lowercase() }
        fun col(vararg names: String): Int =
            header.indexOfFirst { h -> names.any { h.contains(it) } }

        // Détection souple des colonnes
        val eanCol = col("ean", "code", "barre").let { if (it >= 0) it else 1 }
        val ownedCol = col("poss", "statut", "owned", "j'ai", "jai").let { if (it >= 0) it else 2 }
        val titleCol = col("titre", "title", "nom", "jeu").let { if (it >= 0) it else 0 }
        val hasHeader = header.any { it.isNotEmpty() && !it.all { c -> c.isDigit() } }

        val map = HashMap<String, Game>()
        for (r in if (hasHeader) rows.drop(1) else rows) {
            val ean = r.getOrNull(eanCol)?.takeIf { it.isNotBlank() } ?: continue
            val ownedRaw = r.getOrNull(ownedCol)?.trim()?.lowercase() ?: ""
            val owned = ownedRaw in setOf("oui", "yes", "true", "1", "x", "ok", "possédé", "possede")
            val key = EanUtils.normalize(ean)
            map[key] = Game(
                title = r.getOrNull(titleCol)?.trim().orEmpty(),
                ean = key,
                owned = owned
            )
        }
        gamesByEan = map
        gameCount = map.size
    }

    fun findByEan(rawCode: String): Game? = gamesByEan[EanUtils.normalize(rawCode)]
}
