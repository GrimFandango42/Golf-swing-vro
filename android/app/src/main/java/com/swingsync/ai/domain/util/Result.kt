package com.swingsync.ai.domain.util

/**
 * A generic wrapper class for handling success and error states in a clean way.
 * This follows the Result pattern commonly used in clean architecture.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    data class Loading(val isLoading: Boolean = true) : Result<Nothing>()
}

/**
 * Returns true if the result is successful, false otherwise.
 */
fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success

/**
 * Returns true if the result is an error, false otherwise.
 */
fun <T> Result<T>.isError(): Boolean = this is Result.Error

/**
 * Returns true if the result is in loading state, false otherwise.
 */
fun <T> Result<T>.isLoading(): Boolean = this is Result.Loading

/**
 * Returns the data if the result is successful, null otherwise.
 */
fun <T> Result<T>.getOrNull(): T? = when (this) {
    is Result.Success -> data
    else -> null
}

/**
 * Returns the exception if the result is an error, null otherwise.
 */
fun <T> Result<T>.getExceptionOrNull(): Exception? = when (this) {
    is Result.Error -> exception
    else -> null
}

/**
 * Executes the given function on the encapsulated value if this instance represents success.
 */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

/**
 * Executes the given function on the encapsulated exception if this instance represents error.
 */
inline fun <T> Result<T>.onError(action: (Exception) -> Unit): Result<T> {
    if (this is Result.Error) {
        action(exception)
    }
    return this
}

/**
 * Executes the given function if this instance represents loading state.
 */
inline fun <T> Result<T>.onLoading(action: () -> Unit): Result<T> {
    if (this is Result.Loading) {
        action()
    }
    return this
}

/**
 * Transforms a successful result to another type using the given transform function.
 */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading(isLoading)
    }
}

/**
 * Transforms a successful result to another Result using the given transform function.
 */
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> {
    return when (this) {
        is Result.Success -> transform(data)
        is Result.Error -> Result.Error(exception)
        is Result.Loading -> Result.Loading(isLoading)
    }
}

/**
 * Returns the encapsulated value if this instance represents success or throws the encapsulated exception if it is error.
 */
fun <T> Result<T>.getOrThrow(): T {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> throw exception
        is Result.Loading -> throw IllegalStateException("Cannot get value while loading")
    }
}

/**
 * Returns the encapsulated value if this instance represents success or the specified default value if it is error or loading.
 */
fun <T> Result<T>.getOrDefault(defaultValue: T): T {
    return when (this) {
        is Result.Success -> data
        else -> defaultValue
    }
}

/**
 * Returns the encapsulated value if this instance represents success or the result of calling the specified function if it is error or loading.
 */
inline fun <T> Result<T>.getOrElse(onFailure: (Exception?) -> T): T {
    return when (this) {
        is Result.Success -> data
        is Result.Error -> onFailure(exception)
        is Result.Loading -> onFailure(null)
    }
}

/**
 * Wraps a function call in a try-catch block and returns a Result.
 */
inline fun <T> safeCall(action: () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(e)
    }
}

/**
 * Wraps a suspend function call in a try-catch block and returns a Result.
 */
suspend inline fun <T> safeSuspendCall(crossinline action: suspend () -> T): Result<T> {
    return try {
        Result.Success(action())
    } catch (e: Exception) {
        Result.Error(e)
    }
}