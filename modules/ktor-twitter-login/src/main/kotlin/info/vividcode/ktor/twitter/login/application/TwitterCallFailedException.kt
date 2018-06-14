package info.vividcode.ktor.twitter.login.application

class TwitterCallFailedException private constructor(
    message: String,
    initCause: Boolean,
    cause: Exception? = null
) : Exception(message) {
    constructor(message: String) : this(message, false, null)
    constructor(message: String, cause: Exception?) : this(message, true, cause)

    init {
        if (initCause) {
            initCause(cause)
        }
    }

}
