package com.sinopec.formatoshsecampo.core.photo

import android.net.Uri

/** Foto anexada a un reporte. */
data class PhotoItem(
    val uri: Uri,
    val source: String,
    val createdAtDevice: String
)
