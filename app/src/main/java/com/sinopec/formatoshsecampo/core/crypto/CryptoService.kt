package com.sinopec.formatoshsecampo.core.crypto

import android.util.Base64
import com.sinopec.formatoshsecampo.BuildConfig
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Servicio de cifrado compatible con el flujo aprendido en Supervisión Segura.
 * Cifra el JSON completo con AES-256-GCM antes de incrustarlo al PDF.
 */
object CryptoService {
    fun encryptJson(plainJson: JSONObject): JSONObject {
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(BuildConfig.DATA_KEY.toByteArray(Charsets.UTF_8))
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toString().toByteArray(Charsets.UTF_8))

        return JSONObject().apply {
            // Campo de compatibilidad con el lector Python de la app Supervisión Segura original.
            put("tipo", "SUPERVISION_SEGURA_ENCRYPTED")
            put("crypto", "AES-256-GCM")
            put("key_id", "SS-V1-DEMO")
            put("iv_b64", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("data_b64", Base64.encodeToString(encrypted, Base64.NO_WRAP))
        }
    }
}
