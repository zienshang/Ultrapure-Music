package com.ultrapuremusic.core.util

sealed class ResultWrapper<out T> {
    data class Success<T>(val data: T) : ResultWrapper<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ResultWrapper<Nothing>()
    data object Loading : ResultWrapper<Nothing>()

    val isSuccess get() = this is Success
    val isError get() = this is Error
    val isLoading get() = this is Loading

    fun getOrNull(): T? = if (this is Success) data else null

    fun getErrorOrNull(): String? = if (this is Error) message else null

    inline fun <R> map(transform: (T) -> R): ResultWrapper<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    inline fun onSuccess(action: (T) -> Unit): ResultWrapper<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (String, Throwable?) -> Unit): ResultWrapper<T> {
        if (this is Error) action(message, throwable)
        return this
    }

    inline fun onLoading(action: () -> Unit): ResultWrapper<T> {
        if (this is Loading) action()
        return this
    }
}

suspend fun <T> safeApiCall(apiCall: suspend () -> T): ResultWrapper<T> {
    return try {
        ResultWrapper.Success(apiCall())
    } catch (e: Exception) {
        ResultWrapper.Error(e.message ?: "Unknown error", e)
    }
}
