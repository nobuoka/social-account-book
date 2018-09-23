package info.vividcode.orm

/**
 * This exception represents unexpected usage or unexpected state in Kdbi.
 */
class KdbiRuntimeException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
