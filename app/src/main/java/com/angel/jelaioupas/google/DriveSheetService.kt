package com.angel.jelaioupas.google

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import com.google.api.services.sheets.v4.Sheets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Accès Drive + Sheets avec un access token OAuth.
 * - ensureUserCopy() : crée (ou retrouve) la copie perso du sheet maître.
 */
class DriveSheetService(private val accessToken: String) {

    private val transport = GoogleNetHttpTransport.newTrustedTransport()
    private val jsonFactory = GsonFactory.getDefaultInstance()

    // Initializer qui ajoute le header Authorization: Bearer <token>
    private val initializer = com.google.api.client.http.HttpRequestInitializer { request ->
        request.headers.authorization = "Bearer $accessToken"
        request.connectTimeout = 30_000
        request.readTimeout = 60_000
    }

    private val drive: Drive = Drive.Builder(transport, jsonFactory, initializer)
        .setApplicationName("Press SCAN")
        .build()

    private val sheets: Sheets = Sheets.Builder(transport, jsonFactory, initializer)
        .setApplicationName("Press SCAN")
        .build()

    /**
     * Cherche une copie perso existante (marquée appProperties.pressScan=1).
     * Renvoie son ID, ou null si aucune.
     */
    suspend fun findExistingCopy(): String? = withContext(Dispatchers.IO) {
        val res = drive.files().list()
            .setQ("appProperties has { key='pressScan' and value='1' } and trashed=false")
            .setSpaces("drive")
            .setFields("files(id,name)")
            .setPageSize(1)
            .execute()
        res.files?.firstOrNull()?.id
    }

    /** Copie le maître et marque la copie. Renvoie l'ID de la nouvelle copie. */
    suspend fun createCopy(
        masterId: String,
        copyName: String = "Ma collection Press SCAN"
    ): String = withContext(Dispatchers.IO) {
        val meta = DriveFile().apply {
            name = copyName
            appProperties = mapOf("pressScan" to "1")
        }
        drive.files().copy(masterId, meta).setFields("id").execute().id
    }

    /** Met un fichier à la corbeille. */
    suspend fun deleteFile(fileId: String) = withContext(Dispatchers.IO) {
        drive.files().delete(fileId).execute()
    }

    /**
     * Garantit qu'une copie perso existe : si une copie existe, la réutilise ;
     * sinon copie le maître. (Conservé pour compat ; le flux avec choix
     * utilisateur passe plutôt par findExistingCopy + createCopy/deleteFile.)
     */
    suspend fun ensureUserCopy(
        masterId: String,
        copyName: String = "Ma collection Press SCAN"
    ): String = withContext(Dispatchers.IO) {
        findExistingCopy() ?: createCopy(masterId, copyName)
    }

    /** Noms des onglets du classeur. */
    suspend fun listTabs(spreadsheetId: String): List<String> = withContext(Dispatchers.IO) {
        val ss = sheets.spreadsheets().get(spreadsheetId)
            .setFields("sheets.properties.title")
            .execute()
        ss.sheets?.mapNotNull { it.properties?.title } ?: emptyList()
    }

    /**
     * Lit toutes les valeurs d'un onglet sous forme de lignes de chaînes.
     * Réutilisable par le parser existant du repository.
     */
    suspend fun readTab(spreadsheetId: String, tabName: String): List<List<String>> =
        withContext(Dispatchers.IO) {
            // Plage à partir de A pour garder les positions de colonnes (P/T/X/AB)
            val resp = sheets.spreadsheets().values()
                .get(spreadsheetId, "'$tabName'!A1:AF")
                .setValueRenderOption("FORMATTED_VALUE")
                .execute()
            val values = resp.getValues() ?: return@withContext emptyList()
            values.map { row -> row.map { cell -> cell?.toString() ?: "" } }
        }

    /** Écrit une valeur dans une cellule (ex: marquer "Oui" dans "Dans ma collection?"). */
    suspend fun writeCell(spreadsheetId: String, tabName: String, cellA1: String, value: String) =
        withContext(Dispatchers.IO) {
            val body = com.google.api.services.sheets.v4.model.ValueRange()
                .setValues(listOf(listOf<Any>(value)))
            sheets.spreadsheets().values()
                .update(spreadsheetId, "'$tabName'!$cellA1", body)
                .setValueInputOption("USER_ENTERED")
                .execute()
        }

    /**
     * Met à jour les 4 champs d'un jeu sur sa ligne (1-based).
     * Colonnes : P=collection, T=statut, X=notice, AB=état général.
     * Écriture groupée (batchUpdate) en une seule requête.
     */
    suspend fun updateGameDetails(
        spreadsheetId: String,
        tabName: String,
        row: Int,
        collection: String,
        statut: String,
        notice: String,
        etat: String
    ) = withContext(Dispatchers.IO) {
        fun vr(cell: String, value: String) =
            com.google.api.services.sheets.v4.model.ValueRange()
                .setRange("'$tabName'!$cell$row")
                .setValues(listOf(listOf<Any>(value)))

        val body = com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest()
            .setValueInputOption("USER_ENTERED")
            .setData(
                listOf(
                    vr("P", collection),
                    vr("T", statut),
                    vr("X", notice),
                    vr("AB", etat)
                )
            )
        sheets.spreadsheets().values()
            .batchUpdate(spreadsheetId, body)
            .execute()
    }

}
