package cl.emilym.compose.requeststate.internal

import cl.emilym.compose.requeststate.RequestState
import cl.emilym.compose.requeststate.RequestStateConfig
import cl.emilym.compose.requeststate.RetryToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector

internal class SingleRequestStateFlow<U,T> internal constructor(
    override val upstream: Flow<U>,
    private val operation: suspend (U) -> T,
    override val config: RequestStateConfig,
    override val token: RetryToken = RetryToken()
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