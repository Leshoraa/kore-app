package com.leshoraa.kore.core.common

import android.content.Context
import com.leshoraa.kore.core.ble.BleManager
import com.leshoraa.kore.core.ble.BleScanner
import com.leshoraa.kore.data.repository.BleRepositoryImpl
import com.leshoraa.kore.data.repository.NotificationRepositoryImpl
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.NotificationRepository
import com.leshoraa.kore.domain.usecase.ProcessNotificationUseCase

/**
 * Simple Service Locator for manual Dependency Injection.
 */
object ServiceLocator {
    private var bleManager: BleManager? = null
    private var bleScanner: BleScanner? = null

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

    fun provideNotificationRepository(): NotificationRepository {
        return NotificationRepositoryImpl()
    }

    fun provideProcessNotificationUseCase(context: Context): ProcessNotificationUseCase {
        return ProcessNotificationUseCase(
            provideBleRepository(context),
            provideNotificationRepository()
        )
    }
}
