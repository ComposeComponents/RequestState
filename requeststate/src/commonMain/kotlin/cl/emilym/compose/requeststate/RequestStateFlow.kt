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
fun <T> requestStateFlow(operation: suspend () -> Flow<T>): RequestStateFlow<T> {
    return DefaultRequestStateFlow(
        flowOf(Unit),
    ) {
        operation()
    }
}

/**
 * @param operation The retry-able operation
 * @receiver An upstream flow who's value is passed to the retry-able operation
 */
fun <U,T> Flow<U>.requestStateFlow(operation: suspend (U) -> Flow<T>): RequestStateFlow<T> {
    return DefaultRequestStateFlow(
        this,
        operation
    )
}

interface RequestStateFlow<T>: Flow<RequestState<T>> {

    suspend fun retry()

}

internal class DefaultRequestStateFlow<U,T> internal constructor(
    private val upstream: Flow<U>,
    private val operation: suspend (U) -> Flow<T>
): RequestStateFlow<T> {

    private val retryTrigger = Channel<Unit>(Channel.CONFLATED).also { it.trySend(Unit) }

    override suspend fun retry() {
        retryTrigger.send(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collect(collector: FlowCollector<RequestState<T>>) =
        upstream.flatMapLatest { up ->
            retryTrigger.consumeAsFlow()
                .flatMapLatest {
                    flow<RequestState<T>> {
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
        }.collect(collector)

}