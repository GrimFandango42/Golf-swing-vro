package com.swingsync.ai.domain.usecase.swing

import com.swingsync.ai.domain.model.SwingSession
import com.swingsync.ai.domain.repository.SwingRepository
import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for CreateSwingSessionUseCase
 */
class CreateSwingSessionUseCaseTest {
    
    @Mock
    private lateinit var swingRepository: SwingRepository
    
    private lateinit var createSwingSessionUseCase: CreateSwingSessionUseCase
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        createSwingSessionUseCase = CreateSwingSessionUseCase(swingRepository, Dispatchers.Unconfined)
    }
    
    @Test
    fun `invoke with valid parameters should create swing session successfully`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            fps = 30f\n        )\n        whenever(swingRepository.createSwingSession(any())).thenReturn(Result.Success(Unit))\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Success)\n        verify(swingRepository).createSwingSession(any())\n    }\n    \n    @Test\n    fun `invoke with blank userId should return error`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"\",\n            clubUsed = \"Driver\",\n            fps = 30f\n        )\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Error)\n        assertEquals(\"User ID cannot be blank\", (result as Result.Error).exception.message)\n    }\n    \n    @Test\n    fun `invoke with blank clubUsed should return error`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"user123\",\n            clubUsed = \"\",\n            fps = 30f\n        )\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Error)\n        assertEquals(\"Club type cannot be blank\", (result as Result.Error).exception.message)\n    }\n    \n    @Test\n    fun `invoke with negative fps should return error`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            fps = -1f\n        )\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Error)\n        assertEquals(\"FPS must be positive\", (result as Result.Error).exception.message)\n    }\n    \n    @Test\n    fun `invoke should propagate repository errors`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            fps = 30f\n        )\n        val repositoryError = Exception(\"Repository error\")\n        whenever(swingRepository.createSwingSession(any())).thenReturn(Result.Error(repositoryError))\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Error)\n        assertEquals(repositoryError, (result as Result.Error).exception)\n    }\n    \n    @Test\n    fun `invoke should create session with correct properties`() = runTest {\n        // Given\n        val params = CreateSwingSessionUseCase.Params(\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            fps = 30f\n        )\n        var capturedSession: SwingSession? = null\n        whenever(swingRepository.createSwingSession(any())).thenAnswer { invocation ->\n            capturedSession = invocation.getArgument(0)\n            Result.Success(Unit)\n        }\n        \n        // When\n        val result = createSwingSessionUseCase(params)\n        \n        // Then\n        assertTrue(result is Result.Success)\n        assertNotNull(capturedSession)\n        assertEquals(params.userId, capturedSession?.userId)\n        assertEquals(params.clubUsed, capturedSession?.clubUsed)\n        assertEquals(params.fps, capturedSession?.fps)\n        assertEquals(0, capturedSession?.totalFrames)\n        assertEquals(false, capturedSession?.isCompleted)\n        assertNull(capturedSession?.endTime)\n    }\n}"