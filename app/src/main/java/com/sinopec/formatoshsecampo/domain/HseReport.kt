package com.sinopec.formatoshsecampo.domain

import org.json.JSONObject

/**
 * Contrato mínimo que debe cumplir cualquier formato HSE.
 * El PDF es evidencia visual; este JSON es la base estructurada para PC/SQLite.
 */
interface HseReport {
    val format: HseFormat
    val folio: String
    fun toJson(): JSONObject
}
