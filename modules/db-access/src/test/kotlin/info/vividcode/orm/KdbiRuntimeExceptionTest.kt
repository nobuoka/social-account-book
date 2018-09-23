package info.vividcode.orm

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.RuntimeException

internal class KdbiRuntimeExceptionTest {

    companion object {
        private const val TEST_MESSAGE = "Test exception"
    }

    @Test
    internal fun withoutCause() {
        val exception = java.lang.RuntimeException(TEST_MESSAGE)

        Assertions.assertEquals(TEST_MESSAGE, exception.message)
        Assertions.assertNull(exception.cause)
    }

    @Test
    internal fun withCause_null() {
        val exception = KdbiRuntimeException(TEST_MESSAGE, null)

        Assertions.assertEquals(TEST_MESSAGE, exception.message)
        Assertions.assertNull(exception.cause)
    }

    @Test
    internal fun withCause_runtimeException() {
        val cause = RuntimeException("Test cause")
        val exception = KdbiRuntimeException(TEST_MESSAGE, cause)

        Assertions.assertEquals(TEST_MESSAGE, exception.message)
        Assertions.assertSame(cause, exception.cause)
    }

}
