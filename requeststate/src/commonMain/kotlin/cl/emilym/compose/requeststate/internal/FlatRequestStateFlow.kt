package cl.emilym.compose.requeststate.internal

import cl.emilym.compose.requeststate.RequestState
import cl.emilym.compose.requeststate.RequestStateConfig
import cl.emilym.compose.requeststate.RetryToken
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.mapLatest

internal enum class TriggerSource {
    RETRY, EMISSION
}

internal class FlatRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> Flow<T>,
    override val config: RequestStateConfig,
    override val token: RetryToken = RetryToken()
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