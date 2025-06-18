package cl.emilym.compose.requeststate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge

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
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(showLoading: Boolean = true, operation: suspend () -> T): RequestStateFlow<T> =
    requestStateFlow(
        config = RequestStateConfig.fromShowLoading(showLoading),
        operation = operation
    )

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param config Configure the behaviour of the RequestStateFlow
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(config: RequestStateConfig, operation: suspend () -> T): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        config
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

interface RequestStateFlow<T>: Flow<RequestState<T>> {

    suspend fun retry()

}

internal abstract class AbstractRequestStateFlow<U, T>: RequestStateFlow<T> {

    protected abstract val config: RequestStateConfig
    protected abstract val upstream: Flow<U>

    private val retryTrigger = MutableSharedFlow<TriggerSource>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun retry() {
        retryTrigger.emit(TriggerSource.RETRY)
    }

    protected abstract val block: suspend FlowCollector<RequestState<T>>.(U) -> Unit

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<RequestState<T>>) =
        upstream.flatMapLatest { up ->
            merge(
                when (config.waitUntilRetry) {
                    true -> flowOf()
                    else -> flowOf(TriggerSource.EMISSION)
                },
                retryTrigger
            ).flatMapLatest { triggerSource ->
                flow {
                    if (
                        (triggerSource == TriggerSource.RETRY && config.showLoadingOnRetry) ||
                        (triggerSource == TriggerSource.EMISSION && config.showLoadingOnEmission)
                    ) emit(RequestState.Loading())
                    block(this, up)
                }.catch {
                    emit(RequestState.Failure(it))
                }
            }
        }.catch {
            emit(RequestState.Failure(it))
        }.collect(collector)

    private enum class TriggerSource {
        RETRY, EMISSION
    }
}

internal class FlatRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> Flow<T>,
    override val config: RequestStateConfig
): AbstractRequestStateFlow<U, T>() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val block: suspend FlowCollector<RequestState<T>>.(U) -> Unit = {
        emitAll(
            operation(it).mapLatest {
                RequestState.Success(it)
            }.catch {
                emit(RequestState.Failure(it))
            }
        )
    }

}

internal class SingleRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> T,
    override val config: RequestStateConfig
): AbstractRequestStateFlow<U, T>() {

    override val block: suspend FlowCollector<RequestState<T>>.(U) -> Unit = {
        emit(
            try {
                RequestState.Success(operation(it))
            } catch (e: Exception) {
                RequestState.Failure(e)
            }
        )
    }

}