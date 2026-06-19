package com.sinopec.formatoshsecampo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sinopec.formatoshsecampo.core.photo.PhotoAttachmentPanel
import com.sinopec.formatoshsecampo.core.version.VersionService
import com.sinopec.formatoshsecampo.features.home.HomeScreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity principal.
 * Maneja cámara/galería una sola vez y cualquier formato puede registrar su PhotoAttachmentPanel activo.
 */
class MainActivity : Activity() {
    private var activePhotoPanel: PhotoAttachmentPanel? = null
    private var currentCameraUri: Uri? = null

    companion object {
        private const val REQ_GALLERY = 200
        private const val REQ_CAMERA = 201
        private const val REQ_CAMERA_PERMISSION = 202
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VersionService().check { allowed, message ->
            runOnUiThread {
                if (allowed) setContentView(HomeScreen(this).build()) else showBlocked(message)
            }
        }
    }

    fun bindPhotoPanel(panel: PhotoAttachmentPanel) {
        activePhotoPanel = panel
        panel.requestGallery = { openGallery() }
        panel.requestCamera = { requestCamera() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQ_GALLERY)
    }

    private fun requestCamera() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQ_CAMERA_PERMISSION)
            return
        }
        openCamera()
    }

    private fun openCamera() {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FormatoHSECampo").apply { mkdirs() }
        val name = "HSE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
        val file = File(dir, name)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        currentCameraUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        startActivityForResult(intent, REQ_CAMERA)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val panel = activePhotoPanel ?: return
        if (requestCode == REQ_CAMERA) {
            currentCameraUri?.let { panel.add(it, "camera") }
            currentCameraUri = null
        }
        if (requestCode == REQ_GALLERY && data != null) {
            data.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) panel.add(clip.getItemAt(i).uri, "gallery")
            } ?: data.data?.let { panel.add(it, "gallery") }
        }
    }

    private fun showBlocked(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Versión no vigente")
            .setMessage(message.ifBlank { "Esta versión ya no está vigente." })
            .setCancelable(false)
            .setPositiveButton("Cerrar") { _, _ -> finish() }
            .show()
    }
}
