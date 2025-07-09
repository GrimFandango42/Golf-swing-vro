package com.swingsync.ai.domain.usecase

import com.swingsync.ai.domain.util.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Base use case class for common use case functionality.
 * This class provides a foundation for all use cases in the application.
 */
abstract class BaseUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) {
    
    /**
     * Executes the use case with the given parameters.
     * This method handles the execution context and error handling.
     */
    suspend operator fun invoke(parameters: P): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute(parameters)
            }
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    /**
     * Abstract method to be implemented by concrete use cases.
     * This method contains the actual business logic.
     */
    @Throws(Exception::class)
    protected abstract suspend fun execute(parameters: P): Result<R>
}

/**
 * Base use case class for use cases that don't require parameters.
 */
abstract class BaseUseCaseNoParams<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) {
    
    /**
     * Executes the use case without parameters.
     */
    suspend operator fun invoke(): Result<R> {
        return try {
            withContext(coroutineDispatcher) {
                execute()
            }
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    /**
     * Abstract method to be implemented by concrete use cases.
     */
    @Throws(Exception::class)
    protected abstract suspend fun execute(): Result<R>
}

/**
 * Base use case class for use cases that return Flow.
 */
abstract class BaseFlowUseCase<in P, R>(
    private val coroutineDispatcher: CoroutineDispatcher
) {
    
    /**
     * Executes the use case and returns a Flow.
     */
    suspend operator fun invoke(parameters: P): kotlinx.coroutines.flow.Flow<R> {
        return withContext(coroutineDispatcher) {
            execute(parameters)
        }
    }
    
    /**
     * Abstract method to be implemented by concrete use cases.
     */
    @Throws(Exception::class)
    protected abstract suspend fun execute(parameters: P): kotlinx.coroutines.flow.Flow<R>
}

/**
 * Base use case class for use cases that return Flow and don't require parameters.
 */
abstract class BaseFlowUseCaseNoParams<R>(
    private val coroutineDispatcher: CoroutineDispatcher
) {
    
    /**
     * Executes the use case and returns a Flow.
     */
    suspend operator fun invoke(): kotlinx.coroutines.flow.Flow<R> {
        return withContext(coroutineDispatcher) {
            execute()
        }
    }
    
    /**
     * Abstract method to be implemented by concrete use cases.
     */
    @Throws(Exception::class)
    protected abstract suspend fun execute(): kotlinx.coroutines.flow.Flow<R>
}