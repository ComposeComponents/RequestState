package cl.emilym.compose.requeststate

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class HelpersTest {

    @Test
    fun map_should_transform_value_when_success() {
        val successState: RequestState<Int> = RequestState.Success(42)
        val mappedState = successState.map { it.toString() }

        assertEquals(RequestState.Success("42"), mappedState)
    }

    @Test
    fun map_should_return_same_state_for_non_success() {
        assertIs<RequestState.Initial<Any>>(RequestState.Initial<Any>().map { it })
        assertIs<RequestState.Loading<Any>>(RequestState.Loading<Any>().map { it })
        assertIs<RequestState.Failure<Int>>(RequestState.Failure<Int>(Exception("Error")))
    }

    @Test
    fun unwrap_should_return_value_when_success() {
        val successState: RequestState<Int> = RequestState.Success(42)
        assertEquals(42, successState.unwrap())
    }

    @Test
    fun unwrap_should_return_null_for_non_success() {
        assertNull(RequestState.Initial<Any>().unwrap())
        assertNull(RequestState.Loading<Any>().unwrap())
        assertNull(RequestState.Failure<Any>(Exception("Error")).unwrap())
    }

    @Test
    fun unwrap_with_default_should_return_value_when_success() {
        val successState: RequestState<Int> = RequestState.Success(42)
        assertEquals(42, successState.unwrap(99))
    }

    @Test
    fun unwrap_with_default_should_return_default_for_non_success() {
        assertEquals(99, RequestState.Initial<Int>().unwrap(99))
        assertEquals(99, RequestState.Loading<Int>().unwrap(99))
        assertEquals(99, RequestState.Failure<Int>(Exception("Error")).unwrap(99))
    }
    
}