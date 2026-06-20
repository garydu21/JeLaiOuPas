package com.angel.jelaioupas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.angel.jelaioupas.ScanResult

@Composable
fun ResultScreen(result: ScanResult, onRescan: () -> Unit) {
    fun label(title: String, console: String): String {
        val t = title.ifBlank { "?" }
        return if (console.isNotBlank()) "$t — $console" else t
    }

    val (bg, icon, headline, sub) = when (result) {
        is ScanResult.Owned -> Quad(
            Color(0xFF2E7D32), Icons.Default.CheckCircle,
            "TU L'AS DÉJÀ !", label(result.game.title, result.game.console)
        )
        is ScanResult.NotOwned -> Quad(
            Color(0xFFC62828), Icons.Default.Cancel,
            "TU L'AS PAS", label(result.game.title, result.game.console)
        )
        is ScanResult.Unknown -> Quad(
            Color(0xFFEF6C00), Icons.Default.Help,
            "INCONNU", "EAN ${result.ean} absent de ta base"
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(bg).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(140.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            headline,
            color = Color.White,
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            sub,
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onRescan,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = bg),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Scanner un autre jeu", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private data class Quad(val bg: Color, val icon: ImageVector, val title: String, val sub: String)
