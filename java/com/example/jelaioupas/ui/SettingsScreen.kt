package com.example.jelaioupas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUrl: String,
    gameCount: Int,
    syncing: Boolean,
    syncError: String?,
    canGoBack: Boolean,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    if (canGoBack) IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Colle l'URL de ta Google Sheet. Elle doit être partagée en " +
                    "\"Tous les utilisateurs disposant du lien\" (lecture).\n\n" +
                    "Format attendu : une ligne d'en-tête avec au minimum une colonne " +
                    "Titre, une colonne EAN et une colonne Possédé (oui/non).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL Google Sheet") },
                placeholder = { Text("https://docs.google.com/spreadsheets/d/…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(url) },
                enabled = url.isNotBlank() && !syncing,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (syncing) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text("Enregistrer et synchroniser")
            }
            Spacer(Modifier.height(16.dp))
            if (syncError != null) {
                Text("Erreur : $syncError", color = MaterialTheme.colorScheme.error)
            } else if (gameCount > 0) {
                Text("$gameCount jeux chargés ✓", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
