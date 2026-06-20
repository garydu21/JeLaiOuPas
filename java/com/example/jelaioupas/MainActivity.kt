package com.example.jelaioupas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.jelaioupas.ui.ResultScreen
import com.example.jelaioupas.ui.ScanScreen
import com.example.jelaioupas.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    // Premier lancement : pas d'URL configurée -> réglages
    val start = if (state.sheetUrl.isBlank() && !state.ready) "settings" else "scan"

    // Quand un scan aboutit, on pousse l'écran résultat
    LaunchedEffect(result) {
        if (result != null) {
            nav.navigate("result") { launchSingleTop = true }
        }
    }

    NavHost(navController = nav, startDestination = start) {
        composable("scan") {
            ScanScreen(
                gameCount = state.gameCount,
                lastSync = state.lastSync,
                syncing = state.syncing,
                scanEnabled = result == null,
                onBarcode = vm::onBarcodeScanned,
                onOpenSettings = { nav.navigate("settings") },
                onSync = { vm.sync() }
            )
        }
        composable("result") {
            val r = result
            if (r != null) {
                ResultScreen(result = r) {
                    vm.clearResult()
                    nav.popBackStack("scan", inclusive = false)
                }
            }
        }
        composable("settings") {
            SettingsScreen(
                currentUrl = state.sheetUrl,
                gameCount = state.gameCount,
                syncing = state.syncing,
                syncError = state.syncError,
                canGoBack = nav.previousBackStackEntry != null,
                onSave = { url -> vm.saveUrlAndSync(url) },
                onBack = { nav.popBackStack() }
            )
        }
    }

    // Après une première sync réussie depuis l'écran réglages initial, bascule sur le scan
    LaunchedEffect(state.ready) {
        if (state.ready && nav.currentDestination?.route == "settings" &&
            nav.previousBackStackEntry == null
        ) {
            nav.navigate("scan") { popUpTo("settings") { inclusive = true } }
        }
    }
}
