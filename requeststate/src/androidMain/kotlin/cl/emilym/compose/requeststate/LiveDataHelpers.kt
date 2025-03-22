package cl.emilym.compose.requeststate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

/**
 * Execute a coroutine operation, automatically updating the representing state flow to show loading,
 * success, or failure (if an exception is thrown)
 *
 * @param hideLoading Whether to emit the loading state before executing the operation, disable for
 * background loading operations
 * @param operation The operation to execute
 */
suspend fun <T> MutableLiveData<RequestState<T>>.handle(hideLoading: Boolean = false, operation: suspend () -> T) {
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
fun <T,O> LiveData<RequestState<T>>.child(
    transform: (T) -> O
): LiveData<RequestState<O>> {
    val mediator = MediatorLiveData<RequestState<O>>()
    mediator.addSource(this) {
        mediator.value = it.map { transform(it) }
    }

    return mediator
}

fun <T> LiveData<RequestState<T>>.unwrap(): LiveData<T?> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        mediator.value = it.unwrap()
    }

    return mediator
}

/**
 * "Unwrap" a request state into its successful value or a default
 *
 * @param default The default value
 */
fun <T> LiveData<RequestState<T>>.unwrap(default: T): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        mediator.value = it.unwrap(default)
    }

    return mediator
}

/**
 * "Unwrap" a request state into its successful value or a default
 *
 * @param default The default value, which can be null
 */
fun <T> LiveData<RequestState<T>>.unwrapNullable(default: T? = null): LiveData<T?> {
    val mediator = MediatorLiveData<T?>()
    mediator.addSource(this) {
        mediator.value = it.unwrap() ?: default
    }

    return mediator
}