package com.sinopec.formatoshsecampo.core.pdf

import java.io.File
import java.security.MessageDigest

/**
 * Calcula hash SHA-256 del PDF final para control de integridad.
 */
object PdfIntegrity {
    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
