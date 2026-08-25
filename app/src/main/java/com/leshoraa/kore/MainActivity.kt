package com.leshoraa.kore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
import com.leshoraa.kore.presentation.theme.KoReTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permission states handled by UI checks
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            KoReTheme {
                val navController = rememberNavController()
                
                LaunchedEffect(Unit) {
                    if (!PermissionManager.hasPermissions(this@MainActivity)) {
                        requestPermissionLauncher.launch(PermissionManager.requiredPermissions.toTypedArray())
                    }
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            val viewModel: DashboardViewModel = viewModel {
                                DashboardViewModel(
                                    ServiceLocator.provideBleManager(applicationContext),
                                    ServiceLocator.provideBleRepository(applicationContext),
                                    ServiceLocator.provideNotificationRepository(applicationContext)
                                )
                            }
                            DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToScanner = { navController.navigate("scanner") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToRules = { navController.navigate("rules") },
                                onNavigateToLogs = { navController.navigate("logs") }
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
                                onDeviceSelected = { navController.popBackStack() },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("rules") {
                            val viewModel: RulesViewModel = viewModel {
                                RulesViewModel(
                                    ServiceLocator.provideGetInstalledAppsUseCase(applicationContext),
                                    ServiceLocator.provideSaveAppRuleUseCase(applicationContext)
                                )
                            }
                            RulesScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("logs") {
                            val viewModel: LogsViewModel = viewModel {
                                LogsViewModel(ServiceLocator.provideNotificationRepository(applicationContext))
                            }
                            LogsScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(onNavigateBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
