package com.sinopec.formatoshsecampo.core.draft

import org.json.JSONObject

/**
 * Borradores temporales por formato.
 *
 * Viven solo mientras el proceso de la app está abierto. Si el usuario cierra la app
 * completamente, Android elimina esta memoria y el siguiente arranque queda limpio.
 */
object FormDraftStore {
    private val drafts = mutableMapOf<String, JSONObject>()

    fun save(key: String, data: JSONObject) {
        drafts[key] = data
    }

    fun get(key: String): JSONObject? = drafts[key]

    fun clear(key: String) {
        drafts.remove(key)
    }

    fun clearAll() {
        drafts.clear()
    }
}
