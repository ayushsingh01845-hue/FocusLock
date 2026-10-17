package com.focuslock.app.util

import java.security.MessageDigest
import java.security.SecureRandom

/** The admin PIN is never stored as typed - only a salted SHA-256 hash of it. */
object AdminPin {
    fun newSalt(): String {
        val bytes = ByteArray(12)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest((salt + ":" + pin).toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun verify(pin: String, salt: String, expected: String): Boolean =
        expected.isNotEmpty() && hash(pin, salt) == expected
}
