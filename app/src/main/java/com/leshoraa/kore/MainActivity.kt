package com.leshoraa.kore

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
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
 */
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startKoReForegroundService()
        }
    }

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
                    NavigationItem(
                        label = stringResource(R.string.nav_dashboard),
                        route = "dashboard",
                        selectedIcon = Icons.Filled.AutoGraph,
                        unselectedIcon = Icons.Outlined.AutoGraph
                    ),
                    NavigationItem(
                        label = stringResource(R.string.nav_vision),
                        route = "vision",
                        selectedIcon = Icons.Filled.Videocam,
                        unselectedIcon = Icons.Outlined.Videocam
                    ),
                    NavigationItem(
                        label = stringResource(R.string.nav_logs),
                        route = "logs",
                        selectedIcon = Icons.Filled.History,
                        unselectedIcon = Icons.Outlined.History
                    ),
                    NavigationItem(
                        label = stringResource(R.string.nav_settings),
                        route = "settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings
                    )
                )

                LaunchedEffect(Unit) {
                    if (!PermissionManager.hasPermissions(this@MainActivity)) {
                        requestPermissionLauncher.launch(PermissionManager.requiredPermissions.toTypedArray())
                    } else {
                        startKoReForegroundService()
                    }
                }

                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            items.forEach { item ->
                                val selected = currentDestination?.hierarchy?.any { 
                                    it.route?.startsWith(item.route) == true 
                                } == true
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = { Text(item.label) },
                                    selected = selected,
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

    private fun startKoReForegroundService() {
        val serviceIntent = Intent(this, com.leshoraa.kore.service.foreground.KoReForegroundService::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start KoReForegroundService: ${e.message}")
        }
    }
}

// Navigation classes moved to separate file for better maintenance
