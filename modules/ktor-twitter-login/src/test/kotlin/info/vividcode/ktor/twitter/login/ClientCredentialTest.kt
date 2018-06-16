package info.vividcode.ktor.twitter.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class ClientCredentialTest {

    private val token = ClientCredential("token", "sharedSecret")

    @Test
    fun testGetters() {
        assertEquals("token", token.identifier)
        assertEquals("sharedSecret", token.sharedSecret)
    }

    @Test
    fun testHashCode() {
        assertEquals(-1922446180, token.hashCode())
    }

    @Test
    fun testToString() {
        assertEquals(
            "ClientCredential(identifier=token, sharedSecret=sharedSecret)",
            token.toString()
        )
    }

    @Test
    fun testCopy() {
        assertEquals(token, token.copy())
        assertNotEquals(token, token.copy(identifier = "newToken"))
    }

}
