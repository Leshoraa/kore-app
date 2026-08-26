package com.leshoraa.kore

import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.model.NavEvent
import com.leshoraa.kore.domain.model.NotificationEvent
import com.leshoraa.kore.domain.repository.BleRepository
import com.leshoraa.kore.domain.repository.UserPreferencesRepository
import com.leshoraa.kore.domain.usecase.SetBrightnessUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeBleRepository : BleRepository {
    private val _connectionState = MutableStateFlow(0)
    override val connectionState: StateFlow<Int> = _connectionState.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(true)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    var lastSentBrightness: Int? = null
    var lastSavedFlag: Boolean? = null
    var lastSentNavigation: NavEvent? = null
    var lastSentExpression: Expression? = null

    override fun connect(address: String, deviceName: String?) {}
    override fun disconnect() {}

    override suspend fun sendNotification(event: NotificationEvent): Result<Unit> = Result.success(Unit)

    override suspend fun sendBrightness(brightness: Int, save: Boolean): Result<Unit> {
        lastSentBrightness = brightness
        lastSavedFlag = save
        return Result.success(Unit)
    }

    override suspend fun sendNavigation(event: NavEvent): Result<Unit> {
        lastSentNavigation = event
        return Result.success(Unit)
    }

    override suspend fun sendExpression(expression: Expression?): Result<Unit> {
        lastSentExpression = expression
        return Result.success(Unit)
    }
}

class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val _brightness = MutableStateFlow(128)
    override val brightness: StateFlow<Int> = _brightness.asStateFlow()

    private val _autoSync = MutableStateFlow(true)
    override val autoSyncBrightness: StateFlow<Boolean> = _autoSync.asStateFlow()

    private val _selectedExpressionCode = MutableStateFlow<Int?>(null)
    override val selectedExpressionCode: StateFlow<Int?> = _selectedExpressionCode.asStateFlow()

    override fun getBrightness(): Int = _brightness.value

    override fun setBrightness(value: Int) {
        _brightness.value = value
    }

    override fun isAutoSyncEnabled(): Boolean = _autoSync.value

    override fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSync.value = enabled
    }

    override fun getSelectedExpressionCode(): Int? = _selectedExpressionCode.value

    override fun setSelectedExpressionCode(code: Int?) {
        _selectedExpressionCode.value = code
    }
}

class SetBrightnessUseCaseTest {

    @Test
    fun `invoke with normal value updates preferences and dispatches to BLE`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        val useCase = SetBrightnessUseCase(fakeRepo, fakePrefs)

        val result = useCase(200, save = true)

        assertTrue(result.isSuccess)
        assertEquals(200, fakeRepo.lastSentBrightness)
        assertEquals(true, fakeRepo.lastSavedFlag)
        assertEquals(200, fakePrefs.getBrightness())
    }

    @Test
    fun `invoke with out of bound value clamps to 0 and 255`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        val useCase = SetBrightnessUseCase(fakeRepo, fakePrefs)

        useCase(-50, save = true)
        assertEquals(0, fakeRepo.lastSentBrightness)
        assertEquals(0, fakePrefs.getBrightness())

        useCase(300, save = true)
        assertEquals(255, fakeRepo.lastSentBrightness)
        assertEquals(255, fakePrefs.getBrightness())
    }

    @Test
    fun `invoke with save false does not overwrite preferences`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        fakePrefs.setBrightness(128)
        val useCase = SetBrightnessUseCase(fakeRepo, fakePrefs)

        val result = useCase(50, save = false)

        assertTrue(result.isSuccess)
        assertEquals(50, fakeRepo.lastSentBrightness)
        assertEquals(false, fakeRepo.lastSavedFlag)
        assertEquals(128, fakePrefs.getBrightness())
    }
}
