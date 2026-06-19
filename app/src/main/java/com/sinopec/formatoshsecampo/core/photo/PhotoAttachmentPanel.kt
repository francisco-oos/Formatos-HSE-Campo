package com.sinopec.formatoshsecampo.core.photo

import android.app.Activity
import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Panel visual de fotos tipo WhatsApp: botón, menú cámara/galería, miniaturas y quitar.
 * MainActivity solo enruta los resultados; este panel conserva la lista de fotos del formato activo.
 */
class PhotoAttachmentPanel(private val activity: Activity) {
    val photos = mutableListOf<PhotoItem>()
    val view: LinearLayout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private val counter = TextView(activity)
    private val thumbnails = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
    var requestCamera: (() -> Unit)? = null
    var requestGallery: (() -> Unit)? = null

    init {
        val btn = Button(activity).apply {
            text = "Agregar foto"
            setOnClickListener { showMenu() }
        }
        view.addView(btn)
        view.addView(counter)
        view.addView(HorizontalScrollView(activity).apply { addView(thumbnails) })
        refresh()
    }

    private fun showMenu() {
        AlertDialog.Builder(activity)
            .setTitle("Agregar evidencia")
            .setItems(arrayOf("Tomar foto", "Elegir de galería")) { _, which ->
                if (which == 0) requestCamera?.invoke() else requestGallery?.invoke()
            }
            .show()
    }

    fun add(uri: Uri, source: String) {
        if (photos.size >= PhotoQuality.MAX_PHOTOS_PER_REPORT) {
            Toast.makeText(activity, "Límite de fotos alcanzado", Toast.LENGTH_SHORT).show()
            return
        }
        photos.add(PhotoItem(uri, source, SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())))
        refresh()
    }

    fun refresh() {
        counter.text = "Fotos anexas: ${photos.size} · calidad ${PhotoQuality.JPEG_QUALITY}%"
        thumbnails.removeAllViews()
        photos.forEachIndexed { index, item ->
            val box = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
            }
            val img = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(170, 170)
                scaleType = ImageView.ScaleType.CENTER_CROP
                try { setImageURI(item.uri) } catch (_: Exception) {}
            }
            val remove = Button(activity).apply {
                text = "Quitar"
                setOnClickListener { photos.removeAt(index); refresh() }
            }
            box.addView(img)
            box.addView(remove)
            thumbnails.addView(box)
        }
    }
}
