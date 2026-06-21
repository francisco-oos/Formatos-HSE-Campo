package com.sinopec.formatoshsecampo.features.supervisiondiaria

import android.app.Activity
import android.view.View
import android.widget.*
import com.sinopec.formatoshsecampo.MainActivity
import com.sinopec.formatoshsecampo.core.pdf.SimplePdfService
import com.sinopec.formatoshsecampo.core.debug.DebugConfig
import com.sinopec.formatoshsecampo.core.profile.UserProfile
import com.sinopec.formatoshsecampo.core.profile.UserProfileStore
import com.sinopec.formatoshsecampo.core.config.FormOptions
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import com.sinopec.formatoshsecampo.ui.Ui
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ScrollView

/** Formato equivalente a la app estable Supervisión Segura, dejado como módulo separado. */
class SupervisionDiariaScreen(private val activity: Activity) {
    private val nombre = Ui.input(activity, "Nombre")
    private val idEmpleado = Ui.input(activity, "ID empleado")
    private val categoria = Ui.spinner(activity, FormOptions.puestos)
    private val departamentoJefe = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val departamentoJefeContainer = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        addView(Ui.label(activity, "Departamento del jefe"))
        addView(departamentoJefe)
    }
    private val volante = Ui.spinner(activity, FormOptions.volantes)
    private val comentarios = Ui.input(activity, "Comentarios", 3)
    private val checks = mutableListOf<Pair<String, RadioGroup>>()

    /*
     * DEBUG_SUPERVISION_DIARIA
     * true  = carga datos de prueba al abrir el formato.
     * false = captura normal en campo.
     */
    private val DEBUG_SUPERVISION_DIARIA = DebugConfig.SUPERVISION_DIARIA
    private val photos = PhotoAttachmentPanel(activity)

    private val preguntas = listOf(
        "¿Hoy me encuentro bien?", "¿Tengo casco?", "¿Mi overol está en buen estado?",
        "¿Mis botas están en buen estado?", "¿Mis polainas están en buen estado?",
        "¿Mi chaleco está en buen estado?", "¿Cuento con guantes?", "¿Cuento con lentes?",
        "¿Cuento con barbiquejo?", "¿Mis compañeros se encuentran bien?",
        "¿Yo estoy listo para ir a trabajar?", "¿Recibí mi equipo y herramienta en buen estado?",
        "¿Tengo identificados los riesgos a los que estoy expuesto?", "¿Sé qué hacer en caso de una emergencia?",
        "¿Se planearon las actividades con la identificación de riesgos?", "¿Se tienen los procedimientos o instructivos de trabajo?"
    )

    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        (activity as? MainActivity)?.bindPhotoPanel(photos)
        addView(Ui.title(activity, "Check Supervisión Segura"))
        addView(Ui.button(activity, "← Menú", { activity.setContentView(HomeScreen(activity).build()) }))
        addView(Ui.button(activity, "No soy yo / cambiar perfil", { seleccionarPerfil() }))
        cargarUltimoPerfil()
        addView(nombre); addView(idEmpleado)
        addView(Ui.label(activity, "Categoría")); addView(categoria)
        addView(departamentoJefeContainer)
        configurarDepartamentoJefe()
        addView(Ui.label(activity, "Volante / cuadrilla")); addView(volante)
        addView(Ui.section(activity, "Checklist"))
        preguntas.forEach { q ->
            addView(Ui.label(activity, q))
            val group = Ui.radioRow(activity, listOf("SI", "NO", "CAMBIO"), 0)
            checks.add(q to group); addView(group)
        }
        addView(Ui.section(activity, "Comentarios y fotos"))
        addView(comentarios); addView(photos.view)

        if (DEBUG_SUPERVISION_DIARIA) {
            cargarDatosPrueba()
        }

        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
    })

    private fun configurarDepartamentoJefe() {
        categoria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                departamentoJefeContainer.visibility = if (categoria.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun categoriaFinal(): String {
        val seleccionado = categoria.selectedItem?.toString().orEmpty()
        return if (seleccionado == "Jefe de Departamento") {
            "$seleccionado - ${departamentoJefe.selectedItem}"
        } else seleccionado
    }

    /**
     * Datos de prueba para no llenar manualmente cuando se ajuste el formato.
     * Cambia DEBUG_SUPERVISION_DIARIA a true para activarlo.
     */
    private fun cargarDatosPrueba() {
        nombre.setText("Francisco Alvarado")
        idEmpleado.setText("12345")
        categoria.setSelection(FormOptions.puestos.indexOf("Sobrestante").coerceAtLeast(0))
        volante.setSelection(FormOptions.volantes.indexOf("V4").coerceAtLeast(0))
        comentarios.setText("Se realiza supervisión segura antes del inicio de actividades.")

        checks.forEachIndexed { index, (_, group) ->
            val childIndex = when {
                index == 5 -> 2 // CAMBIO para validar salida
                index == 12 -> 1 // NO para validar salida
                else -> 0
            }
            group.check(group.getChildAt(childIndex).id)
        }
    }

    private fun generate() {
        if (nombre.text.toString().trim().isBlank()) { Toast.makeText(activity, "Captura el nombre", Toast.LENGTH_SHORT).show(); return }
        val report = object : HseReport {
            override val format = HseFormat.SUPERVISION_DIARIA
            override val folio = "SS-${dateCompact()}-${System.currentTimeMillis().toString().takeLast(6)}"
            override fun toJson() = baseJson(format, folio).apply {
                put("datos_generales", JSONObject().put("nombre", nombre.text.toString()).put("id_empleado", idEmpleado.text.toString()).put("categoria", categoriaFinal()).put("departamento_jefe", departamentoJefe.selectedItem.toString()).put("volante", volante.selectedItem.toString()))
                put("comentarios", comentarios.text.toString())
                put("checklist", JSONArray().apply { checks.forEach { (q, g) -> put(JSONObject().put("pregunta", q).put("respuesta", selected(g))) } })
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = listOf("Nombre: ${nombre.text}", "ID: ${idEmpleado.text}", "Categoría: ${categoriaFinal()}", "Volante: ${volante.selectedItem}", "Comentarios: ${comentarios.text}")
        guardarPerfilActual()
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }

    private fun cargarUltimoPerfil() {
        UserProfileStore.latest(activity)?.let { aplicarPerfil(it) }
    }

    private fun seleccionarPerfil() {
        UserProfileStore.showChooser(activity, onSelected = { aplicarPerfil(it) }, onNew = {
            nombre.setText("")
            idEmpleado.setText("")
        })
    }

    private fun aplicarPerfil(profile: UserProfile) {
        nombre.setText(profile.nombre)
        idEmpleado.setText(profile.idEmpleado)
        setSpinner(categoria, FormOptions.puestos, profile.puesto)
        setSpinner(departamentoJefe, FormOptions.areasDepartamentos, profile.departamentoJefe)
        departamentoJefeContainer.visibility = if (categoria.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
        setSpinner(volante, FormOptions.volantes, profile.volante)
    }

    private fun guardarPerfilActual() {
        UserProfileStore.save(activity, UserProfile(
            nombre = nombre.text.toString().trim(),
            idEmpleado = idEmpleado.text.toString().trim(),
            puesto = categoria.selectedItem?.toString().orEmpty(),
            departamentoJefe = departamentoJefe.selectedItem?.toString().orEmpty(),
            volante = volante.selectedItem?.toString().orEmpty()
        ))
    }

    private fun setSpinner(spinner: Spinner, items: List<String>, value: String) {
        val idx = items.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (idx >= 0) spinner.setSelection(idx)
    }
}

fun selected(group: RadioGroup): String = group.findViewById<RadioButton>(group.checkedRadioButtonId)?.text?.toString() ?: ""
fun dateIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
fun timeIso(): String = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
fun dateCompact(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
fun baseJson(format: HseFormat, folio: String) = JSONObject().apply {
    put("tipo_formato", format.code); put("version_formato", format.version)
    put("app_version", com.sinopec.formatoshsecampo.BuildConfig.VERSION_NAME)
    put("fecha", dateIso()); put("hora", timeIso()); put("folio", folio)
    put("created_at_device", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()))
}
