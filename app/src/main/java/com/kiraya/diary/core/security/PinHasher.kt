package com.kiraya.diary.core.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Stores the PIN only as a salted PBKDF2 hash. */
object PinHasher {
    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256

    fun newSalt(): String {
        val bytes = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(pin: String, salt: String): String {
        val spec = PBEKeySpec(pin.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITERATIONS, KEY_BITS)
        // PBKDF2WithHmacSHA1 is available on every Android version we support (API 24+).
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        spec.clearPassword()
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    fun matches(pin: String, salt: String, expectedHash: String): Boolean {
        if (salt.isEmpty() || expectedHash.isEmpty()) return false
        return MessageDigest.isEqual(hash(pin, salt).toByteArray(), expectedHash.toByteArray())
    }
}
