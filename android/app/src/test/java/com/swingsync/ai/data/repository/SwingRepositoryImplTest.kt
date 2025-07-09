package com.swingsync.ai.data.repository

import com.swingsync.ai.data.datasource.local.LocalDataSource
import com.swingsync.ai.data.datasource.remote.RemoteDataSource
import com.swingsync.ai.data.local.entity.SwingSessionEntity
import com.swingsync.ai.data.mapper.SwingDataMapper
import com.swingsync.ai.domain.model.SwingSession
import com.swingsync.ai.domain.util.Result
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
 * Unit tests for SwingRepositoryImpl
 */
class SwingRepositoryImplTest {
    
    @Mock
    private lateinit var localDataSource: LocalDataSource
    
    @Mock
    private lateinit var remoteDataSource: RemoteDataSource
    
    @Mock
    private lateinit var mapper: SwingDataMapper
    
    private lateinit var swingRepository: SwingRepositoryImpl
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        swingRepository = SwingRepositoryImpl(localDataSource, remoteDataSource, mapper)
    }
    
    @Test
    fun `createSwingSession should map domain to entity and call local data source`() = runTest {\n        // Given\n        val swingSession = SwingSession(\n            sessionId = \"session123\",\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        val swingSessionEntity = SwingSessionEntity(\n            sessionId = \"session123\",\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        \n        whenever(mapper.toEntity(swingSession)).thenReturn(swingSessionEntity)\n        whenever(localDataSource.insertSwingSession(swingSessionEntity)).thenReturn(Result.Success(Unit))\n        \n        // When\n        val result = swingRepository.createSwingSession(swingSession)\n        \n        // Then\n        assertTrue(result is Result.Success)\n        verify(mapper).toEntity(swingSession)\n        verify(localDataSource).insertSwingSession(swingSessionEntity)\n    }\n    \n    @Test\n    fun `getSwingSession should call local data source and map entity to domain`() = runTest {\n        // Given\n        val sessionId = \"session123\"\n        val swingSessionEntity = SwingSessionEntity(\n            sessionId = sessionId,\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        val swingSession = SwingSession(\n            sessionId = sessionId,\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        \n        whenever(localDataSource.getSwingSession(sessionId)).thenReturn(Result.Success(swingSessionEntity))\n        whenever(mapper.toDomain(swingSessionEntity)).thenReturn(swingSession)\n        \n        // When\n        val result = swingRepository.getSwingSession(sessionId)\n        \n        // Then\n        assertTrue(result is Result.Success)\n        assertEquals(swingSession, (result as Result.Success).data)\n        verify(localDataSource).getSwingSession(sessionId)\n        verify(mapper).toDomain(swingSessionEntity)\n    }\n    \n    @Test\n    fun `getSwingSession should handle null entity`() = runTest {\n        // Given\n        val sessionId = \"session123\"\n        whenever(localDataSource.getSwingSession(sessionId)).thenReturn(Result.Success(null))\n        \n        // When\n        val result = swingRepository.getSwingSession(sessionId)\n        \n        // Then\n        assertTrue(result is Result.Success)\n        assertNull((result as Result.Success).data)\n        verify(localDataSource).getSwingSession(sessionId)\n    }\n    \n    @Test\n    fun `createSwingSession should propagate local data source errors`() = runTest {\n        // Given\n        val swingSession = SwingSession(\n            sessionId = \"session123\",\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        val swingSessionEntity = SwingSessionEntity(\n            sessionId = \"session123\",\n            userId = \"user123\",\n            clubUsed = \"Driver\",\n            startTime = 1000L,\n            endTime = null,\n            totalFrames = 0,\n            fps = 30f,\n            videoPath = null,\n            isCompleted = false,\n            createdAt = 1000L,\n            updatedAt = 1000L\n        )\n        val error = Exception(\"Database error\")\n        \n        whenever(mapper.toEntity(swingSession)).thenReturn(swingSessionEntity)\n        whenever(localDataSource.insertSwingSession(swingSessionEntity)).thenReturn(Result.Error(error))\n        \n        // When\n        val result = swingRepository.createSwingSession(swingSession)\n        \n        // Then\n        assertTrue(result is Result.Error)\n        assertEquals(error, (result as Result.Error).exception)\n    }\n}"