package com.angel.jelaioupas.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.angel.jelaioupas.R

private val CardWhite = Color(0xFFFDFDFB)
private val InkBlack = Color(0xFF161616)

@Composable
fun HomeScreen(
    gameCount: Int,
    syncing: Boolean,
    scanEnabled: Boolean,
    onBarcode: (String) -> Unit,
    onCollection: () -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        scanning = granted
    }

    fun startScan() {
        if (hasPermission) scanning = true
        else launcher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        Image(
            painter = painterResource(R.drawable.home_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Barre du haut ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSync, enabled = !syncing) {
                    if (syncing) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        label("Synchroniser", " la base"),
                        color = Color.White, fontFamily = Lato, fontSize = 18.sp
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onSettings) {
                    IconImage(
                        R.drawable.ic_settings,
                        contentDescription = "Réglages",
                        size = 28.dp,
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // --- Carte : logo OU caméra (même emplacement) ---
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1.5f)
                    .shadow(12.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (scanning) Color.Black else CardWhite),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = scanning, label = "scanbox") { isScanning ->
                    if (isScanning && hasPermission) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CameraScanBox(
                                scanEnabled = scanEnabled,
                                onBarcode = onBarcode,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Bouton fermer (revenir au logo)
                            IconButton(
                                onClick = { scanning = false },
                                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = Color.White)
                            }
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.logo_press_scan),
                                contentDescription = "Press SCAN",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            // --- Boutons ---
            HomeButton(onClick = { startScan() }) {
                Text(scannerLabel(), color = InkBlack, fontSize = 19.sp)
            }

            Spacer(Modifier.height(14.dp))

            HomeButton(onClick = onCollection) {
                Text(label("Voir", " ma collection"), color = InkBlack, fontFamily = Lato, fontSize = 19.sp)
                if (gameCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("($gameCount)", color = InkBlack.copy(alpha = 0.55f), fontFamily = Lato, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun HomeButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .height(58.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

private fun label(bold: String, rest: String) = buildAnnotatedString {
    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append(bold)
    pop()
    append(rest)
}

/** "Scanner un CODE-BARRES" : Lato pour le début, Square pour CODE-BARRES,
 *  dans un seul Text pour une baseline commune (pas de décalage vertical). */
private fun scannerLabel() = buildAnnotatedString {
    pushStyle(
        SpanStyle(
            fontFamily = Lato,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )
    )
    append("Scanner")
    pop()
    pushStyle(SpanStyle(fontFamily = Lato))
    append(" un ")
    pop()
    pushStyle(SpanStyle(fontFamily = SquareFont))
    append("CODE-BARRES")
    pop()
}
