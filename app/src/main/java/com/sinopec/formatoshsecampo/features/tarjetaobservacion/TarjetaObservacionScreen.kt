package com.sinopec.formatoshsecampo.features.tarjetaobservacion

import android.app.Activity
import android.widget.*
import com.sinopec.formatoshsecampo.MainActivity
import com.sinopec.formatoshsecampo.core.pdf.SimplePdfService
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import com.sinopec.formatoshsecampo.features.supervisiondiaria.*
import com.sinopec.formatoshsecampo.ui.Ui
import org.json.JSONArray
import org.json.JSONObject
import android.widget.ScrollView

/** Captura del formato de dos caras: Tarjeta de Observación de Seguridad. */
class TarjetaObservacionScreen(private val activity: Activity) {
    private val area = Ui.input(activity, "Área / Departamento")
    private val actividad = Ui.input(activity, "Actividad realizada")
    private val tipo = Ui.spinner(activity, listOf("Acto Inseguro", "Condición Insegura", "Casi Accidente", "Observación de Seguridad Realizada", "Acción Positiva", "Oportunidad de Mejora", "Intervención de Seguridad"))
    private val razon = Ui.input(activity, "Razón de la observación", 3)
    private val detalles = Ui.input(activity, "Detalles de la observación", 3)
    private val acciones = Ui.input(activity, "Acciones tomadas / recomendaciones", 3)
    private val puntoAccion = Ui.spinner(activity, listOf("Sí", "No"))
    private val asignada = Ui.input(activity, "Acción asignada a")
    private val nombre = Ui.input(activity, "Nombre de quien reporta")
    private val departamento = Ui.input(activity, "Departamento")
    private val puesto = Ui.input(activity, "Puesto de trabajo")
    private val photos = PhotoAttachmentPanel(activity)
    private val checks = mutableListOf<Pair<String, RadioGroup>>()
    //CODIGO DEBUG LLENADO DE DATOS
    private val DEBUG_TOS = false

    private val checklist = listOf(
        "Conducta Personal", "Uso del EPP", "Ojos en la Tarea", "Posición de trabajo segura", "Alzando/jalando/empujando/cargando",
        "Ambiente de Trabajo", "Orden y Limpieza", "Iluminación", "Ventilación", "Superficie Nivelada",
        "Equipo/Herramientas", "Adecuadas para la tarea", "Bloqueos/Aislamientos", "Arnés/Línea de vida",
        "Procedimientos", "Análisis de Seguridad del Trabajo", "Permiso de Trabajo", "Instructivo de Trabajo", "Respuesta a Emergencias"
    )

    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        (activity as? MainActivity)?.bindPhotoPanel(photos)
        addView(Ui.title(activity, "Tarjeta de Observación de Seguridad"))
        addView(Ui.button(activity, "← Menú", { activity.setContentView(HomeScreen(activity).build()) }))
        addView(Ui.label(activity, "Tipo de observación")); addView(tipo)
        addView(area); addView(actividad)
        addView(Ui.section(activity, "Acciones de seguridad y observaciones"))
        checklist.forEach { q ->
            addView(Ui.label(activity, q))
            val group = Ui.radioRow(activity, listOf("Seguro", "Inseguro", "No Aplica"), 2)
            checks.add(q to group); addView(group)
        }
        addView(Ui.section(activity, "Reverso"))
        addView(razon); addView(detalles); addView(acciones)
        addView(Ui.label(activity, "Punto de acción generado")); addView(puntoAccion)
        addView(asignada); addView(nombre); addView(departamento); addView(puesto)
        addView(Ui.section(activity, "Fotos anexas")); addView(photos.view)
        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
        //CODIGO DEBUG LLENADO DE DATOS
        if (DEBUG_TOS) {
            cargarDatosPrueba()
        }
    })

    private fun cargarDatosPrueba() {
        area.setText("Adquisición de Datos")
        actividad.setText("Inspección en comedor")

        tipo.setSelection(0) // 0 Acto Inseguro, 5 Oportunidad de Mejora

        razon.setText("Se observa presencia de moscas en el área del comedor.")
        detalles.setText("La condición puede generar molestias al personal y posible riesgo sanitario.")
        acciones.setText("Solicitar limpieza profunda y fumigación del área.")
        puntoAccion.setSelection(0) // Sí
        asignada.setText("Administración")

        nombre.setText("Bartolo García Rueda de León")
        departamento.setText("Adquisición de Datos")
        puesto.setText("Sobrestante")

        checks.forEachIndexed { index, (_, group) ->
            when (index % 3) {
                0 -> group.check(group.getChildAt(0).id) // Seguro
                1 -> group.check(group.getChildAt(1).id) // Inseguro
                2 -> group.check(group.getChildAt(2).id) // No Aplica
            }
        }
    }
    private fun generate() {

        val report = object : HseReport {
            override val format = HseFormat.TARJETA_OBSERVACION
            override val folio = "TOS-${dateCompact()}-${System.currentTimeMillis().toString().takeLast(6)}"
            override fun toJson() = baseJson(format, folio).apply {
                put("datos_generales", JSONObject().put("area_departamento", area.text.toString()).put("actividad_realizada", actividad.text.toString()).put("tipo_observacion", tipo.selectedItem.toString()))
                put("observaciones", JSONObject().put("razon", razon.text.toString()).put("detalles", detalles.text.toString()))
                put("acciones", JSONObject().put("acciones_tomadas_recomendaciones", acciones.text.toString()).put("punto_accion_generado", puntoAccion.selectedItem.toString()).put("accion_asignada_a", asignada.text.toString()))
                put("reporta", JSONObject().put("nombre", nombre.text.toString()).put("departamento", departamento.text.toString()).put("puesto_trabajo", puesto.text.toString()))
                put("checklist", JSONArray().apply { checks.forEach { (q, g) -> put(JSONObject().put("punto", q).put("respuesta", selected(g))) } })
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = listOf(
            "Tipo: ${tipo.selectedItem}", "Área/Departamento: ${area.text}", "Actividad: ${actividad.text}",
            "Razón: ${razon.text}", "Detalles: ${detalles.text}", "Acciones/Recomendaciones: ${acciones.text}",
            "Punto de acción: ${puntoAccion.selectedItem}", "Asignada a: ${asignada.text}", "Reporta: ${nombre.text} / ${departamento.text} / ${puesto.text}"
        )
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }
}
