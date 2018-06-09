package info.vividcode.whatwg.url

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Parse an application/x-www-form-urlencoded byte sequence.
 *
 * @param input Input byte sequence.
 * @return List of name-value pairs where both name and value hold a string.
 *
 * @see <a href="https://url.spec.whatwg.org/#urlencoded-parsing">application/x-www-form-urlencoded parsing (URL Living Standard)</a>
 */
fun parseWwwFormUrlEncoded(input: ByteArray): List<Pair<String, String>> {
    // Let sequences be the result of splitting input on 0x26 (&).
    var remained: ByteArray? = input
    val sequences = mutableListOf<ByteArray>()
    while (remained?.isNotEmpty() == true) {
        val index = remained.indexOf(0x26)
        val bytes = remained.sliceArray(0..((if (index < 0) remained.size else index) - 1))
        sequences.add(bytes)
        remained = if (index >= 0) remained.sliceArray((index + 1)..(remained.size - 1)) else null
    }

    // Let output be an initially empty list of name-value tuples where both name and value hold a string.
    val output = mutableListOf<Pair<String, String>>()

    // For each byte sequence bytes in sequences:
    for (bytes in sequences) {
        // If bytes is the empty byte sequence, then continue.
        if (bytes.isEmpty()) continue
        // If bytes contains a 0x3D (=), then let name be the bytes from the start of bytes up to but excluding its first 0x3D (=),
        // and let value be the bytes, if any, after the first 0x3D (=) up to the end of bytes.
        // If 0x3D (=) is the first byte, then name will be the empty byte sequence. If it is the last,
        // then value will be the empty byte sequence.
        // Otherwise, let name have the value of bytes and let value be the empty byte sequence.
        val index = bytes.indexOf(0x3D)
        val (nameBytes, valueBytes) = if (index >= 0) {
            Pair(bytes.sliceArray(0..(index - 1)), bytes.sliceArray((index + 1)..(bytes.size - 1)))
        } else {
            Pair(bytes, ByteArray(0))
        }
        // Replace any 0x2B (+) in name and value with 0x20 (SP).
        // Let nameString and valueString be the result of running UTF-8 decode without BOM on the percent decoding of name and value, respectively.
        val decode: ByteArray.() -> String = {
            also { for (i in 0..(it.size - 1)) if (it[i] == 0x2B.toByte()) it[i] = 0x20 }
                // TODO : UTF-8 decoding along with spec
                .let { percentDecode(it).toString(StandardCharsets.UTF_8) }
        }
        // Append (nameString, valueString) to output.
        output.add(Pair(nameBytes.decode(), valueBytes.decode()))
    }

    return output
}

/**
 * Percent decode a byte sequence.
 *
 * @param input Input byte sequence.
 * @return Percent-decoded byte sequence.
 *
 * @see <a href="https://url.spec.whatwg.org/#percent-decode">Percent decode (URL Living Standard)</a>
 */
fun percentDecode(input: ByteArray): ByteArray {
    // 1. Let `output` be an empty byte sequence.
    val output = ByteArrayOutputStream()
    // 2. For each byte `byte` in `input`:
    var index = 0
    while (index < input.size) {
        val byte = input[index]
        // 2-1. If `byte` is not 0x25 (%), then append `byte` to `output`.
        if (byte != BYTE_ASCII_PERCENT) {
            output.write(byte.toInt())
        } else {
            // 2-2. Otherwise, if byte is 0x25 (%) and the next two bytes after byte in input are not in
            //     the ranges 0x30 (0) to 0x39 (9), 0x41 (A) to 0x46 (F), and 0x61 (a) to 0x66 (f), all inclusive,
            //     append byte to output.
            val nextByte1Value = input.getOrNull(index + 1)?.asHexadecimalNumberOrNull()
            val nextByte2Value = input.getOrNull(index + 2)?.asHexadecimalNumberOrNull()

            if (nextByte1Value == null || nextByte2Value == null) {
                output.write(byte.toInt())
            } else {
                // 2-3. Otherwise:
                //   1. Let `bytePoint` be the two bytes after `byte` in `input`, decoded, and then interpreted
                //     as hexadecimal number.
                //   2. Append a byte whose value is `bytePoint` to `output`.
                //   3. Skip the next two bytes in `input`.
                val v = (nextByte1Value shl 4) + nextByte2Value
                output.write(v)
                index += 2
            }
        }
        index += 1
    }

    return output.toByteArray()
}

private const val BYTE_ASCII_PERCENT: Byte = 0x25

private fun Byte.asHexadecimalNumberOrNull(): Int? = when (this) {
    in 0x30..0x39 -> this - 0x30
    in 0x41..0x46 -> this - 0x41 + 0x0A
    in 0x61..0x66 -> this - 0x61 + 0x0A
    else -> null
}
