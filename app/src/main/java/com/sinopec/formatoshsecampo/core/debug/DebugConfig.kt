package com.sinopec.formatoshsecampo.core.debug

/**
 * Interruptores de prueba para los formularios.
 * Cambia a true solo mientras estés ajustando pantallas/PDF.
 * No modifica JSON cifrado, clave, marcador ni AES-GCM.
 */
object DebugConfig {
    const val SUPERVISION_DIARIA = false
    const val TARJETA_OBSERVACION = false
    const val LISTA_CHEQUEO_SUPERVISION = false
    const val INSPECCION_CHALECO = false
}
