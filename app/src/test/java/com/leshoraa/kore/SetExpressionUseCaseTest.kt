package com.leshoraa.kore

import com.leshoraa.kore.domain.model.Expression
import com.leshoraa.kore.domain.usecase.SetExpressionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetExpressionUseCaseTest {

    @Test
    fun `invoke with specific expression updates preferences and dispatches to BLE`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        val useCase = SetExpressionUseCase(fakeRepo, fakePrefs)

        val result = useCase(Expression.JOY)

        assertTrue(result.isSuccess)
        assertEquals(Expression.JOY, fakeRepo.lastSentExpression)
        assertEquals(Expression.JOY.code, fakePrefs.getSelectedExpressionCode())
    }

    @Test
    fun `invoke with all expressions maps codes accurately`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        val useCase = SetExpressionUseCase(fakeRepo, fakePrefs)

        for (expr in Expression.entries) {
            val result = useCase(expr)
            assertTrue(result.isSuccess)
            assertEquals(expr, fakeRepo.lastSentExpression)
            assertEquals(expr.code, fakePrefs.getSelectedExpressionCode())
        }
    }

    @Test
    fun `invoke with null resets to autonomous auto mood`() = runBlocking {
        val fakeRepo = FakeBleRepository()
        val fakePrefs = FakeUserPreferencesRepository()
        fakePrefs.setSelectedExpressionCode(Expression.ANGRY.code)
        val useCase = SetExpressionUseCase(fakeRepo, fakePrefs)

        val result = useCase(null)

        assertTrue(result.isSuccess)
        assertNull(fakeRepo.lastSentExpression)
        assertNull(fakePrefs.getSelectedExpressionCode())
    }

    @Test
    fun `Expression fromCode correctly resolves valid opcodes`() {
        assertEquals(Expression.IDLE, Expression.fromCode(0))
        assertEquals(Expression.JOY, Expression.fromCode(1))
        assertEquals(Expression.ANGRY, Expression.fromCode(2))
        assertEquals(Expression.SMIRK, Expression.fromCode(3))
        assertEquals(Expression.SHOCK, Expression.fromCode(4))
        assertEquals(Expression.OVERLOAD, Expression.fromCode(5))
        assertEquals(Expression.SAD, Expression.fromCode(6))
        assertEquals(Expression.DEADPAN, Expression.fromCode(7))
        assertNull(Expression.fromCode(-1))
        assertNull(Expression.fromCode(99))
    }
}
