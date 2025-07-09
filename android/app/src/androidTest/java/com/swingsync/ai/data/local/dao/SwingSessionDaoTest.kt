package com.swingsync.ai.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.swingsync.ai.data.local.database.SwingSyncDatabase
import com.swingsync.ai.data.local.entity.SwingSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SwingSessionDao
 */
@RunWith(AndroidJUnit4::class)
class SwingSessionDaoTest {
    
    private lateinit var database: SwingSyncDatabase
    private lateinit var swingSessionDao: SwingSessionDao
    
    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SwingSyncDatabase::class.java
        ).build()
        swingSessionDao = database.swingSessionDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndGetSession() = runTest {
        // Given
        val session = SwingSessionEntity(
            sessionId = \"session123\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        // When
        swingSessionDao.insertSession(session)
        val retrievedSession = swingSessionDao.getSessionById(\"session123\")
        
        // Then
        assertNotNull(retrievedSession)
        assertEquals(session, retrievedSession)
    }
    
    @Test
    fun getSessionsByUserId() = runTest {
        // Given
        val session1 = SwingSessionEntity(
            sessionId = \"session1\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        val session2 = SwingSessionEntity(
            sessionId = \"session2\",
            userId = \"user123\",
            clubUsed = \"Iron\",
            startTime = 2000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        
        val session3 = SwingSessionEntity(
            sessionId = \"session3\",
            userId = \"user456\",
            clubUsed = \"Driver\",
            startTime = 3000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 3000L,
            updatedAt = 3000L
        )
        
        // When
        swingSessionDao.insertSession(session1)
        swingSessionDao.insertSession(session2)
        swingSessionDao.insertSession(session3)
        
        val userSessions = swingSessionDao.getSessionsByUserId(\"user123\").first()
        
        // Then
        assertEquals(2, userSessions.size)
        assertTrue(userSessions.contains(session1))
        assertTrue(userSessions.contains(session2))
        assertFalse(userSessions.contains(session3))
    }
    
    @Test
    fun updateSession() = runTest {
        // Given
        val session = SwingSessionEntity(
            sessionId = \"session123\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        swingSessionDao.insertSession(session)
        
        // When
        val updatedSession = session.copy(
            endTime = 2000L,
            totalFrames = 100,
            isCompleted = true,
            updatedAt = 2000L
        )
        swingSessionDao.updateSession(updatedSession)
        
        val retrievedSession = swingSessionDao.getSessionById(\"session123\")
        
        // Then
        assertNotNull(retrievedSession)
        assertEquals(updatedSession, retrievedSession)
        assertEquals(2000L, retrievedSession?.endTime)
        assertEquals(100, retrievedSession?.totalFrames)
        assertEquals(true, retrievedSession?.isCompleted)
    }
    
    @Test
    fun deleteSession() = runTest {
        // Given
        val session = SwingSessionEntity(
            sessionId = \"session123\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        swingSessionDao.insertSession(session)
        
        // When
        swingSessionDao.deleteSessionById(\"session123\")
        val retrievedSession = swingSessionDao.getSessionById(\"session123\")
        
        // Then
        assertNull(retrievedSession)
    }
    
    @Test
    fun getCompletedSessions() = runTest {
        // Given
        val completedSession = SwingSessionEntity(
            sessionId = \"session1\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = 2000L,
            totalFrames = 100,
            fps = 30f,
            videoPath = null,
            isCompleted = true,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        
        val incompleteSession = SwingSessionEntity(
            sessionId = \"session2\",
            userId = \"user123\",
            clubUsed = \"Iron\",
            startTime = 3000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 3000L,
            updatedAt = 3000L
        )
        
        // When
        swingSessionDao.insertSession(completedSession)
        swingSessionDao.insertSession(incompleteSession)
        
        val completedSessions = swingSessionDao.getCompletedSessions(\"user123\", 10).first()
        
        // Then
        assertEquals(1, completedSessions.size)
        assertEquals(completedSession, completedSessions[0])
    }
    
    @Test
    fun getTotalSessionCount() = runTest {
        // Given
        val session1 = SwingSessionEntity(
            sessionId = \"session1\",
            userId = \"user123\",
            clubUsed = \"Driver\",
            startTime = 1000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        
        val session2 = SwingSessionEntity(
            sessionId = \"session2\",
            userId = \"user123\",
            clubUsed = \"Iron\",
            startTime = 2000L,
            endTime = null,
            totalFrames = 0,
            fps = 30f,
            videoPath = null,
            isCompleted = false,
            createdAt = 2000L,
            updatedAt = 2000L
        )
        
        // When
        swingSessionDao.insertSession(session1)
        swingSessionDao.insertSession(session2)
        
        val count = swingSessionDao.getTotalSessionCount(\"user123\")
        
        // Then
        assertEquals(2, count)
    }
}"