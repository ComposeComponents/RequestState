package cl.emilym.compose.requeststate

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestStateFlowTest {

    @Test
    fun single_request_state_flow_emits_success() = runTest {
        val operation: suspend () -> String = { "Result" }
        val flow = requestStateFlow(operation = operation)

        val emissions = flow.toList()

        assertEquals(2, emissions.size)
        assertIs<RequestState.Loading<String>>(emissions[0])
        assertIs<RequestState.Success<String>>(emissions[1])
        assertEquals("Result", (emissions[1] as RequestState.Success).value)
    }

    @Test
    fun single_request_state_flow_emits_failure_on_exception() = runTest {
        val exception = RuntimeException("Error")
        val operation: suspend () -> String = { throw exception }
        val flow = requestStateFlow(operation = operation)

        val emissions = flow.toList()

        assertEquals(2, emissions.size)
        assertIs<RequestState.Loading<String>>(emissions[0])
        assertIs<RequestState.Failure<String>>(emissions[1])
        assertEquals(exception, (emissions[1] as RequestState.Failure).exception)
    }

    @Test
    fun flat_request_state_flow_emits_multiple_values() = runTest {
        val operation: suspend () -> Flow<Int> = { flowOf(1, 2, 3) }
        val flow = flatRequestStateFlow(operation = operation)

        val emissions = flow.toList()

        assertEquals(4, emissions.size)
        assertIs<RequestState.Loading<Int>>(emissions[0])
        assertEquals(listOf(1, 2, 3), emissions.drop(1).map { (it as RequestState.Success).value })
    }

    @Test
    fun flow_upstream_propagates_values_to_request_state_flow() = runTest {
        val upstream = flowOf(1, 2, 3)
        val operation: suspend (Int) -> String = { "Processed: $it" }
        val flow = upstream.requestStateFlow(operation = operation)

        val emissions = flow.toList()

        assertEquals(6, emissions.size) // 3 Loading + 3 Success
        assertEquals("Processed: 1", (emissions[1] as RequestState.Success).value)
        assertEquals("Processed: 2", (emissions[3] as RequestState.Success).value)
        assertEquals("Processed: 3", (emissions[5] as RequestState.Success).value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun retry_triggers_operation_again() = runTest {
        val mockOperation: suspend () -> String = suspend { "Result" }
        val flow = requestStateFlow(operation = mockOperation)

        val emissions = mutableListOf<RequestState<String>>()
        val job = flow
            .onEach { emissions.add(it) }
            .launchIn(this)

        advanceUntilIdle()
        flow.retry()
        advanceUntilIdle()
        job.cancel()

        assertEquals(4, emissions.size)
        assertIs<RequestState.Loading<String>>(emissions[0])
        assertIs<RequestState.Success<String>>(emissions[1])
        assertIs<RequestState.Loading<String>>(emissions[2])
        assertIs<RequestState.Success<String>>(emissions[3])
    }
}
