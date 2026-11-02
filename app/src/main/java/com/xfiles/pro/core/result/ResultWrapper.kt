package com.xfiles.pro.core.result

/**
 * A sealed hierarchy representing the outcome of operations across architecture layers.
 */
sealed interface ResultWrapper<out T> {
    data class Success<out T>(val data: T) : ResultWrapper<T>
    data class Error(val throwable: Throwable, val message: String = throwable.localizedMessage ?: "Unknown error") : ResultWrapper<Nothing>
    data object Loading : ResultWrapper<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}

fun <T> ResultWrapper<T>.getOrDefault(defaultValue: T): T = when (this) {
    is ResultWrapper.Success -> data
    else -> defaultValue
}

inline fun <T, R> ResultWrapper<T>.map(transform: (T) -> R): ResultWrapper<R> = when (this) {
    is ResultWrapper.Success -> ResultWrapper.Success(transform(data))
    is ResultWrapper.Error -> this
    is ResultWrapper.Loading -> ResultWrapper.Loading
}

inline fun <T> ResultWrapper<T>.onSuccess(action: (T) -> Unit): ResultWrapper<T> {
    if (this is ResultWrapper.Success) action(data)
    return this
}

inline fun <T> ResultWrapper<T>.onError(action: (Throwable, String) -> Unit): ResultWrapper<T> {
    if (this is ResultWrapper.Error) action(throwable, message)
    return this
}
