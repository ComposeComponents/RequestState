package cl.emilym.compose.requeststate

import cl.emilym.compose.requeststate.internal.FlatRequestStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(
    showLoading: Boolean = true,
    operation: suspend () -> Flow<T>
): RequestStateFlow<T> =
    flatRequestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output
 *
 * @param token An external token passed in to retry the RequestState
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(
    token: RetryToken,
    showLoading: Boolean = true,
    operation: suspend () -> Flow<T>
): RequestStateFlow<T> =
    flatRequestStateFlow(
        token = token,
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output
 *
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(
    config: RequestStateConfig,
    operation: suspend () -> Flow<T>
): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        config
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output
 *
 * @param token An external token passed in to retry the RequestState
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(
    token: RetryToken,
    config: RequestStateConfig,
    operation: suspend () -> Flow<T>
): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        config,
        token
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output with an upstream
 * flow
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.flatRequestStateFlow(
    showLoading: Boolean = true,
    operation: suspend (U) -> Flow<T>
): RequestStateFlow<T> =
    this.flatRequestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a Flow<RequestState> for a retry-able operation that emits a flow output with an upstream
 * flow
 *
 * @param token An external token passed in to retry the RequestState
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.flatRequestStateFlow(
    token: RetryToken,
    showLoading: Boolean = true,
    operation: suspend (U) -> Flow<T>
): RequestStateFlow<T> =
    this.flatRequestStateFlow(
        token = token,
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output with an upstream
 * flow
 *
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.flatRequestStateFlow(
    config: RequestStateConfig,
    operation: suspend (U) -> Flow<T>
): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        this,
        operation,
        config
    )
}

/**
 * Create a Flow<RequestState> for a retry-able operation that emits a flow output with an upstream
 * flow
 *
 * @param token An external token passed in to retry the RequestState
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.flatRequestStateFlow(
    token: RetryToken,
    config: RequestStateConfig,
    operation: suspend (U) -> Flow<T>
): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        this,
        operation,
        config,
        token
    )
}