package com.sinopec.formatoshsecampo.features.tarjetaobservacion

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
import org.json.JSONArray
import org.json.JSONObject
import android.widget.ScrollView

/** Captura del formato de dos caras: Tarjeta de Observación de Seguridad. */
class TarjetaObservacionScreen(private val activity: Activity) {
    private val DRAFT_KEY = "tarjeta_observacion"
    private val area = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val actividad = Ui.input(activity, "Actividad realizada")
    private val tipo = Ui.spinner(activity, FormOptions.tiposObservacionTarjeta)
    private val tipoOtro = Ui.input(activity, "Especifica el otro tipo de observación")
    private val razon = Ui.input(activity, "Razón de la observación", 3)
    private val detalles = Ui.input(activity, "Detalles de la observación", 3)
    private val acciones = Ui.input(activity, "Acciones tomadas / recomendaciones", 3)
    private val puntoAccion = Ui.spinner(activity, listOf("Sí", "No"))
    private val asignada = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val nombre = Ui.input(activity, "Nombre de quien reporta")
    private val departamento = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val puesto = Ui.spinner(activity, FormOptions.puestos)
    private val departamentoJefe = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val departamentoJefeContainer = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        addView(Ui.label(activity, "Departamento del jefe"))
        addView(departamentoJefe)
    }
    private val photos = PhotoAttachmentPanel(activity)
    private val checks = mutableListOf<Pair<String, RadioGroup>>()
    //CODIGO DEBUG LLENADO DE DATOS
    private val DEBUG_TOS = DebugConfig.TARJETA_OBSERVACION

    private data class ChecklistGroup(val title: String, val items: List<String>)

    private val checklistGroups = listOf(
        ChecklistGroup("Conducta Personal", listOf("Uso del EPP", "Ojos en la Tarea", "Posición de trabajo segura", "Alzando/jalando/empujando/cargando")),
        ChecklistGroup("Ambiente de Trabajo", listOf("Orden y Limpieza", "Iluminación", "Ventilación", "Superficie Nivelada")),
        ChecklistGroup("Equipo/Herramientas", listOf("Adecuadas para la tarea", "Bloqueos/Aislamientos", "Arnés/Línea de vida")),
        ChecklistGroup("Procedimientos", listOf("Análisis de Seguridad del Trabajo", "Permiso de Trabajo", "Instructivo de Trabajo", "Respuesta a Emergencias"))
    )

    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        (activity as? MainActivity)?.bindPhotoPanel(photos)
        addView(Ui.title(activity, "Tarjeta de Observación de Seguridad"))
        addView(accionesSuperiores())
        cargarUltimoPerfil()
        addView(Ui.label(activity, "Tipo de observación")); addView(tipo)
        addView(tipoOtro.apply { visibility = View.GONE })
        configurarCampoOtroTipo()
        addView(Ui.label(activity, "Área / Departamento")); addView(area)
        addView(actividad)
        addView(Ui.section(activity, "Acciones de seguridad y observaciones"))
        checklistGroups.forEach { grupo ->
            addView(Ui.section(activity, grupo.title))
            grupo.items.forEach { q ->
                addView(Ui.label(activity, q))
                val group = Ui.radioRow(activity, listOf("Seguro", "Inseguro", "No Aplica"), 2)
                checks.add(q to group)
                addView(group)
            }
        }
        addView(Ui.section(activity, "Reverso"))
        addView(razon); addView(detalles); addView(acciones)
        addView(Ui.label(activity, "Punto de acción generado")); addView(puntoAccion)
        addView(Ui.label(activity, "Acción asignada a")); addView(asignada); addView(nombre)
        addView(Ui.label(activity, "Departamento")); addView(departamento)
        addView(Ui.label(activity, "Puesto de trabajo")); addView(puesto)
        addView(departamentoJefeContainer)
        configurarDepartamentoJefe()
        addView(Ui.section(activity, "Fotos anexas")); addView(photos.view)
        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
        cargarBorrador()
        //CODIGO DEBUG LLENADO DE DATOS
        if (DEBUG_TOS) {
            cargarDatosPrueba()
        }
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
        area.setSelection(0)
        actividad.setText("")
        tipo.setSelection(0)
        tipoOtro.setText("")
        tipoOtro.visibility = View.GONE
        razon.setText("")
        detalles.setText("")
        acciones.setText("")
        puntoAccion.setSelection(0)
        asignada.setSelection(0)
        nombre.setText("")
        departamento.setSelection(0)
        puesto.setSelection(0)
        departamentoJefe.setSelection(0)
        departamentoJefeContainer.visibility = View.GONE
        checks.forEach { (_, group) -> resetRadioGroup(group, 2) }
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

    private fun configurarCampoOtroTipo() {
        tipo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                tipoOtro.visibility = if (tipo.selectedItem?.toString() == "Otro") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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

    private fun tipoObservacionFinal(): String {
        val seleccionado = tipo.selectedItem?.toString().orEmpty()
        val otro = tipoOtro.text.toString().trim()
        return if (seleccionado == "Otro" && otro.isNotBlank()) "Otro: $otro" else seleccionado
    }

    private fun puestoFinal(): String {
        val seleccionado = puesto.selectedItem?.toString().orEmpty()
        return if (seleccionado == "Jefe de Departamento") {
            "$seleccionado - ${departamentoJefe.selectedItem}"
        } else seleccionado
    }

    private fun cargarDatosPrueba() {
        area.setSelection(FormOptions.areasDepartamentos.indexOf("Adquisición (Registro)").coerceAtLeast(0))
        actividad.setText("Inspección en comedor")

        tipo.setSelection(0) // 0 Acto Inseguro, 5 Oportunidad de Mejora

        razon.setText("Se observa presencia de moscas en el área del comedor.")
        detalles.setText("La condición puede generar molestias al personal y posible riesgo sanitario.")
        acciones.setText("Solicitar limpieza profunda y fumigación del área.")
        puntoAccion.setSelection(0) // Sí
        asignada.setSelection(FormOptions.areasDepartamentos.indexOf("Logística").coerceAtLeast(0))

        nombre.setText("Bartolo García Rueda de León")
        departamento.setSelection(FormOptions.areasDepartamentos.indexOf("Adquisición (Registro)").coerceAtLeast(0))
        puesto.setSelection(FormOptions.puestos.indexOf("Sobrestante").coerceAtLeast(0))

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
                put("datos_generales", JSONObject().put("area_departamento", area.selectedItem.toString()).put("actividad_realizada", actividad.text.toString()).put("tipo_observacion", tipoObservacionFinal()).put("tipo_observacion_otro", tipoOtro.text.toString()))
                put("observaciones", JSONObject().put("razon", razon.text.toString()).put("detalles", detalles.text.toString()))
                put("acciones", JSONObject().put("acciones_tomadas_recomendaciones", acciones.text.toString()).put("punto_accion_generado", puntoAccion.selectedItem.toString()).put("accion_asignada_a", asignada.selectedItem.toString()))
                put("reporta", JSONObject().put("nombre", nombre.text.toString()).put("departamento", departamento.selectedItem.toString()).put("puesto_trabajo", puestoFinal()).put("departamento_jefe", departamentoJefe.selectedItem.toString()))
                put("checklist", JSONArray().apply { checks.forEach { (q, g) -> put(JSONObject().put("punto", q).put("respuesta", selected(g))) } })
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = listOf(
            "Tipo: ${tipoObservacionFinal()}", "Área/Departamento: ${area.selectedItem}", "Actividad: ${actividad.text}",
            "Razón: ${razon.text}", "Detalles: ${detalles.text}", "Acciones/Recomendaciones: ${acciones.text}",
            "Punto de acción: ${puntoAccion.selectedItem}", "Asignada a: ${asignada.selectedItem}", "Reporta: ${nombre.text} / ${departamento.selectedItem} / ${puestoFinal()}"
        )
        guardarPerfilActual()
        FormDraftStore.clear(DRAFT_KEY)
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }


private fun guardarBorrador() {
    FormDraftStore.save(DRAFT_KEY, JSONObject()
        .put("area", area.selectedItemPosition)
        .put("actividad", actividad.text.toString())
        .put("tipo", tipo.selectedItemPosition)
        .put("tipo_otro", tipoOtro.text.toString())
        .put("razon", razon.text.toString())
        .put("detalles", detalles.text.toString())
        .put("acciones", acciones.text.toString())
        .put("punto_accion", puntoAccion.selectedItemPosition)
        .put("asignada", asignada.selectedItemPosition)
        .put("nombre", nombre.text.toString())
        .put("departamento", departamento.selectedItemPosition)
        .put("puesto", puesto.selectedItemPosition)
        .put("departamento_jefe", departamentoJefe.selectedItemPosition)
        .put("checks", JSONArray().apply { checks.forEach { (_, g) -> put(selected(g)) } })
    )
}

private fun cargarBorrador() {
    val d = FormDraftStore.get(DRAFT_KEY) ?: return
    area.setSelection(d.optInt("area", area.selectedItemPosition).coerceAtLeast(0))
    actividad.setText(d.optString("actividad"))
    tipo.setSelection(d.optInt("tipo", tipo.selectedItemPosition).coerceAtLeast(0))
    tipoOtro.setText(d.optString("tipo_otro"))
    tipoOtro.visibility = if (tipo.selectedItem?.toString() == "Otro") View.VISIBLE else View.GONE
    razon.setText(d.optString("razon"))
    detalles.setText(d.optString("detalles"))
    acciones.setText(d.optString("acciones"))
    puntoAccion.setSelection(d.optInt("punto_accion", puntoAccion.selectedItemPosition).coerceAtLeast(0))
    asignada.setSelection(d.optInt("asignada", asignada.selectedItemPosition).coerceAtLeast(0))
    nombre.setText(d.optString("nombre"))
    departamento.setSelection(d.optInt("departamento", departamento.selectedItemPosition).coerceAtLeast(0))
    puesto.setSelection(d.optInt("puesto", puesto.selectedItemPosition).coerceAtLeast(0))
    departamentoJefe.setSelection(d.optInt("departamento_jefe", departamentoJefe.selectedItemPosition).coerceAtLeast(0))
    departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
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
            nombre.setText("")
            departamento.setSelection(0)
            area.setSelection(0)
            puesto.setSelection(0)
            departamentoJefe.setSelection(0)
            departamentoJefeContainer.visibility = View.GONE
        })
    }

    private fun aplicarPerfil(profile: UserProfile) {
        nombre.setText(profile.nombre)
        setSpinner(departamento, FormOptions.areasDepartamentos, profile.areaDepartamento)
        setSpinner(area, FormOptions.areasDepartamentos, profile.areaDepartamento)
        setSpinner(puesto, FormOptions.puestos, profile.puesto)
        setSpinner(departamentoJefe, FormOptions.areasDepartamentos, profile.departamentoJefe)
        departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
    }

    private fun guardarPerfilActual() {
        UserProfileStore.save(activity, UserProfile(
            nombre = nombre.text.toString().trim(),
            puesto = puesto.selectedItem?.toString().orEmpty(),
            departamentoJefe = departamentoJefe.selectedItem?.toString().orEmpty(),
            areaDepartamento = departamento.selectedItem?.toString().orEmpty()
        ))
    }

    private fun setSpinner(spinner: Spinner, items: List<String>, value: String) {
        val idx = items.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (idx >= 0) spinner.setSelection(idx)
    }
}
