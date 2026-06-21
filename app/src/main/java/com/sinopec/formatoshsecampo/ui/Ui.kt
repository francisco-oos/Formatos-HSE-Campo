package com.sinopec.formatoshsecampo.ui

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.*

object Ui {
    val red: Int = Color.rgb(181, 30, 46)
    val lightBorder: Int = Color.rgb(224, 226, 230)

    fun roundedBg(fill: Int, stroke: Int = fill, radius: Float = 12f): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = radius
        setStroke(1, stroke)
    }

    fun root(activity: Activity): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(28, 28, 28, 230)
    }

    fun title(activity: Activity, text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 24f
        setTypeface(null, Typeface.BOLD)
        setTextColor(red)
        setPadding(0, 0, 0, 10)
    }

    fun section(activity: Activity, text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 16f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.WHITE)
        setBackgroundColor(red)
        setPadding(12, 10, 12, 10)
    }

    fun label(activity: Activity, text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.DKGRAY)
        setPadding(0, 12, 0, 4)
    }

    fun input(activity: Activity, hint: String, lines: Int = 1): EditText = EditText(activity).apply {
        this.hint = hint
        minLines = lines
        if (lines == 1) setSingleLine(true)
    }

    fun button(activity: Activity, text: String, onClick: () -> Unit): Button = Button(activity).apply {
        this.text = text
        setOnClickListener { onClick() }
    }

    fun spinner(activity: Activity, items: List<String>): Spinner = Spinner(activity).apply {
        adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, items)
    }

    fun scroll(activity: Activity, content: LinearLayout): ScrollView = ScrollView(activity).apply {
        // Evita que el teclado o la barra de navegación tapen los campos y el botón final.
        isFillViewport = false
        clipToPadding = false
        setPadding(0, 0, 0, 180)
        addView(content)
    }

    fun radioRow(activity: Activity, options: List<String>, defaultIndex: Int = 0): RadioGroup {
        return RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
            options.forEachIndexed { index, option ->
                addView(RadioButton(activity).apply {
                    text = option
                    id = View.generateViewId()
                    isChecked = index == defaultIndex
                })
            }
        }
    }
}
