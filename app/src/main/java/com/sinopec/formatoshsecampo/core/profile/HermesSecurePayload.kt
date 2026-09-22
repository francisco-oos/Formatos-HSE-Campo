package com.sinopec.formatoshsecampo.core.profile

import android.util.Base64
import com.sinopec.formatoshsecampo.BuildConfig
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Puente interno entre apps propias. No modifica el archivo adjunto.
 * Solo cifra el contexto operativo que Hermes puede leer al recibir ACTION_SEND.
 *
 * La clave real no vive en Git: Gradle la toma de HERMES_BRIDGE_KEY en
 * hse-secrets.properties (ignorado) o de una variable de entorno.
 */
object HermesSecurePayload {
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private fun keyBytes(): ByteArray {
        val bytes = BuildConfig.HERMES_BRIDGE_KEY.toByteArray(Charsets.UTF_8)
        require(bytes.size == 16 || bytes.size == 24 || bytes.size == 32) {
            "HERMES_BRIDGE_KEY debe tener 16, 24 o 32 bytes UTF-8"
        }
        return bytes
    }

    fun encrypt(plainText: String): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyBytes(), "AES"),
            GCMParameterSpec(128, iv)
        )
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }
}
