package com.leshoraa.kore

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.leshoraa.kore.core.common.PermissionManager
import com.leshoraa.kore.presentation.navigation.KoReNavGraph
import com.leshoraa.kore.presentation.navigation.NavigationItem
import com.leshoraa.kore.presentation.theme.AppPalette
import com.leshoraa.kore.presentation.theme.KoReTheme

/**
 * Main activity of the KoRe application.
 * Follows industry standards for clean architecture and UI separation.
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            var darkTheme by remember { mutableStateOf(systemInDarkTheme) }
            var selectedPalette by remember { mutableStateOf(AppPalette.FOREST) }

            KoReTheme(darkTheme = darkTheme, palette = selectedPalette) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                val items = listOf(
                    NavigationItem(stringResource(R.string.nav_dashboard), "dashboard", Icons.Default.AutoGraph),
                    NavigationItem(stringResource(R.string.nav_filters), "rules", Icons.Default.FilterList),
                    NavigationItem(stringResource(R.string.nav_logs), "logs", Icons.Default.History),
                    NavigationItem(stringResource(R.string.nav_settings), "settings", Icons.Default.Settings)
                )

                LaunchedEffect(Unit) {
                    if (!PermissionManager.hasPermissions(this@MainActivity)) {
                        requestPermissionLauncher.launch(PermissionManager.requiredPermissions.toTypedArray())
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize()) {
                        KoReNavGraph(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding),
                            darkTheme = darkTheme,
                            onToggleTheme = { darkTheme = !darkTheme },
                            selectedPalette = selectedPalette,
                            onPaletteChange = { selectedPalette = it },
                            enableBluetoothLauncher = enableBluetoothLauncher
                        )
                    }
                }
            }
        }
    }
}

// Navigation classes moved to separate file for better maintenance
