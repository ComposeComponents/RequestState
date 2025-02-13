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

/**
 * @param operation The retry-able operation
 */
fun <T> requestStateFlow(operation: suspend () -> T): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        flowOf(Unit),
    ) {
        operation()
    }
}

/**
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(operation: suspend (U) -> T): RequestStateFlow<T> {
    return SingleRequestStateFlow(
        this,
        operation
    )
}

/**
 * @param operation The retry-able operation
 */
fun <T> flatRequestStateFlow(operation: suspend () -> Flow<T>): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        flowOf(Unit),
    ) {
        operation()
    }
}

/**
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.flatRequestStateFlow(operation: suspend (U) -> Flow<T>): RequestStateFlow<T> {
    return FlatRequestStateFlow(
        this,
        operation
    )
}

interface RequestStateFlow<T>: Flow<RequestState<T>> {

    suspend fun retry()

}

internal abstract class AbstractRequestStateFlow<U, T>: RequestStateFlow<T> {
    
    protected abstract val upstream: Flow<U>

    private val retryTrigger = Channel<Unit>(Channel.CONFLATED).also { it.trySend(Unit) }

    override suspend fun retry() {
        retryTrigger.send(Unit)
    }

    protected abstract fun handle(up: U): Flow<RequestState<T>>

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<RequestState<T>>) =
        upstream.flatMapLatest { up ->
            retryTrigger.consumeAsFlow()
                .flatMapLatest {
                    handle(up)
                }
        }.collect(collector)
}

internal class FlatRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> Flow<T>
): AbstractRequestStateFlow<U, T>() {

    override fun handle(up: U) = flow<RequestState<T>> {
        emit(RequestState.Loading())
        emitAll(
            operation(up).mapLatest {
                RequestState.Success(it)
            }.catch {
                emit(RequestState.Failure(it))
            }
        )
    }

}

internal class SingleRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> T
): AbstractRequestStateFlow<U, T>() {

    override fun handle(up: U): Flow<RequestState<T>> = flow {
        emit(RequestState.Loading())
        emit(
            try {
                RequestState.Success(operation(up))
            } catch (e: Exception) {
                RequestState.Failure(e)
            }
        )
    }
}