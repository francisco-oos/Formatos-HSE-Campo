package com.sinopec.formatoshsecampo.features.supervisionword

import android.app.Activity
import android.view.View
import android.widget.*
import com.sinopec.formatoshsecampo.MainActivity
import com.sinopec.formatoshsecampo.core.pdf.SimplePdfService
import com.sinopec.formatoshsecampo.core.draft.FormDraftStore
import com.sinopec.formatoshsecampo.core.debug.DebugConfig
import com.sinopec.formatoshsecampo.core.profile.UserProfile
import com.sinopec.formatoshsecampo.core.profile.UserProfileStore
import com.sinopec.formatoshsecampo.core.config.FormOptions
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import com.sinopec.formatoshsecampo.features.supervisiondiaria.*
import com.sinopec.formatoshsecampo.ui.Ui
import com.sinopec.formatoshsecampo.ui.SignaturePadView
import org.json.JSONArray
import org.json.JSONObject
import android.widget.ScrollView

/** Formato basado en el Word: Lista de Chequeo Supervisión Segura SSM-HSE-F-20. */
class SupervisionWordScreen(private val activity: Activity) {
    private val DRAFT_KEY = "lista_chequeo_supervision"
    /*
     * MODO PRUEBAS DEL FORMATO LISTA DE CHEQUEO.
     *
     * true  = al entrar al formato se cargan datos de prueba automáticamente.
     * false = modo normal para capturar datos reales en campo.
     *
     * Úsalo solo mientras ajustamos el PDF visual.
     */
    private val DEBUG_LISTA_CHEQUEO = DebugConfig.LISTA_CHEQUEO_SUPERVISION

    private val brigada = Ui.spinner(activity, FormOptions.brigadas)
    private val proyecto = Ui.spinner(activity, FormOptions.proyectos)
    private val departamento = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val quienSupervisa = Ui.input(activity, "Nombre de quien supervisa")
    private val puesto = Ui.spinner(activity, FormOptions.puestos)
    private val departamentoJefe = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val departamentoJefeContainer = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        addView(Ui.label(activity, "Departamento del jefe"))
        addView(departamentoJefe)
    }
    private val supervisorTrabajo = Ui.input(activity, "Nombre del supervisor del trabajo")
    private val observaciones = Ui.input(activity, "Observaciones", 4)
    private val firmaSupervisorTrabajo = SignaturePadView(activity)
    private val firmaQuienSupervisa = SignaturePadView(activity)
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
        addView(accionesSuperiores())
        cargarUltimoPerfil()
        addView(Ui.label(activity, "Brigada")); addView(brigada)
        addView(Ui.label(activity, "Proyecto")); addView(proyecto)
        addView(Ui.label(activity, "Departamento supervisado")); addView(departamento)
        addView(quienSupervisa)
        addView(Ui.label(activity, "Puesto que ocupa")); addView(puesto)
        addView(departamentoJefeContainer)
        configurarDepartamentoJefe()
        addView(supervisorTrabajo)
        addView(Ui.section(activity, "Lista de chequeo"))
        preguntas.forEachIndexed { i, q ->
            addView(Ui.label(activity, "${i + 1}. $q"))
            val group = Ui.radioRow(activity, listOf("SI", "NO"), 0)
            checks.add(q to group); addView(group)
        }
        addView(Ui.section(activity, "Observaciones"))
        addView(observaciones)
        addView(Ui.section(activity, "Firmas"))
        addView(Ui.label(activity, "Firma del supervisor del trabajo"))
        addView(firmaSupervisorTrabajo)
        addView(Ui.button(activity, "Limpiar firma supervisor", { firmaSupervisorTrabajo.clear() }))
        addView(Ui.label(activity, "Firma de quien realizó la supervisión"))
        addView(firmaQuienSupervisa)
        addView(Ui.button(activity, "Limpiar firma de quien supervisa", { firmaQuienSupervisa.clear() }))
        addView(Ui.section(activity, "Fotos anexas"))
        addView(photos.view)

        cargarBorrador()
        if (DEBUG_LISTA_CHEQUEO) {
            cargarDatosPrueba()
        }

        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
    })



    private fun accionesSuperiores(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL

        val fila = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val botonMenu = Ui.button(activity, "← Menú", {
            guardarBorrador()
            activity.setContentView(HomeScreen(activity).build())
        }).apply {
            textSize = 12f
        }

        val botonPerfil = Ui.button(activity, "No soy yo / cambiar perfil", {
            seleccionarPerfil()
        }).apply {
            textSize = 12f
        }

        fila.addView(
            botonMenu,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, 6, 0)
            }
        )
        fila.addView(
            botonPerfil,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f).apply {
                setMargins(6, 0, 0, 0)
            }
        )

        addView(fila)

        val botonLimpiar = Ui.button(activity, "Limpiar todo", {
            confirmarLimpiarTodo()
        }).apply {
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(Ui.red)
        }

        addView(
            botonLimpiar,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 14)
            }
        )
    }

    private fun confirmarLimpiarTodo() {
        android.app.AlertDialog.Builder(activity)
            .setTitle("Limpiar formulario")
            .setMessage("Se borrará toda la información capturada en este formato. ¿Deseas continuar?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Sí, limpiar") { _, _ ->
                limpiarFormulario()
                Toast.makeText(activity, "Formulario limpio", Toast.LENGTH_SHORT).show()
            }
            .show()
    }


    private fun limpiarFormulario() {
        FormDraftStore.clear(DRAFT_KEY)
        brigada.setSelection(0)
        proyecto.setSelection(0)
        departamento.setSelection(0)
        quienSupervisa.setText("")
        puesto.setSelection(0)
        departamentoJefe.setSelection(0)
        departamentoJefeContainer.visibility = View.GONE
        supervisorTrabajo.setText("")
        observaciones.setText("")
        checks.forEach { (_, group) -> resetRadioGroup(group, 0) }
        firmaSupervisorTrabajo.clear()
        firmaQuienSupervisa.clear()
        photos.photos.clear()
        photos.refresh()
    }

    private fun resetRadioGroup(group: RadioGroup, defaultIndex: Int = 0) {
        if (group.childCount > defaultIndex) {
            val rb = group.getChildAt(defaultIndex) as? RadioButton
            if (rb != null) group.check(rb.id) else group.clearCheck()
        } else {
            group.clearCheck()
        }
    }

    private fun configurarDepartamentoJefe() {
        puesto.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun puestoFinal(): String {
        val seleccionado = puesto.selectedItem?.toString().orEmpty()
        return if (seleccionado == "Jefe de Departamento") {
            "$seleccionado - ${departamentoJefe.selectedItem}"
        } else seleccionado
    }

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
        //observaciones.setText("Se revisa condición general del área y cumplimiento de medidas HSE.")

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
                put("datos_generales", JSONObject().put("brigada", brigada.selectedItem.toString()).put("proyecto", proyecto.selectedItem.toString()).put("departamento_supervisado", departamento.selectedItem.toString()).put("quien_supervisa", quienSupervisa.text.toString()).put("puesto", puestoFinal()).put("departamento_jefe", departamentoJefe.selectedItem.toString()).put("supervisor_trabajo", supervisorTrabajo.text.toString()))
                put("checklist", JSONArray().apply { checks.forEach { (q, g) -> put(JSONObject().put("pregunta", q).put("respuesta", selected(g))) } })
                put("comentarios", observaciones.text.toString())
                put("firmas", JSONObject()
                    .put("supervisor_trabajo_png_b64", firmaSupervisorTrabajo.toPngBase64())
                    .put("quien_supervisa_png_b64", firmaQuienSupervisa.toPngBase64())
                    .put("supervisor_trabajo_presente", firmaSupervisorTrabajo.hasSignature)
                    .put("quien_supervisa_presente", firmaQuienSupervisa.hasSignature)
                )
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = listOf("Brigada: ${brigada.selectedItem}", "Proyecto: ${proyecto.selectedItem}", "Departamento supervisado: ${departamento.selectedItem}", "Supervisa: ${quienSupervisa.text}", "Puesto: ${puestoFinal()}", "Supervisor del trabajo: ${supervisorTrabajo.text}", "Observaciones: ${observaciones.text}")
        guardarPerfilActual()
        FormDraftStore.clear(DRAFT_KEY)
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }


private fun guardarBorrador() {
    FormDraftStore.save(DRAFT_KEY, JSONObject()
        .put("brigada", brigada.selectedItemPosition)
        .put("proyecto", proyecto.selectedItemPosition)
        .put("departamento", departamento.selectedItemPosition)
        .put("quien_supervisa", quienSupervisa.text.toString())
        .put("puesto", puesto.selectedItemPosition)
        .put("departamento_jefe", departamentoJefe.selectedItemPosition)
        .put("supervisor_trabajo", supervisorTrabajo.text.toString())
        .put("observaciones", observaciones.text.toString())
        .put("checks", JSONArray().apply { checks.forEach { (_, g) -> put(selected(g)) } })
        .put("firma_supervisor_trabajo_png_b64", firmaSupervisorTrabajo.toPngBase64())
        .put("firma_quien_supervisa_png_b64", firmaQuienSupervisa.toPngBase64())
    )
}

private fun cargarBorrador() {
    val d = FormDraftStore.get(DRAFT_KEY) ?: return
    brigada.setSelection(d.optInt("brigada", brigada.selectedItemPosition).coerceAtLeast(0))
    proyecto.setSelection(d.optInt("proyecto", proyecto.selectedItemPosition).coerceAtLeast(0))
    departamento.setSelection(d.optInt("departamento", departamento.selectedItemPosition).coerceAtLeast(0))
    quienSupervisa.setText(d.optString("quien_supervisa"))
    puesto.setSelection(d.optInt("puesto", puesto.selectedItemPosition).coerceAtLeast(0))
    departamentoJefe.setSelection(d.optInt("departamento_jefe", departamentoJefe.selectedItemPosition).coerceAtLeast(0))
    departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
    supervisorTrabajo.setText(d.optString("supervisor_trabajo"))
    observaciones.setText(d.optString("observaciones"))
    val arr = d.optJSONArray("checks") ?: return
    for (i in 0 until kotlin.math.min(arr.length(), checks.size)) {
        val value = arr.optString(i)
        val group = checks[i].second
        for (c in 0 until group.childCount) {
            val rb = group.getChildAt(c) as? RadioButton ?: continue
            if (rb.text.toString() == value) group.check(rb.id)
        }
    }
}

    private fun cargarUltimoPerfil() {
        UserProfileStore.latest(activity)?.let { aplicarPerfil(it) }
    }

    private fun seleccionarPerfil() {
        guardarBorrador()
        UserProfileStore.showChooser(activity, onSelected = { aplicarPerfil(it) }, onNew = {
            quienSupervisa.setText("")
            supervisorTrabajo.setText("")
            brigada.setSelection(0)
            proyecto.setSelection(0)
            departamento.setSelection(0)
            puesto.setSelection(0)
            departamentoJefe.setSelection(0)
            departamentoJefeContainer.visibility = View.GONE
        })
    }

    private fun aplicarPerfil(profile: UserProfile) {
        setSpinner(brigada, FormOptions.brigadas, profile.brigada)
        setSpinner(proyecto, FormOptions.proyectos, profile.proyecto)
        setSpinner(departamento, FormOptions.areasDepartamentos, profile.areaDepartamento)
        quienSupervisa.setText(profile.nombre)
        setSpinner(puesto, FormOptions.puestos, profile.puesto)
        setSpinner(departamentoJefe, FormOptions.areasDepartamentos, profile.departamentoJefe)
        departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
    }

    private fun guardarPerfilActual() {
        UserProfileStore.save(activity, UserProfile(
            nombre = quienSupervisa.text.toString().trim(),
            puesto = puesto.selectedItem?.toString().orEmpty(),
            departamentoJefe = departamentoJefe.selectedItem?.toString().orEmpty(),
            areaDepartamento = departamento.selectedItem?.toString().orEmpty(),
            brigada = brigada.selectedItem?.toString().orEmpty(),
            proyecto = proyecto.selectedItem?.toString().orEmpty()
        ))
    }

    private fun setSpinner(spinner: Spinner, items: List<String>, value: String) {
        val idx = items.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (idx >= 0) spinner.setSelection(idx)
    }
}
