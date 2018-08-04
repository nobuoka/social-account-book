package info.vividcode.sbs.main.infrastructure.web

import info.vividcode.sbs.main.auth.domain.SessionId
import io.ktor.sessions.SessionTransportTransformerEncrypt
import io.ktor.util.hex
import org.slf4j.LoggerFactory
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * See : [SessionTransportTransformerEncrypt]
 */
internal class SessionCookieEncrypt
/**
 * @param encryptionKey The key material of the secret key used for AES encryption. Its length must be 8.
 * @param signKey The key material of the secret key used for HMAC-SHA256 signature. Its length must be 16.
 */
constructor(
    encryptionKey: ByteArray,
    signKey: ByteArray,
    private val ivGenerator: (size: Int) -> ByteArray = { size -> SecureRandom().generateSeed(size) }
) {

    init {
        require(encryptionKey.size == 16) { "Length of `encryptionKey` must be 16." }
        require(signKey.size == 8) { "Length of `signKey` must be 8." }
    }

    private val log = LoggerFactory.getLogger(SessionCookieEncrypt::class.java)

    private val encryptionKeySpec = SecretKeySpec(encryptionKey, "AES")
    private val signKeySpec = SecretKeySpec(signKey, "HmacSHA256")
    private val encryptAlgorithm = encryptionKeySpec.algorithm
    private val signAlgorithm = signKeySpec.algorithm

    private val charset = Charsets.UTF_8
    private val encryptionKeySize get() = encryptionKeySpec.encoded.size

    // Check that input keys are right
    init {
        encrypt(ivGenerator(encryptionKeySize), byteArrayOf())
        mac(byteArrayOf())
    }

    internal fun transformRead(transportValue: String): String? {
        if (!transportValue.contains('/')) return null
        val encryptedMac = transportValue.substringAfterLast('/')
        val iv = hexOrNull(transportValue.substringBeforeLast('/')) ?: return null

        if (!encryptedMac.contains(':')) return null
        val encrypted = hexOrNull(encryptedMac.substringBeforeLast(':')) ?: return null
        val macHex = encryptedMac.substringAfterLast(':')
        val decrypted = decryptOrNull(iv, encrypted) ?: return null

        if (hex(mac(decrypted)) != macHex) {
            log.debug("Invalid signature")
            return null
        }

        return decrypted.toString(charset)
    }

    internal fun transformWrite(transportValue: String): String {
        val iv = ivGenerator(encryptionKeySize)
        val decrypted = transportValue.toByteArray(charset)
        val encrypted = encrypt(iv, decrypted)
        val mac = mac(decrypted)
        return "${hex(iv)}/${hex(encrypted)}:${hex(mac)}"
    }

    internal fun createCodec(): CookieCodec<SessionId> = Codec(this)

    private fun encrypt(initVector: ByteArray, decrypted: ByteArray): ByteArray =
        encryptDecrypt(Cipher.ENCRYPT_MODE, initVector, decrypted)

    private fun decryptOrNull(initVector: ByteArray, encrypted: ByteArray): ByteArray? =
        try {
            encryptDecrypt(Cipher.DECRYPT_MODE, initVector, encrypted)
        } catch (e: EncryptDecryptException) {
            log.debug("Decryption failed", e)
            null
        }

    private fun encryptDecrypt(mode: Int, initVector: ByteArray, input: ByteArray): ByteArray {
        val iv = IvParameterSpec(initVector)
        val cipher = Cipher.getInstance("$encryptAlgorithm/CBC/PKCS5PADDING")
        try {
            cipher.init(mode, encryptionKeySpec, iv)
        } catch (e: InvalidKeyException) {
            throw EncryptDecryptException(e)
        } catch (e: InvalidAlgorithmParameterException) {
            throw EncryptDecryptException(e)
        }
        return try {
            cipher.doFinal(input)
        } catch (e: IllegalBlockSizeException) {
            throw EncryptDecryptException(e)
        } catch (e: BadPaddingException) {
            throw EncryptDecryptException(e)
        }
    }

    private class EncryptDecryptException(e: Exception) : RuntimeException(e)

    private fun mac(value: ByteArray): ByteArray = Mac.getInstance(signAlgorithm).run {
        init(signKeySpec)
        doFinal(value)
    }

    private fun hexOrNull(s: String) = try {
        hex(s)
    } catch (e: NumberFormatException) {
        log.debug("Parsing hexadecimal string failed", e)
        null
    }

    private class Codec(private val encrypt: SessionCookieEncrypt) : CookieCodec<SessionId> {
        override fun encode(value: SessionId): String = encrypt.transformWrite("${value.value}")
        override fun decode(cookieValue: String): SessionId? =
            encrypt.transformRead(cookieValue)?.toLongOrNull()?.let(::SessionId)
    }

}
