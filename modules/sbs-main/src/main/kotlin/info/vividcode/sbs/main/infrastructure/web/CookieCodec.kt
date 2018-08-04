package info.vividcode.sbs.main.infrastructure.web

internal interface CookieCodec<T> {

    /**
     * Encode object to cookie value.
     */
    fun encode(value: T): String

    /**
     * Decode cookie value to object.
     *
     * If cookie value is invalid, `null` will be returned. (Error is not thrown.)
     */
    fun decode(cookieValue: String): T?

}