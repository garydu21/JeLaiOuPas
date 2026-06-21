package com.angel.jelaioupas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import com.angel.jelaioupas.ui.CollectionScreen
import com.angel.jelaioupas.ui.HomeScreen
import com.angel.jelaioupas.ui.OnboardingScreen
import com.angel.jelaioupas.ui.ResultScreen
import com.angel.jelaioupas.ui.EditGameScreen
import com.angel.jelaioupas.ui.ScanScreen
import com.angel.jelaioupas.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF81C784))
                         else lightColorScheme(primary = Color(0xFF2E7D32))
            MaterialTheme(colorScheme = colors) {
                Surface {
                    App(vm)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun App(vm: MainViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val result by vm.result.collectAsStateWithLifecycle()
    val autoLoggedIn by vm.autoLoggedIn.collectAsStateWithLifecycle()

    // Tant que la décision n'est pas prise -> écran de chargement
    if (autoLoggedIn == null) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        return
    }

    val start = if (autoLoggedIn == true) "home" else "link"

    LaunchedEffect(result) {
        if (result != null) nav.navigate("result") { launchSingleTop = true }
    }

    NavHost(navController = nav, startDestination = start) {

        composable("link") {
            val masterId = androidx.compose.ui.res.stringResource(
                com.angel.jelaioupas.R.string.google_master_sheet_id
            )
            val linking by vm.linking.collectAsStateWithLifecycle()
            val linkError by vm.linkError.collectAsStateWithLifecycle()
            val existingChoice by vm.existingChoice.collectAsStateWithLifecycle()
            val accountLinked by vm.accountLinked.collectAsStateWithLifecycle()
            val goHome = { nav.navigate("home") { popUpTo("link") { inclusive = true } } }
            OnboardingScreen(
                linking = linking,
                errorMessage = linkError,
                showExistingChoice = existingChoice,
                accountLinked = accountLinked,
                onUseExisting = { vm.useExistingCopy() },
                onRecreate = { vm.recreateCopy() },
                onLinked = { accessToken -> vm.onGoogleLinked(accessToken) },
                onFinish = { withImages ->
                    vm.finishInstall(masterId, withImages) { goHome() }
                }
            )
        }

        composable("home") {
            HomeScreen(
                gameCount = state.gameCount,
                syncing = state.syncing,
                loadingGames = state.loadingGames,
                onScan = { nav.navigate("scan") },
                onCollection = { nav.navigate("collection") },
                onSync = { vm.syncFromGoogle() },
                onSettings = { nav.navigate("settings") }
            )
        }

        composable("scan") {
            ScanScreen(
                scanEnabled = result == null,
                onBarcode = vm::onBarcodeScanned,
                onBack = { nav.popBackStack() }
            )
        }

        composable("result") {
            val r = result
            if (r != null) {
                ResultScreen(
                    result = r,
                    onRescan = {
                        vm.clearResult()
                        nav.popBackStack("scan", inclusive = false)
                    },
                    onUpdate = { nav.navigate("edit") },
                    onHome = {
                        vm.clearResult()
                        nav.navigate("home") { popUpTo("home") { inclusive = true } }
                    }
                )
            }
        }

        composable("edit") {
            val r = result
            val game = when (r) {
                is com.angel.jelaioupas.ScanResult.Owned -> r.game
                is com.angel.jelaioupas.ScanResult.NotOwned -> r.game
                else -> null
            }
            val updating by vm.updating.collectAsStateWithLifecycle()
            if (game != null) {
                EditGameScreen(
                    game = game,
                    updating = updating,
                    bg = com.angel.jelaioupas.ui.Ps.Success,
                    onUpdate = { inColl, statut, notice, etat ->
                        vm.updateGame(game, inColl, statut, notice, etat) {
                            nav.popBackStack() // retour à l'écran résultat (mis à jour)
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
        }

        composable("collection") {
            CollectionScreen(
                games = vm.allGames(),
                onBack = { nav.popBackStack() }
            )
        }

        composable("settings") {
            val sound by vm.soundEnabled.collectAsStateWithLifecycle()
            val vibration by vm.vibrationEnabled.collectAsStateWithLifecycle()
            SettingsScreen(
                soundEnabled = sound,
                vibrationEnabled = vibration,
                onSoundChange = { vm.setSound(it) },
                onVibrationChange = { vm.setVibration(it) },
                onDisconnect = {
                    vm.disconnect {
                        nav.navigate("link") { popUpTo(0) { inclusive = true } }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
