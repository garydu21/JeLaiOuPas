package com.angel.jelaioupas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.angel.jelaioupas.ScanResult
import com.angel.jelaioupas.R
import com.angel.jelaioupas.data.Game

private fun under(bold: String, rest: String, accent: Color = Color(0xFF161616)) = buildAnnotatedString {
    pushStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append(bold); pop()
    pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(rest); pop()
}

@Composable
fun ResultScreen(
    result: ScanResult,
    onRescan: () -> Unit,
    onUpdate: (Game) -> Unit = {},
    onHome: () -> Unit = {}
) {
    when (result) {
        is ScanResult.Owned -> OwnedResult(result.game, onRescan, onUpdate, onHome)
        is ScanResult.NotOwned -> SimpleResult(
            bg = Ps.Danger, headline = "TU L'AS PAS",
            game = result.game, onRescan = onRescan, onHome = onHome
        )
        is ScanResult.Unknown -> SimpleResult(
            bg = Ps.Warning, headline = "INCONNU",
            game = null, sub = "EAN ${result.ean} absent de ta base",
            onRescan = onRescan, onHome = onHome
        )
    }
}

@Composable
private fun OwnedResult(game: Game, onRescan: () -> Unit, onUpdate: (Game) -> Unit, onHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .radialBg(Ps.Success, Ps.SuccessDark)
            .statusBarsPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header retour accueil
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onHome) {
                Text(under("Retourner", " à l'accueil", Color.White), color = Color.White, fontFamily = Lato, fontSize = 17.sp)
            }
            IconImage(R.drawable.ic_home, contentDescription = "Accueil", tint = Color.White, size = 28.dp)
        }

        Spacer(Modifier.weight(1f))

        // Smiley pixel content
        PixelSmiley(modifier = Modifier.size(140.dp))

        Spacer(Modifier.height(28.dp))

        Text(
            game.title.ifBlank { "?" }.uppercase(),
            color = Color.White, fontFamily = SquareFont, fontSize = 30.sp,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        InfoLine("Statut", game.statut.ifBlank { "—" })
        InfoLine("Notice", game.notice.ifBlank { "—" })
        InfoLine("État général", game.etat.ifBlank { "—" })

        Spacer(Modifier.weight(1f))

        WhiteResultButton(onClick = { onUpdate(game) }) {
            Text(under("Mettre", " à jour", Ps.SuccessDark), color = Color(0xFF161616), fontFamily = Lato, fontSize = 18.sp)
        }
        Spacer(Modifier.height(14.dp))
        WhiteResultButton(onClick = onRescan) {
            Text(scannerLabelResult(Ps.SuccessDark), color = Color(0xFF161616), fontSize = 18.sp)
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Text(
        buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("$label : "); pop()
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(value); pop()
        },
        color = Color.White, fontFamily = Lato, fontSize = 20.sp,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SimpleResult(
    bg: Color, headline: String, game: Game?, sub: String? = null,
    onRescan: () -> Unit, onHome: () -> Unit
) {
    val edge = when (bg) {
        Ps.Danger -> Ps.DangerDark
        Ps.Warning -> Ps.WarningDark
        else -> bg
    }
    Column(
        modifier = Modifier.fillMaxSize().radialBg(bg, edge).statusBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (game == null) Icons.Default.Help else Icons.Default.Cancel,
            contentDescription = null, tint = Color.White, modifier = Modifier.size(130.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(headline, color = Color.White, fontFamily = SquareFont, fontSize = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            sub ?: game?.title?.ifBlank { "?" } ?: "?",
            color = Color.White, fontFamily = Lato, fontSize = 19.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        WhiteResultButton(onClick = onRescan) {
            Text(scannerLabelResult(edge), color = bg, fontSize = 18.sp)
        }
    }
}

@Composable
private fun WhiteResultButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFDFDFB),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(0.86f).height(56.dp)
    ) {
        Row(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterVertically, content = content)
    }
}

private fun scannerLabelResult(accent: Color = Color(0xFF161616)) = buildAnnotatedString {
    pushStyle(SpanStyle(color = accent, fontFamily = Lato, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    append("Scanner"); pop()
    pushStyle(SpanStyle(fontFamily = Lato)); append(" un "); pop()
    pushStyle(SpanStyle(fontFamily = SquareFont)); append("CODE-BARRES"); pop()
}

/** Smiley pixel blanc : 2 yeux carrés + bouche en U (comme la charte). */
@Composable
private fun PixelSmiley(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val white = Color.White
        // grille 10x10, on positionne tout en fractions de s
        fun px(x: Float, y: Float, w: Float, h: Float) =
            drawRect(white, topLeft = Offset(x * s, y * s), size = Size(w * s, h * s))

        // Yeux carrés (taille 0.16), espacés
        val eyeSize = 0.17f
        val eyeY = 0.18f
        px(0.24f, eyeY, eyeSize, eyeSize)              // œil gauche
        px(0.59f, eyeY, eyeSize, eyeSize)              // œil droit

        // Bouche en U : 2 montants verticaux + barre du bas (légère descente aux coins)
        val mouthLeft = 0.24f
        val mouthRight = 0.59f + eyeSize               // aligné au bord ext. de l'œil droit
        val barTh = 0.11f                              // épaisseur des traits
        val mouthTop = 0.52f
        val mouthBottom = 0.72f
        // montant gauche
        px(mouthLeft, mouthTop, barTh, (mouthBottom - mouthTop))
        // montant droit
        px(mouthRight - barTh, mouthTop, barTh, (mouthBottom - mouthTop))
        // barre du bas (entre les deux montants)
        px(mouthLeft, mouthBottom - barTh, (mouthRight - mouthLeft), barTh)
        // petites pointes descendantes aux coins (effet "U" du design)
        px(mouthLeft, mouthBottom - barTh, barTh, barTh * 1.6f)
        px(mouthRight - barTh, mouthBottom - barTh, barTh, barTh * 1.6f)
    }
}
