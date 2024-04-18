package cl.emilym.compose.requeststate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.emilym.compose.errorwidget.ErrorWidget
import cl.emilym.compose.units.rdp

@Composable
fun <T> RequestStateWidget(
    state: RequestState<T>,
    retry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    RequestStateWidget(
        state,
        initial = { CircularProgressIndicator() },
        loading = { CircularProgressIndicator() },
        failure = { exception ->
            Column(Modifier.padding(1.rdp)) {
                ErrorWidget(
                    exception,
                    null,
                    retry
                )
            }
        },
        success = content
    )
}

@Composable
fun <T> RequestStateWidget(
    state: RequestState<T>,
    initial: @Composable () -> Unit,
    loading: @Composable () -> Unit,
    failure: @Composable (java.lang.Exception) -> Unit,
    success: @Composable (T) -> Unit
) {
    when (state) {
        is RequestState.Initial -> initial()
        is RequestState.Loading -> loading()
        is RequestState.Success -> success(state.value)
        is RequestState.Failure -> failure(state.exception)
    }

}