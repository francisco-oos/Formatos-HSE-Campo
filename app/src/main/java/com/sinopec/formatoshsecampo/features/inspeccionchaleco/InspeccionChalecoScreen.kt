package com.sinopec.formatoshsecampo.features.inspeccionchaleco

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import com.sinopec.formatoshsecampo.MainActivity
import com.sinopec.formatoshsecampo.R
import com.sinopec.formatoshsecampo.core.config.FormOptions
import com.sinopec.formatoshsecampo.core.pdf.SimplePdfService
import com.sinopec.formatoshsecampo.core.debug.DebugConfig
import com.sinopec.formatoshsecampo.core.profile.UserProfile
import com.sinopec.formatoshsecampo.core.profile.UserProfileStore
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import com.sinopec.formatoshsecampo.features.supervisiondiaria.baseJson
import com.sinopec.formatoshsecampo.features.supervisiondiaria.dateCompact
import com.sinopec.formatoshsecampo.ui.Ui
import org.json.JSONArray
import org.json.JSONObject

private enum class EstadoParte(val texto: String) {
    SIN_REVISAR("Sin revisar"),
    CORRECTO("Correcto"),
    OBSERVACION("Observación"),
    REEMPLAZO("Requiere reemplazo")
}

private data class ParteChaleco(
    val id: Int,
    val nombre: String,
    val checklist: List<String>,
    val imagenRes: Int,
    var estado: EstadoParte = EstadoParte.SIN_REVISAR,
    var observacion: String = ""
)

/** Inspección visual e interactiva de chaleco salvavidas. */
class InspeccionChalecoScreen(private val activity: Activity) {
    private val DEBUG_CHALECO = DebugConfig.INSPECCION_CHALECO

    private val brigada = Ui.spinner(activity, FormOptions.brigadas)
    private val proyecto = Ui.spinner(activity, FormOptions.proyectos)
    private val areaDepartamento = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val volante = Ui.spinner(activity, FormOptions.volantes)
    private val empleado = Ui.input(activity, "Empleado inspeccionado")
    private val idEmpleado = Ui.input(activity, "ID empleado")
    private val puesto = Ui.spinner(activity, FormOptions.puestos)
    private val departamentoJefe = Ui.spinner(activity, FormOptions.areasDepartamentos)
    private val departamentoJefeContainer = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        addView(Ui.label(activity, "Departamento del jefe"))
        addView(departamentoJefe)
    }
    private val supervisor = Ui.input(activity, "Supervisor / HSE")
    private val observacionesGenerales = Ui.input(activity, "Observaciones generales", 3)
    private val photos = PhotoAttachmentPanel(activity)
    private val resumen = TextView(activity)
    private val parteButtons = mutableMapOf<Int, Button>()

    private val partes = mutableListOf(
        ParteChaleco(1, "Estado general del chaleco", listOf("Limpio", "Sin rasgaduras", "Sin deformación visible", "Sin daño exterior"), R.drawable.chaleco_salvavidas_general),
        ParteChaleco(2, "Material de flotación", listOf("Sin deformaciones", "Sin hundimientos", "Sin cortes o perforaciones", "No se siente apelmazado"), R.drawable.chaleco_material_flotacion),
        ParteChaleco(3, "Cintas y correas", listOf("Completas", "Sin desgaste excesivo", "Ajustan correctamente", "Sin cortes"), R.drawable.chaleco_cintas_correas),
        ParteChaleco(4, "Hebillas y broches", listOf("Cierran correctamente", "Sin fracturas", "Sin deformaciones", "No se abren al jalar"), R.drawable.chaleco_hebillas_broches),
        ParteChaleco(5, "Costuras", listOf("Sin roturas", "Sin hilos sueltos", "Integridad correcta", "No hay separación de piezas"), R.drawable.chaleco_costuras),
        ParteChaleco(6, "Silbato de emergencia", listOf("Presente", "Funcional", "Sujeto al chaleco"), R.drawable.chaleco_salvavidas_general),
        ParteChaleco(7, "Reflejantes", listOf("Presentes", "Visibles", "Sin desprendimiento", "Sin desgaste excesivo"), R.drawable.chaleco_reflejantes),
        ParteChaleco(8, "Talla y ajuste", listOf("Talla adecuada", "Ajuste firme al cuerpo", "No queda flojo", "Permite movimiento seguro"), R.drawable.chaleco_cintas_correas)
    )

    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        (activity as? MainActivity)?.bindPhotoPanel(photos)
        setBackgroundColor(Color.rgb(246, 247, 249))
        addView(Ui.title(activity, "Inspección de Chaleco Salvavidas"))
        addView(Ui.button(activity, "← Menú", { activity.setContentView(HomeScreen(activity).build()) }))
        addView(Ui.button(activity, "No soy yo / cambiar perfil", { seleccionarPerfil() }))
        cargarUltimoPerfil()

        addView(card().apply {
            addView(Ui.section(activity, "Datos generales"))
            addLabeled("Brigada", brigada)
            addLabeled("Proyecto", proyecto)
            addLabeled("Área / Departamento", areaDepartamento)
            addLabeled("Volante", volante)
            addLabeled("Empleado inspeccionado", empleado)
            addLabeled("ID empleado", idEmpleado)
            addLabeled("Puesto", puesto)
            addView(departamentoJefeContainer)
            configurarDepartamentoJefe()
            addLabeled("Supervisor / HSE", supervisor)
        })

        addView(card().apply {
            addView(Ui.section(activity, "Inspección visual por partes"))
            addView(TextView(activity).apply {
                text = "Toque la parte del chaleco que desea revisar. Cada botón abre la imagen guía correspondiente y el estado de la falla."
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setPadding(4, 10, 4, 12)
            })
            addView(ImageView(activity).apply {
                setImageResource(R.drawable.chaleco_salvavidas_general)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(0, 8, 0, 8)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 430))
            addView(legend())
            addView(zoneGrid())
            addView(resumen.apply {
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.rgb(32, 36, 40))
                setPadding(8, 18, 8, 8)
            })
        })

        addView(card().apply {
            addView(Ui.section(activity, "Observaciones y evidencia"))
            addView(observacionesGenerales)
            addView(TextView(activity).apply {
                text = "Fotos sugeridas: frente, espalda y acercamiento de la falla si existe."
                textSize = 13f
                setTextColor(Color.DKGRAY)
                setPadding(0, 12, 0, 6)
            })
            addView(photos.view)
        })

        addView(Ui.button(activity, "Generar PDF y compartir", { generate() }))
        if (DEBUG_CHALECO) cargarDatosPrueba()
        actualizarResumen()
    })

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

    private fun LinearLayout.addLabeled(label: String, view: android.view.View) {
        addView(Ui.label(activity, label))
        addView(view)
    }

    private fun card(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(16, 16, 16, 16)
        background = Ui.roundedBg(Color.WHITE, Ui.lightBorder, 18f)
        elevation = 2f
    }

    private fun legend(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        addLegendItem("Sin revisar", Color.rgb(235, 237, 240))
        addLegendItem("Correcto", Color.rgb(46, 204, 113))
        addLegendItem("Observación", Color.rgb(241, 196, 15))
        addLegendItem("Reemplazo", Color.rgb(231, 76, 60))
    }

    private fun LinearLayout.addLegendItem(texto: String, color: Int) {
        addView(TextView(activity).apply {
            text = texto
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(if (color == Color.rgb(235, 237, 240) || color == Color.rgb(241, 196, 15)) Color.DKGRAY else Color.WHITE)
            background = Ui.roundedBg(color, color, 14f)
            setPadding(8, 6, 8, 6)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(3, 4, 3, 10) })
    }

    private fun zoneGrid(): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        partes.chunked(2).forEach { rowParts ->
            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                rowParts.forEach { parte ->
                    val btn = Button(activity).apply {
                        text = "${parte.id}. ${parte.nombre}"
                        textSize = 12f
                        setAllCaps(false)
                        setOnClickListener { editarParte(parte) }
                    }
                    parteButtons[parte.id] = btn
                    actualizarBoton(parte)
                    addView(btn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(4, 4, 4, 4) })
                }
            })
        }
    }

    private fun editarParte(parte: ParteChaleco) {
        val vista = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 8, 20, 0)
            addView(ImageView(activity).apply {
                setImageResource(parte.imagenRes)
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(0, 0, 0, 8)
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 360))
            addView(TextView(activity).apply {
                text = parte.checklist.joinToString("\n") { "✓ $it" }
                textSize = 14f
                setTextColor(Color.DKGRAY)
                setPadding(0, 8, 0, 8)
            })
            addView(EditText(activity).apply {
                hint = "Observación de la falla (opcional)"
                minLines = 2
                setText(parte.observacion)
                tag = "observacion"
            })
        }
        val input = vista.findViewWithTag<EditText>("observacion")
        val opciones = arrayOf("Correcto", "Observación", "Requiere reemplazo")
        AlertDialog.Builder(activity)
            .setTitle(parte.nombre)
            .setView(vista)
            .setSingleChoiceItems(opciones, when (parte.estado) {
                EstadoParte.CORRECTO -> 0
                EstadoParte.OBSERVACION -> 1
                EstadoParte.REEMPLAZO -> 2
                EstadoParte.SIN_REVISAR -> -1
            }) { _, which ->
                parte.estado = when (which) {
                    0 -> EstadoParte.CORRECTO
                    1 -> EstadoParte.OBSERVACION
                    else -> EstadoParte.REEMPLAZO
                }
            }
            .setPositiveButton("Guardar") { _, _ ->
                parte.observacion = input.text.toString().trim()
                if (parte.estado == EstadoParte.SIN_REVISAR) parte.estado = EstadoParte.CORRECTO
                actualizarBoton(parte)
                actualizarResumen()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarBoton(parte: ParteChaleco) {
        val color = when (parte.estado) {
            EstadoParte.SIN_REVISAR -> Color.rgb(235, 237, 240)
            EstadoParte.CORRECTO -> Color.rgb(46, 204, 113)
            EstadoParte.OBSERVACION -> Color.rgb(241, 196, 15)
            EstadoParte.REEMPLAZO -> Color.rgb(231, 76, 60)
        }
        parteButtons[parte.id]?.setBackgroundColor(color)
    }

    private fun actualizarResumen() {
        val correctos = partes.count { it.estado == EstadoParte.CORRECTO }
        val obs = partes.count { it.estado == EstadoParte.OBSERVACION }
        val rep = partes.count { it.estado == EstadoParte.REEMPLAZO }
        val revisadas = partes.count { it.estado != EstadoParte.SIN_REVISAR }
        resumen.text = "Revisadas: $revisadas/8   Correctas: $correctos   Observaciones: $obs   Reemplazo: $rep\nDictamen: ${dictamen()}"
    }

    private fun dictamen(): String = when {
        partes.any { it.estado == EstadoParte.REEMPLAZO } -> "NO APTO PARA USO"
        partes.any { it.estado == EstadoParte.OBSERVACION } -> "APTO CON OBSERVACIONES"
        partes.all { it.estado == EstadoParte.CORRECTO } -> "APTO PARA USO"
        else -> "INSPECCIÓN PENDIENTE"
    }

    private fun cargarDatosPrueba() {
        empleado.setText("Personal de prueba")
        idEmpleado.setText("12345")
        supervisor.setText("Jonh Smith")
        observacionesGenerales.setText("Inspección visual de chaleco salvavidas realizada en campo.")
        partes[0].estado = EstadoParte.CORRECTO
        partes[1].estado = EstadoParte.CORRECTO
        partes[2].estado = EstadoParte.OBSERVACION
        partes[2].observacion = "Correa con desgaste visible."
        partes[3].estado = EstadoParte.CORRECTO
        partes[4].estado = EstadoParte.REEMPLAZO
        partes[4].observacion = "Costura abierta en zona lateral."
        partes[5].estado = EstadoParte.CORRECTO
        partes[6].estado = EstadoParte.CORRECTO
        partes[7].estado = EstadoParte.CORRECTO
        partes.forEach { actualizarBoton(it) }
    }

    private fun selected(spinner: Spinner): String = spinner.selectedItem?.toString().orEmpty()

    private fun generate() {
        val folio = "CHAL-${dateCompact()}-${System.currentTimeMillis().toString().takeLast(6)}"

        val now = java.util.Date()

        val fecha = java.text.SimpleDateFormat(
            "dd/MM/yyyy",
            java.util.Locale.getDefault()
        ).format(now)

        val hora = java.text.SimpleDateFormat(
            "HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(now)

        val report = object : HseReport {
            override val format = HseFormat.INSPECCION_CHALECO
            override val folio = folio

            override fun toJson() = baseJson(format, folio).apply {
                put("fecha", fecha)
                put("hora", hora)
                put("datos_generales", JSONObject()
                    .put("brigada", selected(brigada))
                    .put("proyecto", selected(proyecto))
                    .put("area_departamento", selected(areaDepartamento))
                    .put("volante", selected(volante))
                    .put("empleado", empleado.text.toString())
                    .put("id_empleado", idEmpleado.text.toString())
                    .put("puesto", puestoFinal())
                    .put("departamento_jefe", selected(departamentoJefe))
                    .put("supervisor_hse", supervisor.text.toString()))
                put("partes", JSONArray().apply {
                    partes.forEach { p -> put(JSONObject()
                        .put("id", p.id)
                        .put("parte", p.nombre)
                        .put("estado", p.estado.texto)
                        .put("checklist", JSONArray(p.checklist))
                        .put("observacion", p.observacion)) }
                })
                put("resumen", JSONObject()
                    .put("revisadas", partes.count { it.estado != EstadoParte.SIN_REVISAR })
                    .put("correctas", partes.count { it.estado == EstadoParte.CORRECTO })
                    .put("observaciones", partes.count { it.estado == EstadoParte.OBSERVACION })
                    .put("requiere_reemplazo", partes.count { it.estado == EstadoParte.REEMPLAZO })
                    .put("dictamen", dictamen()))
                put("observaciones_generales", observacionesGenerales.text.toString())
                put("fotos_anexas", photos.photos.size)
            }
        }
        val lines = mutableListOf(
            "Folio: $folio",
            "Fecha: $fecha",
            "Hora: $hora",
            "Brigada: ${selected(brigada)}",
            "Proyecto: ${selected(proyecto)}",
            "Área/Departamento: ${selected(areaDepartamento)}",
            "Volante: ${selected(volante)}",
            "Empleado: ${empleado.text}",
            "ID empleado: ${idEmpleado.text}",
            "Puesto: ${puestoFinal()}",
            "Supervisor/HSE: ${supervisor.text}",
            "Dictamen final: ${dictamen()}",
            "Observaciones generales: ${observacionesGenerales.text}",
            ""
        )
        partes.forEach { p -> lines.add("${p.id}. ${p.nombre}: ${p.estado.texto}${if (p.observacion.isNotBlank()) " - ${p.observacion}" else ""}") }
        guardarPerfilActual()
        SimplePdfService(activity).createBasicReportPdf(report, lines, photos.photos).also { SimplePdfService(activity).sharePdf(it) }
    }

    private fun cargarUltimoPerfil() {
        UserProfileStore.latest(activity)?.let { aplicarPerfil(it) }
    }

    private fun seleccionarPerfil() {
        UserProfileStore.showChooser(activity, onSelected = { aplicarPerfil(it) }, onNew = {
            empleado.setText("")
            idEmpleado.setText("")
        })
    }

    private fun aplicarPerfil(profile: UserProfile) {
        empleado.setText(profile.nombre)
        idEmpleado.setText(profile.idEmpleado)
        setSpinner(puesto, FormOptions.puestos, profile.puesto)
        setSpinner(departamentoJefe, FormOptions.areasDepartamentos, profile.departamentoJefe)
        departamentoJefeContainer.visibility = if (puesto.selectedItem?.toString() == "Jefe de Departamento") View.VISIBLE else View.GONE
        setSpinner(areaDepartamento, FormOptions.areasDepartamentos, profile.areaDepartamento)
        setSpinner(volante, FormOptions.volantes, profile.volante)
        setSpinner(brigada, FormOptions.brigadas, profile.brigada)
        setSpinner(proyecto, FormOptions.proyectos, profile.proyecto)
    }

    private fun guardarPerfilActual() {
        UserProfileStore.save(activity, UserProfile(
            nombre = empleado.text.toString().trim(),
            idEmpleado = idEmpleado.text.toString().trim(),
            puesto = puesto.selectedItem?.toString().orEmpty(),
            departamentoJefe = departamentoJefe.selectedItem?.toString().orEmpty(),
            areaDepartamento = areaDepartamento.selectedItem?.toString().orEmpty(),
            volante = volante.selectedItem?.toString().orEmpty(),
            brigada = brigada.selectedItem?.toString().orEmpty(),
            proyecto = proyecto.selectedItem?.toString().orEmpty()
        ))
    }

    private fun setSpinner(spinner: Spinner, items: List<String>, value: String) {
        val idx = items.indexOfFirst { it.equals(value, ignoreCase = true) }
        if (idx >= 0) spinner.setSelection(idx)
    }
}
