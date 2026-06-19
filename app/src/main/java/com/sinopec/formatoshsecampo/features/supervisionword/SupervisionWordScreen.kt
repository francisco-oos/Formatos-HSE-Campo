package com.sinopec.formatoshsecampo.features.supervisionword

import android.app.Activity
import android.widget.*
import com.sinopec.formatoshsecampo.MainActivity
import com.sinopec.formatoshsecampo.core.pdf.SimplePdfService
import com.sinopec.formatoshsecampo.core.config.FormOptions
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import com.sinopec.formatoshsecampo.features.supervisiondiaria.*
import com.sinopec.formatoshsecampo.ui.Ui
import org.json.JSONArray
import org.json.JSONObject
import android.widget.ScrollView

/** Formato basado en el Word: Lista de Chequeo Supervisión Segura SSM-HSE-F-20. */
class SupervisionWordScreen(private val activity: Activity) {
    /*
     * MODO PRUEBAS DEL FORMATO LISTA DE CHEQUEO.
     *
     * true  = al entrar al formato se cargan datos de prueba automáticamente.
     * false = modo normal para capturar datos reales en campo.
     *
     * Úsalo solo mientras ajustamos el PDF visual.
     */
    private val DEBUG_LISTA_CHEQUEO = false

    private val brigada = Ui.spinner(activity, FormOptions.brigadas)
    private val proyecto = Ui.spinner(activity, FormOptions.proyectos)
    private val departamento = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val quienSupervisa = Ui.input(activity, "Nombre de quien supervisa")
    private val puesto = Ui.spinner(activity, FormOptions.puestos)
    private val supervisorTrabajo = Ui.input(activity, "Nombre del supervisor del trabajo")
    private val observaciones = Ui.input(activity, "Observaciones", 4)
    private val photos = PhotoAttachmentPanel(activity)
    private val checks = mutableListOf<Pair<String, RadioGroup>>()

    private val preguntas = listOf(
        "¿El personal tiene claro el objetivo del trabajo/actividad que está ejecutando?",
        "¿Se explicó a los trabajadores qué hacer si observan alguna condición insegura?",
        "¿Las condiciones físicas y mentales de los trabajadores son adecuadas para el trabajo?",
        "¿Los trabajadores están capacitados sobre las tareas que les corresponden desarrollar?",
        "¿Cuenta el personal con su ropa de trabajo y EPP requerido?",
        "¿El trabajo a realizar requiere EPP específico? ¿Está disponible?",
        "¿Se tienen identificados los riesgos a que estarán expuestos los trabajadores?",
        "¿Los equipos, accesorios o herramientas están en buen estado y disponibles?",
        "¿Se comprobó que no existen fugas o derrames en la instalación/equipos?",
        "¿Los trabajadores saben qué hacer en caso de emergencia?",
        "¿Conoce el personal la ruta de evacuación del sitio?",
        "¿El sitio de trabajo cuenta con orden y limpieza?",
        "¿Se planearon las actividades con identificación de riesgos y medidas de control mediante AST?",
        "¿Se tienen los procedimientos o instructivos del área de trabajo?",
        "¿Se tramitó el permiso de trabajo con riesgo cuando aplique?",
        "¿Se aplicaron las medidas de seguridad identificadas en AST y/o PPTR?",
        "¿Está capacitado el personal para uso correcto de EPP específico?",
        "¿Requiere supervisión constante durante las actividades?",
        "¿Hay operaciones simultáneas en el área de trabajo?",
        "¿Se cumplen las distancias de seguridad y parámetros de calidad?"
    )

    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        (activity as? MainActivity)?.bindPhotoPanel(photos)
        addView(Ui.title(activity, "Formato Supervisión Segura"))
        addView(Ui.button(activity, "← Menú", { activity.setContentView(HomeScreen(activity).build()) }))
        addView(Ui.label(activity, "Brigada")); addView(brigada)
        addView(Ui.label(activity, "Proyecto")); addView(proyecto)
        addView(Ui.label(activity, "Departamento supervisado")); addView(departamento)
        addView(quienSupervisa)
        addView(Ui.label(activity, "Puesto que ocupa")); addView(puesto)
        addView(supervisorTrabajo)
        addView(Ui.section(activity, "Lista de chequeo"))
        preguntas.forEachIndexed { i, q ->
            addView(Ui.label(activity, "${i + 1}. $q"))
            val group = Ui.radioRow(activity, listOf("SI", "NO"), 0)
            checks.add(q to group); addView(group)
        }
        addView(Ui.section(activity, "Observaciones y fotos"))
        addView(observaciones); addView(photos.view)

        if (DEBUG_LISTA_CHEQUEO) {
            cargarDatosPrueba()
        }

        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
    })


    /**
     * Carga datos de prueba para generar el PDF sin llenar el formulario a mano.
     * Para regresar al modo normal, cambia DEBUG_LISTA_CHEQUEO a false.
     */
    private fun cargarDatosPrueba() {
        brigada.setSelection(FormOptions.brigadas.indexOf("371").coerceAtLeast(0))
        proyecto.setSelection(FormOptions.proyectos.indexOf("ALACTE").coerceAtLeast(0))
        departamento.setSelection(FormOptions.areasDepartamentos.indexOf("Adquisición (Registro)").coerceAtLeast(0))
        quienSupervisa.setText("Francisco Alvarado")
        puesto.setSelection(FormOptions.puestos.indexOf("Sobrestante").coerceAtLeast(0))
        supervisorTrabajo.setText("Supervisor de campo")
        observaciones.setText("Se revisa condición general del área y cumplimiento de medidas HSE.")

        checks.forEachIndexed { index, (_, group) ->
            // Patrón de prueba: casi todo SI y algunos NO para validar alineación.
            val respuestaNo = index == 1 || index == 9
            val childIndex = if (respuestaNo) 1 else 0
            group.check(group.getChildAt(childIndex).id)
        }
    }

    private fun generate() {
        val report = object : HseReport {
            override val format = HseFormat.SUPERVISION_WORD
            override val folio = "FSS-${dateCompact()}-${System.currentTimeMillis().toString().takeLast(6)}"
            override fun toJson() = baseJson(format, folio).apply {
                put("datos_generales", JSONObject().put("brigada", brigada.selectedItem.toString()).put("proyecto", proyecto.selectedItem.toString()).put("departamento_supervisado", departamento.selectedItem.toString()).put("quien_supervisa", quienSupervisa.text.toString()).put("puesto", puesto.selectedItem.toString()).put("supervisor_trabajo", supervisorTrabajo.text.toString()))
                put("checklist", JSONArray().apply { checks.forEach { (q, g) -> put(JSONObject().put("pregunta", q).put("respuesta", selected(g))) } })
                put("comentarios", observaciones.text.toString())
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = listOf("Brigada: ${brigada.selectedItem}", "Proyecto: ${proyecto.selectedItem}", "Departamento supervisado: ${departamento.selectedItem}", "Supervisa: ${quienSupervisa.text}", "Puesto: ${puesto.selectedItem}", "Supervisor del trabajo: ${supervisorTrabajo.text}", "Observaciones: ${observaciones.text}")
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }
}
