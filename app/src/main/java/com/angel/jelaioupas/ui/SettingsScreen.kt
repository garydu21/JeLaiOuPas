package com.angel.jelaioupas.ui

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
    currentTabs: String,
    gameCount: Int,
    syncing: Boolean,
    syncError: String?,
    canGoBack: Boolean,
    onSave: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    var tabs by remember(currentTabs) { mutableStateOf(currentTabs) }

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
                "Colle l'URL de ta Google Sheet (partagée \"Tous les utilisateurs " +
                    "disposant du lien\", lecture). Laisse le champ Onglets vide pour " +
                    "charger automatiquement TOUS les onglets, ou liste-en certains " +
                    "(séparés par des virgules) pour te limiter à ceux-là.",
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
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = tabs,
                onValueChange = { tabs = it },
                label = { Text("Onglets (vide = tous, auto)") },
                placeholder = { Text("ex : Xbox, PS2 — ou laisse vide") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(url, tabs) },
                enabled = url.isNotBlank() && !syncing,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (syncing) CircularProgressIndicator(
                    Modifier.size(22.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
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
