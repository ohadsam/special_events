package com.ohadsam.specialevents.domain.backup

import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

sealed interface DecryptResult {
    data class Ok(val text: String) : DecryptResult

    /** The password is wrong, or the file was altered (AES-GCM cannot tell these apart, by design). */
    data object WrongPassword : DecryptResult

    data object Corrupt : DecryptResult
}

/**
 * Password-protected backups: AES-256-GCM with a key from PBKDF2-HMAC-SHA256 (random salt and IV per file). The file says which
 * parameters it used, so the iteration count can be raised later and old files still open. Pure JVM, so the tests run without a device.
 */
object BackupCrypto {
    const val FORMAT = "special-events-encrypted"
    const val MIN_PASSWORD = 6
    private const val VERSION = 1
    private const val ITERATIONS = 210_000
    private const val MAX_ITERATIONS = 2_000_000
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun isEncrypted(text: String): Boolean = runCatching { Json.parseToJsonElement(text).jsonObject.prim("format") == FORMAT }.getOrDefault(false)

    fun encrypt(plain: String, password: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key(password, salt, ITERATIONS), GCMParameterSpec(TAG_BITS, iv)) }
        val b64 = Base64.getEncoder()
        return buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iterations", ITERATIONS)
            put("salt", b64.encodeToString(salt))
            put("iv", b64.encodeToString(iv))
            put("data", b64.encodeToString(cipher.doFinal(plain.toByteArray(Charsets.UTF_8))))
        }.toString()
    }

    fun decrypt(envelope: String, password: CharArray): DecryptResult {
        val o = runCatching { Json.parseToJsonElement(envelope).jsonObject }.getOrNull() ?: return DecryptResult.Corrupt
        if (o.prim("format") != FORMAT) return DecryptResult.Corrupt
        val iterations = (o["iterations"] as? JsonPrimitive)?.intOrNull?.takeIf { it in 1..MAX_ITERATIONS } ?: return DecryptResult.Corrupt
        val b64 = Base64.getDecoder()
        return try {
            val salt = b64.decode(o.prim("salt") ?: return DecryptResult.Corrupt)
            val iv = b64.decode(o.prim("iv") ?: return DecryptResult.Corrupt)
            val data = b64.decode(o.prim("data") ?: return DecryptResult.Corrupt)
            if (iv.size != IV_BYTES) return DecryptResult.Corrupt
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(password, salt, iterations), GCMParameterSpec(TAG_BITS, iv)) }
            DecryptResult.Ok(String(cipher.doFinal(data), Charsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            DecryptResult.Corrupt // bad base64
        } catch (e: GeneralSecurityException) {
            DecryptResult.WrongPassword
        }
    }

    private fun key(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun JsonObject.prim(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
