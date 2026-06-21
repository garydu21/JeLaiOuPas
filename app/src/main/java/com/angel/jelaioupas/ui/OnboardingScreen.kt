package com.angel.jelaioupas.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.jelaioupas.R
import com.angel.jelaioupas.google.GoogleAuthManager
import kotlinx.coroutines.launch

private val CardWhite = Color(0xFFFDFDFB)
private val InkBlack = Color(0xFF161616)

/** Souligne+gras le 1er mot, italique pour le reste — style charte. */
private fun under(bold: String, rest: String) = buildAnnotatedString {
    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append(bold); pop()
    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(rest); pop()
}

@Composable
fun OnboardingScreen(
    linking: Boolean,
    errorMessage: String?,
    showExistingChoice: Boolean,
    onUseExisting: () -> Unit,
    onRecreate: () -> Unit,
    // step 1
    onLinked: (String) -> Unit,
    // step 2
    onFinish: (withImages: Boolean) -> Unit,
    // contrôle de l'étape : on passe à l'étape 2 quand la liaison a réussi
    accountLinked: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.google_web_client_id)
    val auth = remember { GoogleAuthManager(context) }
    var loading by remember { mutableStateOf(false) }
    val busy = loading || linking
    var withImages by remember { mutableStateOf(true) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        val token = auth.tokenFromIntent(res.data)
        loading = false
        if (token != null) onLinked(token)
        else Toast.makeText(context, "Connexion annulée", Toast.LENGTH_SHORT).show()
    }

    fun startLink() {
        loading = true
        scope.launch {
            try {
                when (val state = auth.authorize(webClientId)) {
                    is GoogleAuthManager.AuthState.Granted -> { loading = false; onLinked(state.accessToken) }
                    is GoogleAuthManager.AuthState.NeedsConsent ->
                        consentLauncher.launch(IntentSenderRequest.Builder(state.pendingIntent).build())
                }
            } catch (e: Exception) {
                loading = false
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.home_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo teinté en blanc (sur fond sombre)
            Image(
                painter = painterResource(R.drawable.logo_press_scan),
                contentDescription = "Press SCAN",
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.White),
                modifier = Modifier.fillMaxWidth(0.62f)
            )

            Spacer(Modifier.height(48.dp))

            if (!accountLinked) {
                // ---------- ÉTAPE 1/2 ----------
                Text(
                    buildAnnotatedString {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
                        append("Bienvenue"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" sur l'application !\n"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append("Merci de "); pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)); append("suivre"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" les deux étapes\npour "); pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)); append("finaliser"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" l'installation."); pop()
                    },
                    color = Color.White, fontFamily = Lato, fontSize = 22.sp,
                    textAlign = TextAlign.Center, lineHeight = 30.sp
                )
                Spacer(Modifier.height(40.dp))
                Text("1/2", color = Color.White, fontFamily = SquareFont, fontSize = 44.sp)
                Spacer(Modifier.height(40.dp))

                WhiteButton(onClick = { if (!busy) startLink() }, enabled = !busy) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = InkBlack)
                    } else {
                        Image(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(under("Connecter", " mon Google"), color = InkBlack, fontFamily = Lato, fontSize = 20.sp)
                    }
                }
            } else {
                // ---------- ÉTAPE 2/2 ----------
                Text("2/2", color = Color.White, fontFamily = SquareFont, fontSize = 44.sp)
                Spacer(Modifier.height(28.dp))
                Text(
                    buildAnnotatedString {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append("Voulez-vous "); pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)); append("charger"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" la base de données "); pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)); append("AVEC"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" ou "); pop()
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)); append("SANS"); pop()
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(" images ?"); pop()
                    },
                    color = Color.White, fontFamily = Lato, fontSize = 22.sp,
                    textAlign = TextAlign.Center, lineHeight = 30.sp
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    ChoiceChip("Avec", selected = withImages) { withImages = true }
                    ChoiceChip("Sans", selected = !withImages) { withImages = false }
                }
                Spacer(Modifier.height(36.dp))
                WhiteButton(onClick = { if (!busy) onFinish(withImages) }, enabled = !busy) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = InkBlack)
                    else Text(under("Finaliser", " l'installation"), color = InkBlack, fontFamily = Lato, fontSize = 20.sp)
                }
            }

            if (linking) {
                Spacer(Modifier.height(18.dp))
                Text("Préparation de ta collection…", color = Color.White, fontFamily = Lato, fontSize = 14.sp)
            }
            if (errorMessage != null) {
                Spacer(Modifier.height(18.dp))
                Text("Erreur : $errorMessage", color = Color(0xFFFF6B6B), fontFamily = Lato,
                    fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }

        if (showExistingChoice) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Collection existante", fontFamily = Lato, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Une collection Press SCAN existe déjà sur ton compte Google. " +
                            "Veux-tu la réutiliser, ou repartir d'une collection vierge " +
                            "(l'ancienne sera supprimée) ?",
                        fontFamily = Lato
                    )
                },
                confirmButton = {
                    TextButton(onClick = onUseExisting) {
                        Text("Réutiliser", fontFamily = Lato, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onRecreate) {
                        Text("Repartir de zéro", fontFamily = Lato, color = Color(0xFFCC0000))
                    }
                }
            )
        }
    }
}

@Composable
private fun WhiteButton(onClick: () -> Unit, enabled: Boolean, content: @Composable RowScope.() -> Unit) {
    Surface(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(16.dp), color = CardWhite, shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(0.86f).height(62.dp)
    ) {
        Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically, content = content)
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CardWhite else Color.Black.copy(alpha = 0.35f),
        modifier = Modifier
            .width(130.dp).height(52.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) InkBlack else Color.White, fontFamily = Lato, fontSize = 18.sp)
        }
    }
}

