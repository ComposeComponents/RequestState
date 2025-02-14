package cl.emilym.compose.requeststate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.merge

/**
 * Create a RequestStateFlow for a retry-able operation that emits a single output
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(showLoading: Boolean = true, operation: suspend () -> T): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        showLoading
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
fun <U,T> Flow<U>.requestStateFlow(showLoading: Boolean = true, operation: suspend (U) -> T): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        this,
        operation,
        showLoading
    )
}

/**
 * Create a RequestStateFlow for a retry-able operation that emits a flow output
 *
 * @param showLoading Whether to silently complete the loading operation in the background
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(showLoading: Boolean = true, operation: suspend () -> Flow<T>): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        flowOf(Unit),
        {
            operation()
        },
        showLoading
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
fun <U,T> Flow<U>.flatRequestStateFlow(showLoading: Boolean = true, operation: suspend (U) -> Flow<T>): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        this,
        operation,
        showLoading
    )
}

interface RequestStateFlow<T>: Flow<RequestState<T>> {

    suspend fun retry()

}

internal abstract class AbstractRequestStateFlow<U, T>: RequestStateFlow<T> {

    protected abstract val showLoading: Boolean
    protected abstract val upstream: Flow<U>

    private val retryTrigger = Channel<Unit>(Channel.CONFLATED)

    override suspend fun retry() {
        retryTrigger.send(Unit)
    }

    protected abstract val block: suspend FlowCollector<RequestState<T>>.(U) -> Unit

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<RequestState<T>>) =
        upstream.flatMapLatest { up ->
            merge(
                flowOf(Unit),
                retryTrigger.consumeAsFlow()
            ).flatMapLatest {
                flow {
                    emit(RequestState.Loading())
                    block(this, up)
                }
            }
        }.collect(collector)
}

internal class FlatRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> Flow<T>,
    override val showLoading: Boolean
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
    override val showLoading: Boolean
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