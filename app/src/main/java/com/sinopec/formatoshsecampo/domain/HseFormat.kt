package com.sinopec.formatoshsecampo.domain

/**
 * Catálogo de formatos disponibles en la app.
 * Para agregar otro formato en el futuro, se agrega aquí y se crea su Feature.
 */
enum class HseFormat(val code: String, val title: String, val version: String) {
    SUPERVISION_DIARIA("SUPERVISION_SEGURA", "Supervisión Segura", "1.0"),
    TARJETA_OBSERVACION("TARJETA_OBSERVACION_SEGURIDAD", "Tarjeta de Observación de Seguridad", "1.0"),
    SUPERVISION_WORD("LISTA_SUPERVISION_SEGURA", "Formato Supervisión Segura", "2.0"),
    INSPECCION_CHALECO("INSPECCION_CHALECO_SALVAVIDAS", "Inspección de Chaleco Salvavidas", "1.0")
}
