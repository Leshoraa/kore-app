package com.leshoraa.kore.core.common

import android.content.Context
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.ble.BleScanner
import com.leshoraa.kore.core.database.AppDatabase
import com.leshoraa.kore.data.repository.BleRepositoryImpl
import com.leshoraa.kore.data.repository.CameraVisionRepositoryImpl
import com.leshoraa.kore.data.repository.NotificationRepositoryImpl
import com.leshoraa.kore.data.repository.RuleRepositoryImpl
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.CameraVisionRepository
import com.leshoraa.kore.domain.repository.NotificationRepository
import com.leshoraa.kore.domain.repository.RuleRepository
import com.leshoraa.kore.domain.usecase.*

/**
 * Simple Service Locator for manual Dependency Injection.
 */
object ServiceLocator {
    private var bleManager: BleManager? = null
    private var bleScanner: BleScanner? = null
    private var database: AppDatabase? = null
    private var preferencesManager: PreferencesManager? = null
    private var cameraVisionRepository: CameraVisionRepository? = null

    private fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: AppDatabase.getDatabase(context).also { database = it }
        }
    }

    fun provideBleManager(context: Context): BleManager {
        return bleManager ?: synchronized(this) {
            bleManager ?: BleManager(context.applicationContext).also { bleManager = it }
        }
    }

    fun provideBleScanner(): BleScanner {
        return bleScanner ?: synchronized(this) {
            bleScanner ?: BleScanner().also { bleScanner = it }
        }
    }

    fun provideBleRepository(context: Context): BleRepository {
        return BleRepositoryImpl(provideBleManager(context))
    }

    fun provideCameraVisionRepository(context: Context): CameraVisionRepository {
        return cameraVisionRepository ?: synchronized(this) {
            cameraVisionRepository ?: CameraVisionRepositoryImpl(context.applicationContext).also { cameraVisionRepository = it }
        }
    }

    fun provideGetCameraStreamUseCase(context: Context): GetCameraStreamUseCase {
        return GetCameraStreamUseCase(provideCameraVisionRepository(context))
    }

    fun provideGetTelemetryStreamUseCase(context: Context): GetTelemetryStreamUseCase {
        return GetTelemetryStreamUseCase(provideCameraVisionRepository(context))
    }

    fun provideUpdateCameraSensorUseCase(context: Context): UpdateCameraSensorUseCase {
        return UpdateCameraSensorUseCase(provideCameraVisionRepository(context))
    }

    fun provideRuleRepository(context: Context): RuleRepository {
        return RuleRepositoryImpl(provideDatabase(context).ruleDao())
    }

    fun provideNotificationRepository(context: Context): NotificationRepository {
        return NotificationRepositoryImpl(provideDatabase(context).notificationLogDao())
    }

    fun provideProcessNotificationUseCase(context: Context): ProcessNotificationUseCase {
        return ProcessNotificationUseCase(
            provideBleRepository(context),
            provideNotificationRepository(context),
            provideFilterAppRuleUseCase(context)
        )
    }

    fun provideFilterAppRuleUseCase(context: Context): FilterAppRuleUseCase {
        return FilterAppRuleUseCase(provideRuleRepository(context))
    }

    fun provideGetInstalledAppsUseCase(context: Context): GetInstalledAppsUseCase {
        return GetInstalledAppsUseCase(context.applicationContext, provideRuleRepository(context))
    }

    fun provideSaveAppRuleUseCase(context: Context): SaveAppRuleUseCase {
        return SaveAppRuleUseCase(provideRuleRepository(context))
    }

    fun provideSaveAppRulesUseCase(context: Context): SaveAppRulesUseCase {
        return SaveAppRulesUseCase(provideRuleRepository(context))
    }

    fun providePreferencesManager(context: Context): PreferencesManager {
        return preferencesManager ?: synchronized(this) {
            preferencesManager ?: PreferencesManager(context.applicationContext).also { preferencesManager = it }
        }
    }

    fun provideSetBrightnessUseCase(context: Context): SetBrightnessUseCase {
        return SetBrightnessUseCase(
            provideBleRepository(context),
            providePreferencesManager(context)
        )
    }

    fun provideSetExpressionUseCase(context: Context): SetExpressionUseCase {
        return SetExpressionUseCase(
            provideBleRepository(context),
            providePreferencesManager(context)
        )
    }

    fun provideProcessNavigationUseCase(context: Context): ProcessNavigationUseCase {
        return ProcessNavigationUseCase(
            provideBleRepository(context)
        )
    }
}

