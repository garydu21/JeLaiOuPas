package com.angel.jelaioupas.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.jelaioupas.R

private val CardWhite = Color(0xFFFDFDFB)
private val InkBlack = Color(0xFF161616)

private fun under(bold: String, rest: String) = buildAnnotatedString {
    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append(bold); pop()
    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(rest); pop()
}

@Composable
fun SettingsScreen(
    soundEnabled: Boolean,
    vibrationEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onVibrationChange: (Boolean) -> Unit,
    onDisconnect: () -> Unit,
    onBack: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.home_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(modifier = Modifier.fillMaxSize()) {

            // Header : Retourner à l'accueil + maison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text(under("Retourner", " à l'accueil"), color = Color.White, fontFamily = Lato, fontSize = 18.sp)
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onBack) {
                    IconImage(R.drawable.ic_home, contentDescription = "Accueil", tint = Color.White, size = 30.dp)
                }
            }

            Spacer(Modifier.weight(1f))

            // Carte : Sons + Vibrations
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
            ) {
                Column(Modifier.padding(horizontal = 22.dp, vertical = 18.dp)) {
                    SettingRow(
                        icon = { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = InkBlack, modifier = Modifier.size(28.dp)) },
                        label = "Sons",
                        checked = soundEnabled,
                        onCheckedChange = onSoundChange
                    )
                    Spacer(Modifier.height(14.dp))
                    SettingRow(
                        icon = { Icon(Icons.Default.GraphicEq, null, tint = InkBlack, modifier = Modifier.size(28.dp)) },
                        label = "Vibrations",
                        checked = vibrationEnabled,
                        onCheckedChange = onVibrationChange
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Carte : Compte Google + bouton Déconnecter
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_google_g),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("Compte Google", color = InkBlack, fontFamily = Lato, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Button(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Déconnecter", color = Color.White, fontFamily = Lato, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(label, color = InkBlack, fontFamily = Lato, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = InkBlack,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}
