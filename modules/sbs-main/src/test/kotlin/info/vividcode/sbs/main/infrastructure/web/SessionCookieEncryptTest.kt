package info.vividcode.sbs.main.infrastructure.web

import info.vividcode.sbs.main.auth.domain.SessionId
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SessionCookieEncryptTest {

    @Nested
    internal inner class CodecTest {
        private val sessionCookieEncrypt = SessionCookieEncrypt(
            "0123456789ABCDEF".toByteArray(), "01234567".toByteArray()
        ) { ByteArray(it) { 0x10 } }
        private val codec = sessionCookieEncrypt.createCodec()

        @Test
        internal fun encode() {
            // Act
            val cookieValue = codec.encode(SessionId(100))

            // Assert
            val expected = "10101010101010101010101010101010/" +
                    "299496e151356230e143ee80fd657456:" +
                    "de86f11f6ab268320ae9040740d0e7acce1b194a16605d3867c501d7ced9dba7"
            Assertions.assertEquals(expected, cookieValue)
        }

        @Test
        internal fun decode_normal() {
            val cookieValue = codec.encode(SessionId(100))

            // Act
            val decoded = codec.decode(cookieValue)

            // Assert
            Assertions.assertEquals(SessionId(100L), decoded)
        }

        @Test
        internal fun decode_notNumber() {
            val cookieValue = sessionCookieEncrypt.transformWrite("not-a-number")

            // Act
            val decoded = codec.decode(cookieValue)

            // Assert
            Assertions.assertNull(decoded)
        }

        @Test
        internal fun decode_invalidCookieValue() {
            val cookieValue = "invalid-cookie-value"

            // Act
            val decoded = codec.decode(cookieValue)

            // Assert
            Assertions.assertNull(decoded)
        }
    }

}
