package com.sinopec.formatoshsecampo.core.photo

/**
 * Configuración manual de compresión de fotos.
 * Aquí es donde vas a jugar para encontrar el punto óptimo calidad/peso.
 */
object PhotoQuality {
    // AJUSTE MANUAL PRINCIPAL: 100 = máxima calidad/peso alto, 70 = buen equilibrio, 45 = peso bajo.
    const val JPEG_QUALITY: Int = 72

    // AJUSTE MANUAL SECUNDARIO: lado máximo en pixeles antes de guardar/anexar al PDF.
    const val MAX_IMAGE_SIDE_PX: Int = 1600

    // Límite solicitado para piloto: máximo 5 evidencias por reporte.
    const val MAX_PHOTOS_PER_REPORT: Int = 5
}
