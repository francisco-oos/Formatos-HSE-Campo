package com.sinopec.formatoshsecampo.features.tarjetaobservacion

import com.sinopec.formatoshsecampo.BuildConfig
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Modelo inicial del formato de dos caras: Tarjeta de Observación de Seguridad. */
data class TarjetaObservacionReport(
    override val folio: String,
    val fecha: String,
    val hora: String,
    val tipoObservacion: String,
    val areaDepartamento: String,
    val actividadRealizada: String,
    val razon: String,
    val detalles: String,
    val acciones: String,
    val puntoAccionGenerado: Boolean,
    val accionAsignadaA: String,
    val nombreReporta: String,
    val departamentoReporta: String,
    val puestoTrabajo: String,
    val checklist: List<ChecklistRespuesta>,
    val fotosAnexas: Int
) : HseReport {
    override val format = HseFormat.TARJETA_OBSERVACION

    override fun toJson(): JSONObject = JSONObject().apply {
        put("tipo_formato", format.code)
        put("version_formato", format.version)
        put("app_version", BuildConfig.VERSION_NAME)
        put("fecha", fecha)
        put("hora", hora)
        put("folio", folio)
        put("datos_generales", JSONObject().apply {
            put("area_departamento", areaDepartamento)
            put("actividad_realizada", actividadRealizada)
            put("tipo_observacion", tipoObservacion)
        })
        put("observaciones", JSONObject().apply {
            put("razon", razon)
            put("detalles", detalles)
        })
        put("checklist", JSONArray(checklist.map { it.toJson() }))
        put("respuestas", JSONArray(checklist.map { it.toJson() }))
        put("comentarios", detalles)
        put("acciones", JSONObject().apply {
            put("acciones_tomadas_recomendaciones", acciones)
            put("punto_accion_generado", puntoAccionGenerado)
            put("accion_asignada_a", accionAsignadaA)
        })
        put("datos_reportante", JSONObject().apply {
            put("nombre", nombreReporta)
            put("departamento", departamentoReporta)
            put("puesto_trabajo", puestoTrabajo)
        })
        put("fotos_anexas", fotosAnexas)
        put("created_at_device", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
    }
}

data class ChecklistRespuesta(val grupo: String, val punto: String, val respuesta: String) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("grupo", grupo)
        put("punto", punto)
        put("respuesta", respuesta) // Seguro, Inseguro, No Aplica
    }
}
