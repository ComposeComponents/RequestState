package cl.emilym.compose.requeststate.internal

import cl.emilym.compose.requeststate.RequestState
import cl.emilym.compose.requeststate.RequestStateConfig
import cl.emilym.compose.requeststate.RequestStateFlow
import cl.emilym.compose.requeststate.RetryToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

internal abstract class AbstractRequestStateFlow<U, T>: RequestStateFlow<T> {

    protected abstract val config: RequestStateConfig
    protected abstract val upstream: Flow<U>
    protected abstract val token: RetryToken

    override suspend fun retry() {
        token.retry()
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
                token.trigger
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
}