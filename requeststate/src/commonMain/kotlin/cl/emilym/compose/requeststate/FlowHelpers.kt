package cl.emilym.compose.requeststate

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

suspend fun <T> MutableStateFlow<RequestState<T>>.handle(hideLoading: Boolean = false, operation: suspend () -> T) {
    if (!hideLoading) value = RequestState.Loading()
    value = try {
        RequestState.Success(operation())
    } catch (e: Exception) {
        RequestState.Failure(e)
    }
}

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

fun <T> Flow<RequestState<T>>.unwrap(default: T): Flow<T> {
    return map {
        when (it) {
            is RequestState.Success -> it.value
            else -> default
        }
    }
}

fun <T> Flow<RequestState<T>>.unwrapNullable(default: T? = null): Flow<T?> {
    return map {
        when (it) {
            is RequestState.Success -> it.value
            else -> default
        }
    }
}