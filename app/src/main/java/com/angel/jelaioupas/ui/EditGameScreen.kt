package com.angel.jelaioupas.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.jelaioupas.data.Game

private val CardWhite = Color(0xFFFDFDFB)
private val InkBlack = Color(0xFF161616)

private fun titledUnder(bold: String, rest: String, accent: Color = Color.White) = buildAnnotatedString {
    pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append(bold); pop()
    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(rest); pop()
}

/**
 * Édition d'un jeu. Les valeurs partent de l'état actuel du jeu.
 * "Mettre à jour" appelle onUpdate(...). Si on quitte sans cliquer, rien n'est modifié.
 */
@Composable
fun EditGameScreen(
    game: Game,
    updating: Boolean,
    bg: Color,
    onUpdate: (inCollection: Boolean, statut: String, notice: String, etat: String) -> Unit,
    onBack: () -> Unit
) {
    var inCollection by remember { mutableStateOf(game.owned) }
    var statut by remember { mutableStateOf(game.statut.ifBlank { "Occasion" }) }
    var notice by remember { mutableStateOf(game.notice.ifBlank { "Non" }) }
    var etat by remember { mutableStateOf(game.etat.ifBlank { "Moyen" }) }

    // Couleur du fond : vert si possédé (Oui), rouge sinon (Non) — transition douce
    val targetCenter = if (inCollection) Ps.Success else Ps.Danger
    val targetEdge = if (inCollection) Ps.SuccessDark else Ps.DangerDark
    val animCenter by animateColorAsState(targetCenter, animationSpec = tween(450), label = "center")
    val animEdge by animateColorAsState(targetEdge, animationSpec = tween(450), label = "edge")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .radialBg(animCenter, animEdge)
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header (fixe en haut)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text(titledUnder("Retourner", " au scan"), color = Color.White, fontFamily = Lato, fontSize = 16.sp)
            }
        }

        // Espace flexible haut -> centre le bloc
        Spacer(Modifier.weight(1f))

        // --- Bloc central (se recentre quand Statut/Notice/État disparaissent) ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            Text(
                game.title.ifBlank { "?" }.uppercase(),
                color = Color.White, fontFamily = SquareFont, fontSize = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(28.dp))
            GroupTitle(titledUnder("Dans ma", " collection ?"))
            ChoiceRow2(
                left = "Oui", right = "Non",
                selected = if (inCollection) "Oui" else "Non",
                onSelect = { inCollection = it == "Oui" }
            )

            // Statut / Notice / État : visibles seulement si possédé (Oui)
            AnimatedVisibility(visible = inCollection) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(24.dp))
                    GroupTitle(titledUnder("Statut", ""))
                    ChoiceRow2("Neuf", "Occasion", statut) { statut = it }

                    Spacer(Modifier.height(24.dp))
                    GroupTitle(titledUnder("Notice", ""))
                    ChoiceRow2("Oui", "Non", notice) { notice = it }

                    Spacer(Modifier.height(24.dp))
                    GroupTitle(titledUnder("État", " général"))
                    EtatChoices(etat) { etat = it }
                }
            }
        }

        // Espace flexible bas -> centre le bloc
        Spacer(Modifier.weight(1f))

        // Bouton "Mettre à jour" (ancré en bas)
        Surface(
            onClick = {
                if (!updating) {
                    if (inCollection) onUpdate(true, statut, notice, etat)
                    else onUpdate(false, "", "", "")  // Non -> on vide les détails
                }
            },
            enabled = !updating,
            shape = RoundedCornerShape(14.dp),
            color = CardWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(0.9f).height(52.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (updating) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = InkBlack)
                else Text(titledUnder("Mettre", " à jour", animEdge), color = InkBlack, fontFamily = Lato, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun GroupTitle(text: androidx.compose.ui.text.AnnotatedString) {
    Text(
        text, color = Color.White, fontFamily = Lato, fontSize = 19.sp,
        modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun ChoiceRow2(left: String, right: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        Pill(left, selected == left) { onSelect(left) }
        Pill(right, selected == right) { onSelect(right) }
    }
}

@Composable
private fun EtatChoices(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("Mauvais", "Moyen", "Bon", "Comme neuf", "Neuf")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Pill(options[0], selected == options[0]) { onSelect(options[0]) }
            Pill(options[1], selected == options[1]) { onSelect(options[1]) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Pill(options[2], selected == options[2]) { onSelect(options[2]) }
            Pill(options[3], selected == options[3]) { onSelect(options[3]) }
        }
        Spacer(Modifier.height(12.dp))
        Pill(options[4], selected == options[4]) { onSelect(options[4]) }
    }
}

@Composable
private fun Pill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) CardWhite else Color.Transparent,
        shadowElevation = if (selected) 6.dp else 0.dp,
        modifier = Modifier
            .widthIn(min = 120.dp)
            .height(44.dp)
            .border(1.5.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(50))
    ) {
        Box(Modifier.padding(horizontal = 20.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) InkBlack else Color.White, fontFamily = Lato, fontSize = 15.sp)
        }
    }
}
