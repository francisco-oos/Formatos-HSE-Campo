package com.sinopec.formatoshsecampo.core.pdf

import android.util.Base64
import org.json.JSONObject
import java.io.File

/**
 * Incrusta el payload cifrado al final del PDF con un marcador estable.
 *
 * IMPORTANTE:
 * - Se conserva el marcador histórico de Supervisión Segura para que el lector Python
 *   que ya funcionaba pueda seguir encontrando el JSON cifrado.
 * - Más adelante podemos agregar marcadores por formato, pero para cerrar este primer
 *   formato dejamos compatibilidad directa.
 */
object PdfPayloadEmbedder {
    const val MARKER = "%%SUPERVISION_SEGURA_JSON_ENCRYPTED_BASE64:"

    fun appendEncryptedPayload(pdf: File, encryptedPayload: JSONObject) {
        val b64 = Base64.encodeToString(
            encryptedPayload.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )

        // El lector de PC busca esta línea al final del PDF.
        pdf.appendText("\n$MARKER$b64\n")
    }
}
