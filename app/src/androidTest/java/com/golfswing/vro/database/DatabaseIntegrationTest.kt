package com.golfswing.vro.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.golfswing.vro.data.database.GolfSwingDatabase
import com.golfswing.vro.data.database.SwingAnalysisDao
import com.golfswing.vro.data.entity.SwingAnalysisEntity
import com.golfswing.vro.data.model.SwingAnalysis
import com.golfswing.vro.data.repository.SwingAnalysisRepository
import com.golfswing.vro.pixel.metrics.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

/**
 * Integration tests for database operations
 * Tests Room database, DAOs, and repository layer integration
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {

    private lateinit var database: GolfSwingDatabase
    private lateinit var swingAnalysisDao: SwingAnalysisDao
    private lateinit var swingAnalysisRepository: SwingAnalysisRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            GolfSwingDatabase::class.java
        ).allowMainThreadQueries().build()
        
        swingAnalysisDao = database.swingAnalysisDao()
        swingAnalysisRepository = SwingAnalysisRepository(swingAnalysisDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Basic CRUD Operations Tests
    @Test
    fun testInsertAndRetrieveSwingAnalysis() = runBlocking {
        val swingAnalysis = createTestSwingAnalysis()
        
        // Insert swing analysis
        val insertedId = swingAnalysisDao.insertSwingAnalysis(swingAnalysis)
        assertTrue("Insert should return valid ID", insertedId > 0)
        
        // Retrieve swing analysis
        val retrievedAnalysis = swingAnalysisDao.getSwingAnalysisById(insertedId)
        assertNotNull("Retrieved analysis should not be null", retrievedAnalysis)
        assertEquals("Analysis data should match", swingAnalysis.analysisData, retrievedAnalysis?.analysisData)
        assertEquals("Club type should match", swingAnalysis.clubType, retrievedAnalysis?.clubType)
        assertEquals("Score should match", swingAnalysis.score, retrievedAnalysis?.score)
        
        println("Database Integration: Insert and retrieve test passed")
    }

    @Test
    fun testUpdateSwingAnalysis() = runBlocking {
        val originalAnalysis = createTestSwingAnalysis()
        val insertedId = swingAnalysisDao.insertSwingAnalysis(originalAnalysis)
        
        // Update the analysis
        val updatedAnalysis = originalAnalysis.copy(
            id = insertedId,
            score = 8.5f,
            clubType = "DRIVER",
            analysisData = "Updated analysis data"
        )
        
        val updatedRows = swingAnalysisDao.updateSwingAnalysis(updatedAnalysis)
        assertEquals("Should update one row", 1, updatedRows)
        
        // Verify update
        val retrievedAnalysis = swingAnalysisDao.getSwingAnalysisById(insertedId)
        assertNotNull("Updated analysis should exist", retrievedAnalysis)
        assertEquals("Score should be updated", 8.5f, retrievedAnalysis?.score)
        assertEquals("Club type should be updated", "DRIVER", retrievedAnalysis?.clubType)
        assertEquals("Analysis data should be updated", "Updated analysis data", retrievedAnalysis?.analysisData)
        
        println("Database Integration: Update test passed")
    }

    @Test
    fun testDeleteSwingAnalysis() = runBlocking {
        val swingAnalysis = createTestSwingAnalysis()
        val insertedId = swingAnalysisDao.insertSwingAnalysis(swingAnalysis)
        
        // Verify insertion
        assertNotNull("Analysis should exist before deletion", 
            swingAnalysisDao.getSwingAnalysisById(insertedId))
        
        // Delete the analysis
        val deletedRows = swingAnalysisDao.deleteSwingAnalysisById(insertedId)
        assertEquals("Should delete one row", 1, deletedRows)
        
        // Verify deletion
        assertNull("Analysis should not exist after deletion", 
            swingAnalysisDao.getSwingAnalysisById(insertedId))
        
        println("Database Integration: Delete test passed")
    }

    // Query Operations Tests
    @Test
    fun testGetAllSwingAnalyses() = runBlocking {
        val analyses = listOf(
            createTestSwingAnalysis().copy(clubType = "DRIVER", score = 7.5f),
            createTestSwingAnalysis().copy(clubType = "IRON", score = 8.0f),
            createTestSwingAnalysis().copy(clubType = "WEDGE", score = 6.5f)
        )
        
        // Insert multiple analyses
        analyses.forEach { swingAnalysisDao.insertSwingAnalysis(it) }
        
        // Retrieve all analyses
        val allAnalyses = swingAnalysisDao.getAllSwingAnalyses()
        assertEquals("Should retrieve all inserted analyses", 3, allAnalyses.size)
        
        // Verify order (should be ordered by timestamp desc)
        val sortedAnalyses = allAnalyses.sortedByDescending { it.timestamp }
        assertEquals("Should be ordered by timestamp descending", sortedAnalyses, allAnalyses)
        
        println("Database Integration: Get all analyses test passed")
    }

    @Test
    fun testGetSwingAnalysesByClubType() = runBlocking {
        val driverAnalyses = listOf(
            createTestSwingAnalysis().copy(clubType = "DRIVER", score = 7.5f),
            createTestSwingAnalysis().copy(clubType = "DRIVER", score = 8.0f)
        )
        val ironAnalyses = listOf(
            createTestSwingAnalysis().copy(clubType = "IRON", score = 6.5f)
        )
        
        // Insert analyses
        (driverAnalyses + ironAnalyses).forEach { swingAnalysisDao.insertSwingAnalysis(it) }
        
        // Query by club type
        val driverResults = swingAnalysisDao.getSwingAnalysesByClubType("DRIVER")
        val ironResults = swingAnalysisDao.getSwingAnalysesByClubType("IRON")
        
        assertEquals("Should get correct number of driver analyses", 2, driverResults.size)
        assertEquals("Should get correct number of iron analyses", 1, ironResults.size)
        
        // Verify club types
        assertTrue("All results should be driver analyses", 
            driverResults.all { it.clubType == "DRIVER" })
        assertTrue("All results should be iron analyses", 
            ironResults.all { it.clubType == "IRON" })
        
        println("Database Integration: Get by club type test passed")
    }

    @Test
    fun testGetSwingAnalysesByDateRange() = runBlocking {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)
        
        val analyses = listOf(
            createTestSwingAnalysis().copy(timestamp = now),
            createTestSwingAnalysis().copy(timestamp = oneDayAgo),
            createTestSwingAnalysis().copy(timestamp = twoDaysAgo)
        )
        
        // Insert analyses
        analyses.forEach { swingAnalysisDao.insertSwingAnalysis(it) }
        
        // Query by date range (last 24 hours)
        val recentAnalyses = swingAnalysisDao.getSwingAnalysesByDateRange(oneDayAgo, now)
        assertEquals("Should get analyses from last 24 hours", 2, recentAnalyses.size)
        
        // Query by date range (last 2 days)
        val allAnalyses = swingAnalysisDao.getSwingAnalysesByDateRange(twoDaysAgo, now)
        assertEquals("Should get all analyses from last 2 days", 3, allAnalyses.size)
        
        println("Database Integration: Get by date range test passed")
    }

    // Repository Layer Tests
    @Test
    fun testRepositoryDataMapping() = runBlocking {
        val swingAnalysis = SwingAnalysis(
            id = 0,
            timestamp = System.currentTimeMillis(),
            clubType = "MID_IRON",
            score = 7.8f,
            analysisData = "Repository test data",
            swingPhase = "IMPACT",
            metrics = createTestMetrics()
        )
        
        // Save through repository
        val savedAnalysis = swingAnalysisRepository.saveSwingAnalysis(swingAnalysis)
        assertNotNull("Saved analysis should not be null", savedAnalysis)
        assertTrue("Saved analysis should have valid ID", savedAnalysis.id > 0)
        
        // Retrieve through repository
        val retrievedAnalysis = swingAnalysisRepository.getSwingAnalysisById(savedAnalysis.id)
        assertNotNull("Retrieved analysis should not be null", retrievedAnalysis)
        assertEquals("Analysis data should match", swingAnalysis.analysisData, retrievedAnalysis?.analysisData)
        assertEquals("Metrics should match", swingAnalysis.metrics, retrievedAnalysis?.metrics)
        
        println("Database Integration: Repository data mapping test passed")
    }

    @Test
    fun testRepositoryErrorHandling() = runBlocking {
        // Test retrieval of non-existent analysis
        val nonExistentAnalysis = swingAnalysisRepository.getSwingAnalysisById(999L)
        assertNull("Non-existent analysis should return null", nonExistentAnalysis)
        
        // Test retrieval with invalid club type
        val invalidClubAnalyses = swingAnalysisRepository.getAnalysesByClubType("INVALID_CLUB")
        assertTrue("Invalid club type should return empty list", invalidClubAnalyses.isEmpty())
        
        println("Database Integration: Repository error handling test passed")
    }

    // Transaction Tests
    @Test
    fun testTransactionRollback() = runBlocking {
        val analysis1 = createTestSwingAnalysis().copy(clubType = "DRIVER")
        val analysis2 = createTestSwingAnalysis().copy(clubType = "IRON")
        
        try {
            database.runInTransaction {
                swingAnalysisDao.insertSwingAnalysis(analysis1)
                swingAnalysisDao.insertSwingAnalysis(analysis2)
                
                // Simulate an error to trigger rollback
                throw RuntimeException("Simulated error")
            }
        } catch (e: RuntimeException) {
            // Expected error
        }
        
        // Verify that no data was persisted due to rollback
        val allAnalyses = swingAnalysisDao.getAllSwingAnalyses()
        assertTrue("No data should be persisted after rollback", allAnalyses.isEmpty())
        
        println("Database Integration: Transaction rollback test passed")
    }

    @Test
    fun testTransactionCommit() = runBlocking {
        val analysis1 = createTestSwingAnalysis().copy(clubType = "DRIVER")
        val analysis2 = createTestSwingAnalysis().copy(clubType = "IRON")
        
        database.runInTransaction {
            swingAnalysisDao.insertSwingAnalysis(analysis1)
            swingAnalysisDao.insertSwingAnalysis(analysis2)
            // No error - transaction should commit
        }
        
        // Verify that data was persisted
        val allAnalyses = swingAnalysisDao.getAllSwingAnalyses()
        assertEquals("Both analyses should be persisted", 2, allAnalyses.size)
        
        println("Database Integration: Transaction commit test passed")
    }

    // Performance Tests
    @Test
    fun testBulkInsertPerformance() = runBlocking {
        val analyses = (1..1000).map { index ->
            createTestSwingAnalysis().copy(
                clubType = if (index % 2 == 0) "DRIVER" else "IRON",
                score = (index % 10).toFloat() + 1f,
                analysisData = "Bulk insert test data $index"
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        // Bulk insert
        analyses.forEach { swingAnalysisDao.insertSwingAnalysis(it) }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Verify all records were inserted
        val allAnalyses = swingAnalysisDao.getAllSwingAnalyses()
        assertEquals("All analyses should be inserted", 1000, allAnalyses.size)
        
        // Performance assertion (should complete within reasonable time)
        assertTrue("Bulk insert should complete within 5 seconds", duration < 5000)
        
        println("Database Integration: Bulk insert performance test passed (${duration}ms)")
    }

    @Test
    fun testQueryPerformance() = runBlocking {
        // Insert test data
        val analyses = (1..100).map { index ->
            createTestSwingAnalysis().copy(
                clubType = listOf("DRIVER", "IRON", "WEDGE")[index % 3],
                score = (index % 10).toFloat() + 1f,
                timestamp = System.currentTimeMillis() - (index * 60000) // 1 minute intervals
            )
        }
        
        analyses.forEach { swingAnalysisDao.insertSwingAnalysis(it) }
        
        // Test query performance
        val startTime = System.currentTimeMillis()
        
        val driverAnalyses = swingAnalysisDao.getSwingAnalysesByClubType("DRIVER")
        val recentAnalyses = swingAnalysisDao.getSwingAnalysesByDateRange(
            System.currentTimeMillis() - (30 * 60000), // Last 30 minutes
            System.currentTimeMillis()
        )
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Verify queries returned expected results
        assertTrue("Driver analyses should be returned", driverAnalyses.isNotEmpty())
        assertTrue("Recent analyses should be returned", recentAnalyses.isNotEmpty())
        
        // Performance assertion
        assertTrue("Queries should complete within 1 second", duration < 1000)
        
        println("Database Integration: Query performance test passed (${duration}ms)")
    }

    // Data Integrity Tests
    @Test
    fun testDataIntegrityConstraints() = runBlocking {
        // Test with valid data
        val validAnalysis = createTestSwingAnalysis()
        val insertedId = swingAnalysisDao.insertSwingAnalysis(validAnalysis)
        assertTrue("Valid analysis should be inserted", insertedId > 0)
        
        // Test uniqueness constraints (if any)
        val duplicateAnalysis = validAnalysis.copy(id = 0)
        val duplicateId = swingAnalysisDao.insertSwingAnalysis(duplicateAnalysis)
        assertTrue("Duplicate analysis should be allowed (no unique constraints)", duplicateId > 0)
        
        println("Database Integration: Data integrity constraints test passed")
    }

    @Test
    fun testConcurrentAccess() = runBlocking {
        val analysis = createTestSwingAnalysis()
        val insertedId = swingAnalysisDao.insertSwingAnalysis(analysis)
        
        // Simulate concurrent reads
        val results = (1..10).map { index ->
            kotlinx.coroutines.async {
                swingAnalysisDao.getSwingAnalysisById(insertedId)
            }
        }
        
        // Wait for all reads to complete
        val retrievedAnalyses = results.map { it.await() }
        
        // Verify all reads succeeded
        assertTrue("All concurrent reads should succeed", 
            retrievedAnalyses.all { it != null })
        
        // Verify data consistency
        val firstResult = retrievedAnalyses.first()
        assertTrue("All reads should return consistent data", 
            retrievedAnalyses.all { it?.analysisData == firstResult?.analysisData })
        
        println("Database Integration: Concurrent access test passed")
    }

    // Helper methods
    private fun createTestSwingAnalysis(): SwingAnalysisEntity {
        return SwingAnalysisEntity(
            id = 0,
            timestamp = System.currentTimeMillis(),
            clubType = "MID_IRON",
            score = 7.5f,
            analysisData = "Test analysis data",
            swingPhase = "BACKSWING",
            xFactor = 35f,
            tempo = 3.2f,
            balance = 0.85f,
            consistency = 0.78f,
            powerGeneration = 750f,
            attackAngle = -2.5f,
            clubPath = 1.2f,
            swingPlane = 62f
        )
    }

    private fun createTestMetrics(): Map<String, Float> {
        return mapOf(
            "xFactor" to 35f,
            "tempo" to 3.2f,
            "balance" to 0.85f,
            "consistency" to 0.78f,
            "powerGeneration" to 750f,
            "attackAngle" to -2.5f,
            "clubPath" to 1.2f,
            "swingPlane" to 62f
        )
    }
}

// Mock repository implementation for testing
class SwingAnalysisRepository(private val dao: SwingAnalysisDao) {
    
    suspend fun saveSwingAnalysis(swingAnalysis: SwingAnalysis): SwingAnalysis {
        val entity = SwingAnalysisEntity(
            id = swingAnalysis.id,
            timestamp = swingAnalysis.timestamp,
            clubType = swingAnalysis.clubType,
            score = swingAnalysis.score,
            analysisData = swingAnalysis.analysisData,
            swingPhase = swingAnalysis.swingPhase,
            xFactor = swingAnalysis.metrics["xFactor"] ?: 0f,
            tempo = swingAnalysis.metrics["tempo"] ?: 0f,
            balance = swingAnalysis.metrics["balance"] ?: 0f,
            consistency = swingAnalysis.metrics["consistency"] ?: 0f,
            powerGeneration = swingAnalysis.metrics["powerGeneration"] ?: 0f,
            attackAngle = swingAnalysis.metrics["attackAngle"] ?: 0f,
            clubPath = swingAnalysis.metrics["clubPath"] ?: 0f,
            swingPlane = swingAnalysis.metrics["swingPlane"] ?: 0f
        )
        
        val insertedId = dao.insertSwingAnalysis(entity)
        return swingAnalysis.copy(id = insertedId)
    }
    
    suspend fun getSwingAnalysisById(id: Long): SwingAnalysis? {
        val entity = dao.getSwingAnalysisById(id)
        return entity?.let { entityToSwingAnalysis(it) }
    }
    
    suspend fun getAnalysesByClubType(clubType: String): List<SwingAnalysis> {
        return dao.getSwingAnalysesByClubType(clubType).map { entityToSwingAnalysis(it) }
    }
    
    private fun entityToSwingAnalysis(entity: SwingAnalysisEntity): SwingAnalysis {
        return SwingAnalysis(
            id = entity.id,
            timestamp = entity.timestamp,
            clubType = entity.clubType,
            score = entity.score,
            analysisData = entity.analysisData,
            swingPhase = entity.swingPhase,
            metrics = mapOf(
                "xFactor" to entity.xFactor,
                "tempo" to entity.tempo,
                "balance" to entity.balance,
                "consistency" to entity.consistency,
                "powerGeneration" to entity.powerGeneration,
                "attackAngle" to entity.attackAngle,
                "clubPath" to entity.clubPath,
                "swingPlane" to entity.swingPlane
            )
        )
    }
}