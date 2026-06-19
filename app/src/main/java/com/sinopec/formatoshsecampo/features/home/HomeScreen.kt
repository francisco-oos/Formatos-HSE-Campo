package com.sinopec.formatoshsecampo.features.home

import android.app.Activity
import android.widget.LinearLayout
import com.sinopec.formatoshsecampo.features.supervisiondiaria.SupervisionDiariaScreen
import com.sinopec.formatoshsecampo.features.supervisionword.SupervisionWordScreen
import com.sinopec.formatoshsecampo.features.tarjetaobservacion.TarjetaObservacionScreen
import com.sinopec.formatoshsecampo.ui.Ui
import android.widget.ScrollView

/** Menú principal para elegir qué formato llenar. */
class HomeScreen(private val activity: Activity) {
    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        addView(Ui.title(activity, "Formatos HSE Campo"))
        addView(Ui.section(activity, "Seleccione el formato"))
        addButton("Supervisión Segura") { activity.setContentView(SupervisionDiariaScreen(activity).build()) }
        addButton("Tarjeta de Observación de Seguridad") { activity.setContentView(TarjetaObservacionScreen(activity).build()) }
        addButton("Formato Supervisión Segura") { activity.setContentView(SupervisionWordScreen(activity).build()) }
    })

    private fun LinearLayout.addButton(text: String, action: () -> Unit) {
        addView(Ui.button(activity, text, action))
    }
}
