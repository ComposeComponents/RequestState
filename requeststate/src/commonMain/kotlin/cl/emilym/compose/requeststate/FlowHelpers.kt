package cl.emilym.compose.requeststate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Execute a coroutine operation, automatically updating the representing state flow to show loading,
 * success, or failure (if an exception is thrown)
 *
 * @param hideLoading Whether to emit the loading state before executing the operation, disable for
 * background loading operations
 * @param operation The operation to execute
 */
suspend fun <T> MutableStateFlow<RequestState<T>>.handle(hideLoading: Boolean = false, operation: suspend () -> T) {
    if (!hideLoading) value = RequestState.Loading()
    value = try {
        RequestState.Success(operation())
    } catch (e: Exception) {
        RequestState.Failure(e)
    }
}

/**
 * Map a successful request state to another value and transparently pass through all others
 *
 * @param transform Modify the result of a successful operation
 */
fun <T,O> Flow<RequestState<T>>.child(
    transform: (T) -> O
): Flow<RequestState<O>> {
    return map {
        when (it) {
            is RequestState.Initial -> RequestState.Initial()
            is RequestState.Loading -> RequestState.Loading()
            is RequestState.Failure -> RequestState.Failure(it.exception)
            is RequestState.Success -> RequestState.Success(transform(it.value))
        }
    }
}

/**
 * "Unwrap" a request state into its successful value or a default
 *
 * @param default The default value
 */
fun <T> Flow<RequestState<T>>.unwrap(default: T): Flow<T> {
    return map {
        when (it) {
            is RequestState.Success -> it.value
            else -> default
        }
    }
}

/**
 * "Unwrap" a request state into its successful value or a default
 *
 * @param default The default value, which can be null
 */
fun <T> Flow<RequestState<T>>.unwrapNullable(default: T? = null): Flow<T?> {
    return map {
        when (it) {
            is RequestState.Success -> it.value
            else -> default
        }
    }
}