package info.vividcode.sbs.main

/**
 * Exception class that indicates a bug of the application.
 */
sealed class ApplicationInternalException(message: String) : RuntimeException(message) {

    /**
     * Exception class that indicates there is data inconsistency.
     */
    class DataInconsistency(dataInconsistencyDetailMessage: String) :
        ApplicationInternalException("Data inconsistency ($dataInconsistencyDetailMessage)")

}
