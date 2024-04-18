package cl.emilym.compose.requeststate

sealed interface RequestState<T> {

    class Initial<T>: RequestState<T>
    class Loading<T>: RequestState<T>
    class Success<T>(
        val value: T
    ): RequestState<T>
    class Failure<T>(
        val exception: Exception
    ): RequestState<T>

}