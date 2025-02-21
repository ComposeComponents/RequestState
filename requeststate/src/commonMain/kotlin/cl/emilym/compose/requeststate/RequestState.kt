package cl.emilym.compose.requeststate

/**
 * Represents the state of a data transfer operation between the UI and ViewModel.
 */
sealed interface RequestState<T> {

    /**
     * The data transfer has not begun
     */
    class Initial<T>: RequestState<T>

    /**
     * The data transfer is in progress
     */
    class Loading<T>: RequestState<T>

    /**
     * The data transfer succeeded
     *
     * @param value The result of the data transfer, usually the data itself for read operations
     */
    data class Success<T>(
        val value: T
    ): RequestState<T>

    /**
     * The data transfer failed
     *
     * @param exception The cause of the failure
     */
    data class Failure<T>(
        val exception: Throwable
    ): RequestState<T>

}