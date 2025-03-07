package cl.emilym.compose.requeststate

fun <T,O> RequestState<T>.map(
    transform: (T) -> O
): RequestState<O> {
    return when (this) {
        is RequestState.Initial -> RequestState.Initial()
        is RequestState.Loading -> RequestState.Loading()
        is RequestState.Failure -> RequestState.Failure(exception)
        is RequestState.Success -> RequestState.Success(transform(value))
    }
}

fun <T> RequestState<T>.unwrap(): T? {
    return when (this) {
        is RequestState.Success -> value
        else -> null
    }
}

fun <T> RequestState<T>.unwrap(default: T): T {
    return when (this) {
        is RequestState.Success -> value
        else -> default
    }
}

fun <T> RequestState<T>.unwrapNullable(default: T? = null): T? {
    return when (this) {
        is RequestState.Success -> value
        else -> default
    }
}