package info.vividcode.whatwg.url

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class WwwFormUrlEncodedTest {

    @Test
    fun parseWwwFormUrlEncoded() {
        val testInput = "ab%c=ddd+%3+0%39aaa&&no-equal-sign&efg=klm".toByteArray()
        val expected = listOf(
            "ab%c" to "ddd %3 09aaa",
            "no-equal-sign" to "",
            "efg" to "klm"
        )

        val actual = parseWwwFormUrlEncoded(testInput)
        assertEquals(expected, actual)
    }

    @Test
    fun parseWwwFormUrlEncoded_emptyByteSequence() {
        val testInput = byteArrayOf()
        val expected = emptyList<Pair<String, String>>()

        val actual = parseWwwFormUrlEncoded(testInput)
        assertEquals(expected, actual)
    }

    @Test
    fun percentDecode() {
        val testValue = "abc%39%6d%6E%l%".toByteArray()
        val expected = "abc9mn%l%".toByteArray()

        val actual = percentDecode(testValue)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun percentDecode_emptyByteSequence() {
        val testValue = byteArrayOf()
        val expected = byteArrayOf()

        val actual = percentDecode(testValue)
        assertArrayEquals(expected, actual)
    }

}
