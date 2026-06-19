package com.sinopec.formatoshsecampo.features.home

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.sinopec.formatoshsecampo.features.inspeccionchaleco.InspeccionChalecoScreen
import com.sinopec.formatoshsecampo.features.supervisiondiaria.SupervisionDiariaScreen
import com.sinopec.formatoshsecampo.features.supervisionword.SupervisionWordScreen
import com.sinopec.formatoshsecampo.features.tarjetaobservacion.TarjetaObservacionScreen
import com.sinopec.formatoshsecampo.ui.Ui

/** Menú principal profesional para elegir qué formato llenar. */
class HomeScreen(private val activity: Activity) {
    fun build(): ScrollView = Ui.scroll(activity, Ui.root(activity).apply {
        setBackgroundColor(Color.rgb(246, 247, 249))
        addView(Ui.title(activity, "Formatos HSE Campo"))
        addView(TextView(activity).apply {
            text = "Seleccione el formato a levantar en campo"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 0, 0, 18)
        })

        addFormatCard(
            title = "Supervisión Segura",
            subtitle = "Revisión diaria rápida del personal, equipo y condiciones de trabajo.",
            badge = "HSE",
            action = { activity.setContentView(SupervisionDiariaScreen(activity).build()) }
        )

        addFormatCard(
            title = "Tarjeta de Observación de Seguridad",
            subtitle = "Registro de actos, condiciones inseguras, acciones positivas y oportunidades de mejora.",
            badge = "TOS",
            action = { activity.setContentView(TarjetaObservacionScreen(activity).build()) }
        )

        addFormatCard(
            title = "Formato Supervisión Segura",
            subtitle = "Lista de chequeo completa para supervisión y evidencia fotográfica.",
            badge = "PDF",
            action = { activity.setContentView(SupervisionWordScreen(activity).build()) }
        )


        addFormatCard(
            title = "Inspección de Chaleco Salvavidas",
            subtitle = "Inspección visual por partes: costuras, broches, reflejantes, flotación, cintas y ajuste.",
            badge = "CHALECO",
            action = { activity.setContentView(InspeccionChalecoScreen(activity).build()) }
        )
    })

    private fun LinearLayout.addFormatCard(title: String, subtitle: String, badge: String, action: () -> Unit) {
        addView(LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20, 18, 20, 18)
            background = Ui.roundedBg(Color.WHITE, Ui.lightBorder, 18f)
            elevation = 2f
            setOnClickListener { action() }

            val badgeView = TextView(activity).apply {
                text = badge
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = Ui.roundedBg(Ui.red, Ui.red, 14f)
            }
            addView(badgeView, LinearLayout.LayoutParams(86, 62).apply { rightMargin = 18 })

            addView(LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(activity).apply {
                    text = title
                    textSize = 17f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.rgb(32, 36, 40))
                })
                addView(TextView(activity).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(Color.rgb(95, 99, 104))
                    setPadding(0, 6, 0, 0)
                })
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = 16
        })
    }
}
