package info.vividcode.ktor.twitter.login

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

internal class TwitterTokenTest {

    private val token = TwitterToken("token", "sharedSecret", "userId", "screenName")

    @Test
    fun testGetters() {
        assertEquals("token", token.token)
        assertEquals("sharedSecret", token.sharedSecret)
        assertEquals("userId", token.userId)
        assertEquals("screenName", token.screenName)
    }

    @Test
    fun testHashCode() {
        assertEquals(-1199552211, token.hashCode())
    }

    @Test
    fun testToString() {
        assertEquals(
            "TwitterToken(token=token, sharedSecret=sharedSecret, userId=userId, screenName=screenName)",
            token.toString()
        )
    }

    @Test
    fun testCopy() {
        assertEquals(token, token.copy())
        assertNotEquals(token, token.copy(token = "newToken"))
    }

}
