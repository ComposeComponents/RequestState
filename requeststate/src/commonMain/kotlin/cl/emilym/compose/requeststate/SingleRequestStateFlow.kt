package cl.emilym.compose.requeststate

import cl.emilym.compose.requeststate.internal.SingleRequestStateFlow
import cl.emilym.compose.requeststate.internal.TriggerSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(
    showLoading: Boolean = true,
    operation: suspend () -> T
): RequestStateFlow<T> =
    requestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param token An external token passed in to retry the RequestState
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(
    token: RetryToken,
    showLoading: Boolean = true,
    operation: suspend () -> T
): RequestStateFlow<T> =
    requestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation,
        token = token
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(
    config: RequestStateConfig,
    operation: suspend () -> T
): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        config
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param token An external token passed in to retry the RequestState
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(
    token: RetryToken,
    config: RequestStateConfig,
    operation: suspend () -> T
): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        config,
        token
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output with an upstream
 * flow
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(
    showLoading: Boolean = true,
    operation: suspend (U) -> T
): RequestStateFlow<T> =
    this.requestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output with an upstream
 * flow
 *
 * @param token An external token passed in to retry the RequestState
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(
    token: RetryToken,
    showLoading: Boolean = true,
    operation: suspend (U) -> T
): RequestStateFlow<T> =
    this.requestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation,
        token = token
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output with an upstream
 * flow
 *
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(
    config: RequestStateConfig,
    operation: suspend (U) -> T
): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        this,
        operation,
        config
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output with an upstream
 * flow
 *
 * @param token An external token passed in to retry the RequestState
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(
    token: RetryToken,
    config: RequestStateConfig,
    operation: suspend (U) -> T
): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        this,
        operation,
        config,
        token
    )
}