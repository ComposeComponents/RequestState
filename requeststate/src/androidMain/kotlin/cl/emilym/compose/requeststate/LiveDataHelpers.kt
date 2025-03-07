package cl.emilym.compose.requeststate

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData

suspend fun <T> MutableLiveData<RequestState<T>>.handle(hideLoading: Boolean = false, operation: suspend () -> T) {
    if (!hideLoading) value = RequestState.Loading()
    value = try {
        RequestState.Success(operation())
    } catch (e: Exception) {
        RequestState.Failure(e)
    }
}

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

fun <T> LiveData<RequestState<T>>.unwrap(default: T): LiveData<T> {
    val mediator = MediatorLiveData<T>()
    mediator.addSource(this) {
        mediator.value = it.unwrap(default)
    }

    return mediator
}

fun <T> LiveData<RequestState<T>>.unwrapNullable(default: T? = null): LiveData<T?> {
    val mediator = MediatorLiveData<T?>()
    mediator.addSource(this) {
        mediator.value = it.unwrapNullable(default)
    }

    return mediator
}