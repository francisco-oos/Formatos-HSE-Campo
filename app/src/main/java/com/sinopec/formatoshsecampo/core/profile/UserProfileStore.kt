package com.sinopec.formatoshsecampo.core.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.sinopec.formatoshsecampo.core.debug.DebugConfig
import org.json.JSONArray
import org.json.JSONObject

data class UserProfile(
    val nombre: String = "",
    val idEmpleado: String = "",
    val puesto: String = "",
    val departamentoJefe: String = "",
    val areaDepartamento: String = "",
    val volante: String = "",
    val brigada: String = "",
    val proyecto: String = ""
) {
    fun title(): String = listOf(nombre, idEmpleado).filter { it.isNotBlank() }.joinToString(" / ").ifBlank { "Perfil sin nombre" }
    fun subtitle(): String = listOf(puesto, areaDepartamento, volante).filter { it.isNotBlank() }.joinToString(" · ")
}

/**
 * Guarda máximo los últimos 4 perfiles usados en este teléfono.
 * Es intencionalmente local y temporal para reducir captura repetida en campo.
 */
object UserProfileStore {
    private const val PREFS = "hse_temp_profiles"
    private const val KEY = "profiles_json"
    private const val MAX = 4

    fun latest(context: Context): UserProfile? = load(context).firstOrNull()

    fun load(context: Context): MutableList<UserProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val arr = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        val out = mutableListOf<UserProfile>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                UserProfile(
                    nombre = o.optString("nombre"),
                    idEmpleado = o.optString("id_empleado"),
                    puesto = o.optString("puesto"),
                    departamentoJefe = o.optString("departamento_jefe"),
                    areaDepartamento = o.optString("area_departamento"),
                    volante = o.optString("volante"),
                    brigada = o.optString("brigada"),
                    proyecto = o.optString("proyecto")
                )
            )
        }
        return out
    }

    fun save(context: Context, profile: UserProfile) {
        if (profile.nombre.isBlank() && profile.idEmpleado.isBlank()) return
        val current = load(context)
            .filterNot { samePerson(it, profile) }
            .toMutableList()
        current.add(0, profile)
        val arr = JSONArray()
        current.take(MAX).forEach { p ->
            arr.put(JSONObject()
                .put("nombre", p.nombre)
                .put("id_empleado", p.idEmpleado)
                .put("puesto", p.puesto)
                .put("departamento_jefe", p.departamentoJefe)
                .put("area_departamento", p.areaDepartamento)
                .put("volante", p.volante)
                .put("brigada", p.brigada)
                .put("proyecto", p.proyecto))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }


fun seedDebugProfiles(context: Context) {
    if (!DebugConfig.SUPERVISION_DIARIA &&
        !DebugConfig.TARJETA_OBSERVACION &&
        !DebugConfig.LISTA_CHEQUEO_SUPERVISION &&
        !DebugConfig.INSPECCION_CHALECO
    ) return

    save(context, UserProfile(
        nombre = "Francisco Alvarado",
        idEmpleado = "12345",
        puesto = "Sobrestante",
        departamentoJefe = "Adquisición (Registro)",
        areaDepartamento = "Adquisición (Registro)",
        volante = "V4",
        brigada = "371",
        proyecto = "ALACTE"
    ))

    save(context, UserProfile(
        nombre = "Bartolo García Rueda de León",
        idEmpleado = "67890",
        puesto = "Observador",
        departamentoJefe = "Topografía",
        areaDepartamento = "Topografía",
        volante = "V8",
        brigada = "371",
        proyecto = "ALACTE"
    ))
}

    private fun samePerson(a: UserProfile, b: UserProfile): Boolean {
        val aid = a.idEmpleado.trim()
        val bid = b.idEmpleado.trim()
        if (aid.isNotBlank() && bid.isNotBlank()) return aid.equals(bid, ignoreCase = true)
        return a.nombre.trim().equals(b.nombre.trim(), ignoreCase = true)
    }

    fun showChooser(activity: Activity, onSelected: (UserProfile) -> Unit, onNew: () -> Unit) {
        seedDebugProfiles(activity)
        val profiles = load(activity)
        if (profiles.isEmpty()) {
            Toast.makeText(activity, "Aún no hay perfiles guardados", Toast.LENGTH_SHORT).show()
            onNew()
            return
        }
        val labels = profiles.map { p ->
            val sub = p.subtitle()
            if (sub.isBlank()) p.title() else "${p.title()}\n$sub"
        }.plus("Capturar nuevo perfil")
        AlertDialog.Builder(activity)
            .setTitle("¿Quién está usando la app?")
            .setItems(labels.toTypedArray()) { _, which ->
                if (which < profiles.size) onSelected(profiles[which]) else onNew()
            }
            .show()
    }
}
