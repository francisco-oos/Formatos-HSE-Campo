package com.sinopec.formatoshsecampo.core.profile

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

/**
 * Puente interno entre apps propias. No modifica el archivo adjunto.
 * Solo cifra el contexto operativo que Hermes puede leer al recibir ACTION_SEND.
 */
object HermesSecurePayload {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY = "HermesOpsBridge1" // 16 bytes exactos. Misma clave puente en apps propias.

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY.toByteArray(Charsets.UTF_8), "AES"), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }
}
