package com.sinopec.formatoshsecampo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

/**
 * Área sencilla para firma con el dedo.
 * Devuelve PNG en Base64 para guardarse dentro del JSON cifrado y dibujarse en el PDF.
 */
class SignaturePadView(context: Context) : View(context) {
    private val path = Path()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 35, 120)
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(170, 170, 170)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val hintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(130, 130, 130)
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    var hasSignature: Boolean = false
        private set

    init {
        setBackgroundColor(Color.WHITE)
        minimumHeight = 210
        setPadding(8, 8, 8, 8)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 220
        val height = resolveSize(desiredHeight, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(2f, 2f, width - 2f, height - 2f, borderPaint)
        if (!hasSignature) {
            canvas.drawText("Firma aquí", width / 2f, height / 2f + 10f, hintPaint)
        }
        canvas.drawPath(path, strokePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(event.x, event.y)
                hasSignature = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(event.x, event.y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }

    fun clear() {
        path.reset()
        hasSignature = false
        invalidate()
    }

    fun toPngBase64(): String {
        if (!hasSignature || width <= 0 || height <= 0) return ""
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawPath(path, strokePaint)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
