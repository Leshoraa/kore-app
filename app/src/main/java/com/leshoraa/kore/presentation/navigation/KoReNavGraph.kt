package com.leshoraa.kore.presentation.navigation

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.leshoraa.kore.core.common.ServiceLocator
import com.leshoraa.kore.presentation.camera.CameraVisionScreen
import com.leshoraa.kore.presentation.camera.CameraVisionViewModel
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

/**
 * Central navigation graph for the application.
 * Separating navigation logic from Activity improves stability and testability.
 */
@Composable
fun KoReNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    selectedPalette: AppPalette,
    onPaletteChange: (AppPalette) -> Unit,
    enableBluetoothLauncher: ActivityResultLauncher<Intent>
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "dashboard",
        modifier = modifier
    ) {
        composable("dashboard") {
            val viewModel: DashboardViewModel = viewModel {
                DashboardViewModel(
                    ServiceLocator.provideBleManager(context),
                    ServiceLocator.provideBleRepository(context),
                    ServiceLocator.provideNotificationRepository(context),
                    ServiceLocator.providePreferencesManager(context),
                    ServiceLocator.provideSetBrightnessUseCase(context),
                    ServiceLocator.provideSetExpressionUseCase(context)
                )
            }
            DashboardScreen(
                viewModel = viewModel,
                isDarkTheme = darkTheme,
                onToggleTheme = onToggleTheme,
                onEnableBluetooth = {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                },
                onNavigateToScanner = { navController.navigate("scanner") },
                onNavigateToSettings = { section -> 
                    val route = if (section != null) "settings?scrollToSection=$section" else "settings"
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable("vision") {
            val viewModel: CameraVisionViewModel = viewModel {
                CameraVisionViewModel(
                    ServiceLocator.provideGetCameraStreamUseCase(context),
                    ServiceLocator.provideGetTelemetryStreamUseCase(context),
                    ServiceLocator.provideUpdateCameraSensorUseCase(context),
                    ServiceLocator.providePreferencesManager(context),
                    ServiceLocator.provideCameraVisionRepository(context),
                    ServiceLocator.provideBleRepository(context)
                )
            }
            CameraVisionScreen(
                viewModel = viewModel,
                isDarkTheme = darkTheme,
                onToggleTheme = onToggleTheme
            )
        }
        composable("scanner") {
            val viewModel: BleScannerViewModel = viewModel {
                BleScannerViewModel(
                    ServiceLocator.provideBleScanner(),
                    ServiceLocator.provideBleRepository(context)
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
                    ServiceLocator.provideGetInstalledAppsUseCase(context),
                    ServiceLocator.provideSaveAppRuleUseCase(context),
                    ServiceLocator.provideSaveAppRulesUseCase(context)
                )
            }
            RulesScreen(
                viewModel = viewModel,
                onToggleTheme = onToggleTheme,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("logs") {
            val viewModel: LogsViewModel = viewModel {
                LogsViewModel(ServiceLocator.provideNotificationRepository(context))
            }
            LogsScreen(
                viewModel = viewModel,
                onToggleTheme = onToggleTheme,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "settings?scrollToSection={section}",
            arguments = listOf(
                navArgument("section") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section")
            val viewModel: com.leshoraa.kore.presentation.settings.SettingsViewModel = viewModel {
                com.leshoraa.kore.presentation.settings.SettingsViewModel(
                    ServiceLocator.provideGetDeviceConfigUseCase(context),
                    ServiceLocator.provideSaveDeviceConfigUseCase(context),
                    ServiceLocator.provideGetWeatherConfigUseCase(context),
                    ServiceLocator.provideSaveWeatherConfigUseCase(context),
                    ServiceLocator.provideGetPhoneLocationUseCase(context),
                    ServiceLocator.provideSyncPhoneWeatherUseCase(context),
                    ServiceLocator.provideShowClockUseCase(context),
                    ServiceLocator.provideShowWeatherUseCase(context),
                    ServiceLocator.provideBleRepository(context)
                )
            }
            SettingsScreen(
                viewModel = viewModel,
                selectedPalette = selectedPalette,
                onPaletteChange = onPaletteChange,
                scrollToSection = section,
                onNavigateToFilters = { navController.navigate("rules") }
            )
        }
    }
}
