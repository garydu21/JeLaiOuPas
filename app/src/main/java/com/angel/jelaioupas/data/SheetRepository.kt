package com.angel.jelaioupas.data

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
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private val Context.dataStore by preferencesDataStore(name = "settings")

class SheetRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val keyUrl = stringPreferencesKey("sheet_url")
    private val keyTabs = stringPreferencesKey("sheet_tabs")
    private val keyLastSync = longPreferencesKey("last_sync")
    private val cacheFile get() = File(context.filesDir, "games_cache.tsv")

    @Volatile private var gamesByEan: Map<String, Game> = emptyMap()
    @Volatile var gameCount: Int = 0; private set

    val sheetUrlFlow = context.dataStore.data.map { it[keyUrl] ?: "" }
    val tabsFlow = context.dataStore.data.map { it[keyTabs] ?: "" }
    val lastSyncFlow = context.dataStore.data.map { it[keyLastSync] ?: 0L }

    suspend fun getSheetUrl(): String = sheetUrlFlow.first()
    suspend fun getTabs(): String = tabsFlow.first()

    suspend fun saveConfig(url: String, tabs: String) {
        context.dataStore.edit {
            it[keyUrl] = url.trim()
            it[keyTabs] = tabs.trim()
        }
    }

    private fun sheetId(sheetUrl: String): String? =
        Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)").find(sheetUrl)?.groupValues?.get(1)

    /** Un onglet précis par son nom (sheet partagée par lien, pas de clé API). */
    private fun tabCsvUrl(id: String, tabName: String): String =
        "https://docs.google.com/spreadsheets/d/$id/gviz/tq?tqx=out:csv&sheet=" +
            URLEncoder.encode(tabName.trim(), "UTF-8")

    /** Fallback : premier onglet (ou gid de l'URL) si aucun onglet listé. */
    private fun defaultCsvUrl(sheetUrl: String, id: String): String {
        val gid = Regex("[?#&]gid=(\\d+)").find(sheetUrl)?.groupValues?.get(1)
        val base = "https://docs.google.com/spreadsheets/d/$id/export?format=csv"
        return if (gid != null) "$base&gid=$gid" else base
    }

    private fun fetchCsv(url: String): String {
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
            val body = resp.body?.string() ?: ""
            if (body.trimStart().startsWith("<"))
                throw Exception("onglet introuvable ou sheet non partagée")
            return body
        }
    }

    /**
     * Détecte les noms d'onglets sans clé API : l'export xlsx est un zip
     * dont xl/workbook.xml liste les feuilles. On lit le zip en streaming
     * et on s'arrête dès qu'on a trouvé workbook.xml.
     */
    private fun fetchTabNames(id: String): List<String> {
        val url = "https://docs.google.com/spreadsheets/d/$id/export?format=xlsx"
        client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("HTTP ${resp.code} (détection des onglets)")
            val stream = resp.body?.byteStream() ?: throw Exception("réponse vide")
            java.util.zip.ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "xl/workbook.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return Regex("<sheet[^>]*name=\"([^\"]+)\"")
                            .findAll(xml)
                            .map { unescapeXml(it.groupValues[1]) }
                            .toList()
                    }
                    entry = zip.nextEntry
                }
            }
        }
        throw Exception("workbook.xml introuvable dans l'export")
    }

    private fun unescapeXml(s: String): String = s
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&quot;", "\"").replace("&apos;", "'")

    /** Télécharge tous les onglets (configurés ou auto-détectés), fusionne, met en cache. */
    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        val sheetUrl = getSheetUrl()
        val id = sheetId(sheetUrl)
            ?: return@withContext Result.failure(IllegalArgumentException("URL de sheet invalide"))
        var tabs = getTabs().split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }

        try {
            val all = HashMap<String, Game>()
            val errors = mutableListOf<String>()

            if (tabs.isEmpty()) {
                // Auto-détection ; si elle échoue, on retombe sur le premier onglet
                tabs = try { fetchTabNames(id) } catch (e: Exception) { emptyList() }
            }

            if (tabs.isEmpty()) {
                all.putAll(parseAny(fetchCsv(defaultCsvUrl(sheetUrl, id)), console = ""))
            } else {
                for (tab in tabs) {
                    try {
                        all.putAll(parseAny(fetchCsv(tabCsvUrl(id, tab)), console = tab))
                    } catch (e: Exception) {
                        errors.add("$tab (${e.message})")
                    }
                }
            }

            if (all.isEmpty())
                return@withContext Result.failure(
                    Exception("Aucun jeu chargé." + if (errors.isNotEmpty()) " Onglets en échec : ${errors.joinToString()}" else "")
                )

            gamesByEan = all
            gameCount = all.values.count { it.owned }
            saveCache(all.values)
            context.dataStore.edit { it[keyLastSync] = System.currentTimeMillis() }

            if (errors.isNotEmpty())
                Result.failure(Exception("${all.size} jeux chargés, mais onglets en échec : ${errors.joinToString()}"))
            else
                Result.success(gameCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------------- cache local (TSV plat) ----------------

    private fun saveCache(games: Collection<Game>) {
        val sb = StringBuilder()
        for (g in games) {
            fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ')
            sb.append(clean(g.title)).append('\t')
                .append(g.ean).append('\t')
                .append(if (g.owned) "1" else "0").append('\t')
                .append(clean(g.console)).append('\n')
        }
        cacheFile.writeText(sb.toString())
    }

    suspend fun loadCache(): Boolean = withContext(Dispatchers.IO) {
        if (!cacheFile.exists()) return@withContext false
        runCatching {
            val map = HashMap<String, Game>()
            cacheFile.readLines().forEach { line ->
                val p = line.split('\t')
                if (p.size >= 4) map[p[1]] = Game(p[0], p[1], p[2] == "1", p[3])
            }
            gamesByEan = map
            gameCount = map.values.count { it.owned }
        }.isSuccess && gameCount > 0
    }

    fun findByEan(rawCode: String): Game? = gamesByEan[EanUtils.normalize(rawCode)]

    /** Met à jour un jeu dans la map locale (après écriture Sheet). */
    fun updateLocal(game: Game) {
        val m = HashMap(gamesByEan)
        m[game.ean] = game
        gamesByEan = m
        gameCount = m.values.count { it.owned }
    }

    fun allGames(): List<Game> = gamesByEan.values.toList()

    /**
     * Charge la base depuis des données déjà lues via l'API Sheets.
     * Réutilise le même parsing que l'export CSV.
     */
    suspend fun loadFromApiRows(tabs: List<Pair<String, List<List<String>>>>): Int =
        withContext(Dispatchers.IO) {
            val all = HashMap<String, Game>()
            for ((tabName, rows) in tabs) {
                all.putAll(parseRows(rows, console = tabName))
            }
            gamesByEan = all
            gameCount = all.values.count { it.owned }
            saveCache(all.values)
            context.dataStore.edit { it[keyLastSync] = System.currentTimeMillis() }
            gameCount
        }

    private fun parseRows(rows: List<List<String>>, console: String): Map<String, Game> {
        if (rows.isEmpty()) return emptyMap()
        val header = rows.first().map { it.trim().lowercase() }
        return if (header.any { it.contains("ean") || it.contains("code-barre") })
            parseFlatTable(rows, header, console)
        else
            parseCatalog(rows, console)
    }

    // ----------------------------------------------------------------
    // Parsing : tableau plat (en-tête Titre/EAN/Possédé) OU catalogue
    // mis en forme (titre sur une ligne, EAN sur la suivante).
    // ----------------------------------------------------------------

    private val eanRegex = Regex("^\\d{8,14}$")
    private val ownedTokens = setOf("oui", "x", "true", "vrai", "ok")
    private val skipTokens = setOf(
        "pal", "ntsc", "ntsc-u", "ntsc-j", "fr", "en", "de", "es", "it",
        "jap", "us", "uk", "oui", "non", "x", "code-barres"
    )

    private fun parseAny(csv: String, console: String): Map<String, Game> {
        val rows = Csv.parse(csv)
        if (rows.isEmpty()) return emptyMap()
        val header = rows.first().map { it.trim().lowercase() }
        return if (header.any { it.contains("ean") || it.contains("code-barre") })
            parseFlatTable(rows, header, console)
        else
            parseCatalog(rows, console)
    }

    private fun parseFlatTable(
        rows: List<List<String>>, header: List<String>, console: String
    ): Map<String, Game> {
        fun col(vararg names: String) =
            header.indexOfFirst { h -> names.any { h.contains(it) } }
        val eanCol = col("ean", "code").let { if (it >= 0) it else 1 }
        val ownedCol = col("poss", "collection", "statut", "owned").let { if (it >= 0) it else 2 }
        val titleCol = col("titre", "title", "nom", "jeu").let { if (it >= 0) it else 0 }

        val map = HashMap<String, Game>()
        for (r in rows.drop(1)) {
            val ean = r.getOrNull(eanCol)?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val key = EanUtils.normalize(ean)
            map[key] = Game(
                title = r.getOrNull(titleCol)?.trim().orEmpty(),
                ean = key,
                owned = r.getOrNull(ownedCol)?.trim()?.lowercase() in ownedTokens,
                console = console
            )
        }
        return map
    }

    private fun parseCatalog(rows: List<List<String>>, console: String): Map<String, Game> {
        fun bestTitle(row: List<String>): String? =
            row.map { it.trim() }
                .filter { it.isNotEmpty() && it.lowercase() !in skipTokens }
                .filter { it.count { c -> c.isLetter() } >= 2 }
                .maxByOrNull { it.length }

        // Index de colonnes (0-based) du format Press SCAN
        val colCollection = 15 // P
        val colStatut = 19     // T
        val colNotice = 23     // X
        val colEtat = 27       // AB

        val map = HashMap<String, Game>()
        for (i in rows.indices) {
            val row = rows[i]
            val ean = row.firstOrNull { eanRegex.matches(it.trim()) }?.trim() ?: continue

            var title: String? = null
            for (j in intArrayOf(i - 1, i, i - 2)) {
                if (j in rows.indices) {
                    title = bestTitle(rows[j])
                    if (title != null) break
                }
            }

            fun cell(idx: Int): String = row.getOrNull(idx)?.trim().orEmpty()
            val collection = cell(colCollection)
            val statut = cell(colStatut)
            val notice = cell(colNotice)
            val etat = cell(colEtat)

            // "possédé" UNIQUEMENT si la colonne P (collection) dit oui.
            // (Avant : un "oui" dans Notice faisait croire à tort qu'on possédait le jeu.)
            val collectionLower = collection.lowercase()
            val owned = if (collection.isNotBlank()) {
                collectionLower in ownedTokens
            } else {
                // Ancien format sans colonne P remplie : repli sur tokens de la ligne
                row.any { it.trim().lowercase() in ownedTokens }
            }

            val key = EanUtils.normalize(ean)
            map[key] = Game(
                title = title.orEmpty(),
                ean = key,
                owned = owned,
                console = console,
                statut = statut,
                notice = notice,
                etat = etat,
                tab = console,
                rowIndex = i + 1   // ligne Sheet 1-based de la ligne EAN
            )
        }
        return map
    }
}
