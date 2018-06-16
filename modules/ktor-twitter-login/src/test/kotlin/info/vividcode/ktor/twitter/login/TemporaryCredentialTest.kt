package info.vividcode.ktor.twitter.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class TemporaryCredentialTest {

    private val token = TemporaryCredential("token", "sharedSecret")

    @Test
    fun testGetters() {
        assertEquals("token", token.token)
        assertEquals("sharedSecret", token.secret)
    }

    @Test
    fun testHashCode() {
        assertEquals(-1922446180, token.hashCode())
    }

    @Test
    fun testToString() {
        assertEquals(
            "TemporaryCredential(token=token, secret=sharedSecret)",
            token.toString()
        )
    }

    @Test
    fun testCopy() {
        assertEquals(token, token.copy())
        assertNotEquals(token, token.copy(token = "newToken"))
    }

}
