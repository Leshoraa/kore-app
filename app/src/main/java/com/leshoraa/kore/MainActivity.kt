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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.leshoraa.kore.core.common.PermissionManager
import com.leshoraa.kore.core.common.ServiceLocator
import com.leshoraa.kore.presentation.dashboard.DashboardScreen
import com.leshoraa.kore.presentation.dashboard.DashboardViewModel
import com.leshoraa.kore.presentation.logs.LogsScreen
import com.leshoraa.kore.presentation.logs.LogsViewModel
import com.leshoraa.kore.presentation.rules.RulesScreen
import com.leshoraa.kore.presentation.rules.RulesViewModel
import com.leshoraa.kore.presentation.scanner.BleScannerViewModel
import com.leshoraa.kore.presentation.scanner.ScannerScreen
import com.leshoraa.kore.presentation.settings.SettingsScreen
import com.leshoraa.kore.presentation.theme.AppPalette
import com.leshoraa.kore.presentation.theme.KoReTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission states handled by UI checks
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Handle result if needed
    }

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
                    NavigationItem("Dashboard", "dashboard", Icons.Default.AutoGraph),
                    NavigationItem("Filters", "rules", Icons.Default.FilterList),
                    NavigationItem("Logs", "logs", Icons.Default.History),
                    NavigationItem("Settings", "settings", Icons.Default.Settings)
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
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable("dashboard") {
                                val viewModel: DashboardViewModel = viewModel {
                                    DashboardViewModel(
                                        ServiceLocator.provideBleManager(applicationContext),
                                        ServiceLocator.provideBleRepository(applicationContext),
                                        ServiceLocator.provideNotificationRepository(applicationContext),
                                        ServiceLocator.providePreferencesManager(applicationContext),
                                        ServiceLocator.provideSetBrightnessUseCase(applicationContext)
                                    )
                                }
                                DashboardScreen(
                                    viewModel = viewModel,
                                    isDarkTheme = darkTheme,
                                    onToggleTheme = { darkTheme = !darkTheme },
                                    onEnableBluetooth = {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        enableBluetoothLauncher.launch(enableBtIntent)
                                    },
                                    onNavigateToScanner = { navController.navigate("scanner") }
                                )
                            }
                            composable("scanner") {
                                val viewModel: BleScannerViewModel = viewModel {
                                    BleScannerViewModel(
                                        ServiceLocator.provideBleScanner(),
                                        ServiceLocator.provideBleRepository(applicationContext)
                                    )
                                }
                                ScannerScreen(
                                    viewModel = viewModel,
                                    onEnableBluetooth = {
                                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                        enableBluetoothLauncher.launch(enableBtIntent)
                                    },
                                    onDeviceSelected = { navController.popBackStack() },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("rules") {
                                val viewModel: RulesViewModel = viewModel {
                                    RulesViewModel(
                                        ServiceLocator.provideGetInstalledAppsUseCase(applicationContext),
                                        ServiceLocator.provideSaveAppRuleUseCase(applicationContext),
                                        ServiceLocator.provideSaveAppRulesUseCase(applicationContext)
                                    )
                                }
                                RulesScreen(
                                    viewModel = viewModel,
                                    isDarkTheme = darkTheme,
                                    onToggleTheme = { darkTheme = !darkTheme }
                                )
                            }
                            composable("logs") {
                                val viewModel: LogsViewModel = viewModel {
                                    LogsViewModel(ServiceLocator.provideNotificationRepository(applicationContext))
                                }
                                LogsScreen(
                                    viewModel = viewModel,
                                    isDarkTheme = darkTheme,
                                    onToggleTheme = { darkTheme = !darkTheme },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    selectedPalette = selectedPalette,
                                    onPaletteChange = { selectedPalette = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
