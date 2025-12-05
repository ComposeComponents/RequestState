package cl.emilym.compose.requeststate

import cl.emilym.compose.requeststate.internal.TriggerSource
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Catches exceptions emitted by operations and allows them to be retried, showing intermediate
 * loading and initial states.
 *
 * ```kotlin
 * val flow = requestStateFlow { ... }
 *
 * launch {
 *      flow.collect { value ->
 *          when (value) {
 *              // Exceptions are caught and propagated as RequestState.Failure
 *              // retry() restarts the operation
 *              is RequestState.Failure -> flow.retry()
 *              is RequestState.Success -> it.value // The result of the operation
 *              else -> {}
 *          }
 *      }
 * }
 */
interface RequestStateFlow<T>: Flow<RequestState<T>> {

    suspend fun retry()

}

/**
 * @property showLoadingOnEmission Whether to emit a RequestState.Loading value when subscribing or
 * there is a new upstream emission.
 * @property showLoadingOnRetry Whether to emit a RequestState.Loading value when retry() is called.
 * @property waitUntilRetry Whether to wait until a retry() call is executed before further computation.
 *
 * This is useful in the case where a UI element needs to trigger some kind of data operation.
 */
data class RequestStateConfig(
    val showLoadingOnEmission: Boolean = true,
    val showLoadingOnRetry: Boolean = true,
    val waitUntilRetry: Boolean = false
) {

    companion object {

        internal fun fromShowLoading(showLoading: Boolean): RequestStateConfig =
            RequestStateConfig(
                showLoadingOnEmission = showLoading,
                showLoadingOnRetry = showLoading
            )

    }

}

/**
 * Can be passed to a RequestStateFlow to externally trigger a retry without having access to the
 * instance itself.
 *
 * ```kotlin
 * val token = RetryToken()
 * val flow = requestStateFlow(token) { ... }
 * ...
 * // Retries "flow"
 * token.retry()
 * ```
 *
 * Can be used to retry multiple flows at once:
 * ```kotlin
 * val token = RetryToken()
 * val flow1 = requestStateFlow(token) { ... }
 * val flow2 = requestStateFlow(token) { ... }
 * ...
 * // Retries "flow1" and "flow2"
 * token.retry()
 * ```
 */
class RetryToken {
    internal val trigger = MutableSharedFlow<TriggerSource>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    suspend fun retry() {
        trigger.emit(TriggerSource.RETRY)
    }
}