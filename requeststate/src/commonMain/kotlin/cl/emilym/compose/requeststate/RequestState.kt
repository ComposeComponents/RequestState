package cl.emilym.compose.requeststate

sealed interface RequestState<T> {

    class Initial<T>: RequestState<T>
    class Loading<T>: RequestState<T>
    data class Success<T>(
        val value: T
    ): RequestState<T>
    data class Failure<T>(
        val exception: Throwable
    ): RequestState<T>

}