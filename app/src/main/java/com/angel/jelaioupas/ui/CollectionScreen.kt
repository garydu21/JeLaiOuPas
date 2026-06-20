package com.angel.jelaioupas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.angel.jelaioupas.data.Game

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(games: List<Game>, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query, games) {
        if (query.isBlank()) games.sortedBy { it.title.lowercase() }
        else games.filter {
            it.title.contains(query, true) ||
                it.console.contains(query, true) ||
                it.ean.contains(query)
        }.sortedBy { it.title.lowercase() }
    }
    val ownedCount = remember(games) { games.count { it.owned } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ma collection") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Rechercher un jeu, une console…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                "${games.size} jeux · $ownedCount possédés",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.ean }) { game ->
                    ListItem(
                        headlineContent = {
                            Text(
                                game.title.ifBlank { game.ean },
                                fontWeight = FontWeight.Medium
                            )
                        },
                        supportingContent = {
                            if (game.console.isNotBlank()) Text(game.console)
                        },
                        leadingContent = {
                            if (game.owned) Icon(
                                Icons.Default.CheckCircle, contentDescription = "Possédé",
                                tint = Color(0xFF2E7D32)
                            ) else Icon(
                                Icons.Default.RadioButtonUnchecked,
                                contentDescription = "Non possédé",
                                tint = Color(0xFFBDBDBD)
                            )
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
