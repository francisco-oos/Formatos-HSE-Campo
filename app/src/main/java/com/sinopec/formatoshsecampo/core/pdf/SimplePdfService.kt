package com.sinopec.formatoshsecampo.core.pdf

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sinopec.formatoshsecampo.core.crypto.CryptoService
import com.sinopec.formatoshsecampo.core.photo.PhotoItem
import com.sinopec.formatoshsecampo.core.photo.PhotoManager
import com.sinopec.formatoshsecampo.domain.HseFormat
import com.sinopec.formatoshsecampo.domain.HseReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale

/**
 * Servicio central para generar y compartir PDFs.
 *
 * IMPORTANTE:
 * - El PDF es la evidencia visual que se comparte en campo.
 * - El JSON cifrado es la evidencia estructurada que después podrá leer el programa de PC.
 * - Cada formato puede tener su propio render visual, pero todos pasan por el mismo flujo:
 *   PDF visible + fotos + JSON cifrado AES-256-GCM embebido.
 */
class SimplePdfService(private val activity: Activity) {

    fun createBasicReportPdf(report: HseReport, visibleLines: List<String>, photos: List<PhotoItem>): File {
        val dir = File(activity.getExternalFilesDir(null), "FormatoHSECampo").apply { mkdirs() }
        val safeFolio = report.folio.replace(Regex("[^A-Za-z0-9_-]+"), "_")
        val file = File(dir, "${report.format.code}_$safeFolio.pdf")
        val doc = PdfDocument()

        // El JSON se obtiene una sola vez para que lo visible y lo embebido correspondan al mismo reporte.
        val json = report.toJson()

        when (report.format) {
            HseFormat.SUPERVISION_DIARIA -> addSupervisionSeguraPage(doc, json, report.folio)
            HseFormat.TARJETA_OBSERVACION -> addTarjetaObservacionPages(doc, json, report.folio)
            HseFormat.SUPERVISION_WORD -> addListaChequeoSupervisionPage(doc, json, report.folio)
            HseFormat.INSPECCION_CHALECO -> addGenericPage(doc, report, visibleLines, json)
        }

        addPhotoPages(doc, dir, photos)

        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        // Mantiene el JSON cifrado embebido para compatibilidad con el lector de escritorio.
        val encrypted = CryptoService.encryptJson(json)
        PdfPayloadEmbedder.appendEncryptedPayload(file, encrypted)
        return file
    }

    fun sharePdf(file: File) {
        val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            // No se fuerza WhatsApp para que también aparezca WhatsApp Business, correo, Drive, etc.
        }
        activity.startActivity(Intent.createChooser(send, "Compartir formato HSE"))
    }

    // -------------------------------------------------------------------------
    // SUPERVISIÓN SEGURA - Render visual migrado desde la app estable anterior.
    // -------------------------------------------------------------------------

    private fun addSupervisionSeguraPage(doc: PdfDocument, json: JSONObject, folio: String) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val bitmap = renderSupervisionSeguraAsBitmap(json, folio)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        doc.finishPage(page)
    }

    private fun renderSupervisionSeguraAsBitmap(json: JSONObject, folio: String): Bitmap {
        val page = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(page)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.WHITE)

        val left = 35f
        val top = 18f
        val right = 560f
        val bottom = 822f
        val red = Color.rgb(225, 0, 0)
        val blue = Color.rgb(35, 55, 165)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = Color.BLACK
        canvas.drawRect(left, top, right, bottom, p)

        drawSupervisionHeader(canvas, p, json, left, top, right)

        val personalY = top + 100f
        drawRedSection(canvas, p, "PERSONAL", left, personalY, right, red)

        val personalQuestions = listOf(
            "¿HOY ME ENCUENTRO BIEN?",
            "¿TENGO CASCO?",
            "¿MI OVEROL ESTA EN BUEN ESTADO?",
            "¿MIS BOTAS ESTAN EN BUEN ESTADO?",
            "¿MIS POLAINAS ESTAN EN BUEN ESTADO?",
            "¿MI CHALECO ESTA EN BUEN ESTADO?",
            "¿CUENTO CON GUANTES?",
            "¿CUENTO CON LENTES?",
            "¿CUENTO CON BARBIQUEJO?",
            "¿MIS COMPAÑEROS SE ENCUENTRAN BIEN?",
            "¿YO ESTOY LISTO PARA IR A TRABAJAR?"
        )

        drawSupervisionChecklistBlock(
            canvas, p, json, 0, personalQuestions,
            x = left + 7f, y = personalY + 42f, rowH = 23f,
            boxX = right - 148f, boxW = 43f, boxH = 23f, textSize = 10.8f, blue = blue
        )

        val toolsY = personalY + 42f + personalQuestions.size * 23f + 8f
        drawRedSection(canvas, p, "EQUIPOS Y HERRAMIENTAS", left, toolsY, right, red)

        val toolsQuestions = listOf(
            "¿RECIBI MI EQUIPO Y HERRAMIENTA\nEN BUEN ESTADO?",
            "¿TENGO IDENTIFICADOS LOS RIESGOS A LOS QUE\nESTOY EXPUESTO?",
            "¿SE QUE HACER EN CASO DE UNA EMERGENCIA?"
        )

        drawSupervisionChecklistBlock(
            canvas, p, json, 11, toolsQuestions,
            x = left + 7f, y = toolsY + 42f, rowH = 41f,
            boxX = right - 148f, boxW = 43f, boxH = 41f, textSize = 10.0f, blue = blue
        )

        val jobsY = toolsY + 42f + toolsQuestions.size * 41f + 8f
        drawRedSection(canvas, p, "TRABAJOS A REALIZAR", left, jobsY, right, red)

        val jobQuestions = listOf(
            "¿SE PLANEARON LAS ACTIVIDADES CON LA\nIDENTIFICACION DE RIESGOS?",
            "¿SE TIENEN LOS PROCEDIMIENTOS O\nINSTRUCTIVOS DE TRABAJO?"
        )

        drawSupervisionChecklistBlock(
            canvas, p, json, 14, jobQuestions,
            x = left + 7f, y = jobsY + 42f, rowH = 39f,
            boxX = right - 148f, boxW = 43f, boxH = 39f, textSize = 10.0f, blue = blue
        )

        val hydrationY = jobsY + 42f + jobQuestions.size * 39f + 7f
        p.style = Paint.Style.FILL
        p.color = red
        canvas.drawRect(left, hydrationY, right, hydrationY + 42f, p)

        p.color = Color.WHITE
        p.textSize = 12.7f
        p.typeface = Typeface.DEFAULT_BOLD
        drawCenteredText(canvas, p, "PRIMERO QUE TODO TU SEGURIDAD LLEVA AGUA Y SUERO PARA", left, right, hydrationY + 17f)
        drawCenteredText(canvas, p, "MANTENERTE HIDRATADO", left, right, hydrationY + 34f)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.BLACK
        canvas.drawRect(left, hydrationY, right, hydrationY + 42f, p)

        val commentTop = hydrationY + 42f
        drawSupervisionCommentsBox(canvas, p, json, folio, left, commentTop, right, bottom, blue)

        return page
    }

    private fun drawSupervisionHeader(canvas: Canvas, p: Paint, json: JSONObject, left: Float, top: Float, right: Float) {
        val red = Color.rgb(225, 0, 0)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = Color.BLACK

        val headerBottom = top + 100f
        val logoRight = left + 155f
        val titleRight = right - 165f
        canvas.drawRect(left, top, right, headerBottom, p)
        canvas.drawLine(logoRight, top, logoRight, headerBottom, p)
        canvas.drawLine(titleRight, top, titleRight, top + 72f, p)
        canvas.drawLine(titleRight, top + 36f, right, top + 36f, p)
        canvas.drawLine(logoRight, top + 72f, right, top + 72f, p)

        val logo = loadLogoBitmap()
        if (logo != null) {
            canvas.drawBitmap(logo, null, RectF(left + 8f, top + 8f, logoRight - 8f, top + 66f), null)
        } else {
            p.style = Paint.Style.FILL
            p.color = red
            p.textSize = 21f
            p.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("SINOPEC", left + 22f, top + 44f, p)
        }

        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.textSize = 21f
        p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Supervisión Segura", logoRight + 8f, top + 44f, p)

        p.textSize = 10.5f
        canvas.drawText("FECHA:", titleRight + 8f, top + 17f, p)
        canvas.drawText(fechaHoraVisible(json), titleRight + 70f, top + 17f, p)
        canvas.drawText("VOLANTE:", titleRight + 8f, top + 53f, p)
        canvas.drawText(jsonGeneral(json, "volante").uppercase(Locale.US).take(14), titleRight + 75f, top + 53f, p)

        p.textSize = 10.2f
        canvas.drawText("CATEGORIA:", logoRight + 8f, top + 88f, p)
        canvas.drawText(jsonGeneral(json, "categoria").uppercase(Locale("es", "MX")).take(18), logoRight + 90f, top + 88f, p)

        canvas.drawText("NOMBRE:", logoRight + 8f, top + 99f, p)
        canvas.drawText(jsonGeneral(json, "nombre").uppercase(Locale("es", "MX")).take(24), logoRight + 75f, top + 99f, p)

        canvas.drawText("ID:", titleRight + 8f, top + 88f, p)
        canvas.drawText(jsonGeneral(json, "id_empleado").uppercase(Locale.US).take(18), titleRight + 35f, top + 88f, p)
    }

    private fun drawRedSection(canvas: Canvas, p: Paint, title: String, left: Float, y: Float, right: Float, red: Int) {
        p.style = Paint.Style.FILL
        p.color = red
        canvas.drawRect(left, y, right, y + 22f, p)
        p.color = Color.WHITE
        p.textSize = 13f
        p.typeface = Typeface.DEFAULT_BOLD
        drawCenteredText(canvas, p, title, left, right, y + 16f)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.BLACK
        canvas.drawRect(left, y, right, y + 22f, p)
    }

    private fun drawSupervisionChecklistBlock(
        canvas: Canvas,
        p: Paint,
        json: JSONObject,
        startIndex: Int,
        questions: List<String>,
        x: Float,
        y: Float,
        rowH: Float,
        boxX: Float,
        boxW: Float,
        boxH: Float,
        textSize: Float,
        blue: Int
    ) {
        val arr = json.optJSONArray("checklist") ?: JSONArray()

        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.textSize = 9.4f
        p.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("SI", boxX + 14f, y - 7f, p)
        canvas.drawText("NO", boxX + boxW + 10f, y - 7f, p)
        canvas.drawText("CAMBIO", boxX + boxW * 2f + 1f, y - 7f, p)

        questions.forEachIndexed { idx, question ->
            val rowTop = y + idx * rowH
            val lines = question.split("\n")
            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            p.textSize = textSize
            p.typeface = Typeface.DEFAULT_BOLD
            var textY = rowTop + if (lines.size > 1) 13f else 15.5f
            lines.forEach {
                canvas.drawText(it, x, textY, p)
                textY += 12f
            }

            p.style = Paint.Style.STROKE
            p.strokeWidth = 1.1f
            p.color = Color.BLACK
            for (c in 0..2) {
                canvas.drawRect(boxX + c * boxW, rowTop, boxX + (c + 1) * boxW, rowTop + boxH, p)
            }

            val globalIndex = startIndex + idx
            val respuesta = if (globalIndex < arr.length()) {
                arr.optJSONObject(globalIndex)?.optString("respuesta", "SI") ?: "SI"
            } else "SI"

            p.style = Paint.Style.FILL
            p.color = blue
            p.textSize = if (boxH <= 24f) 22f else 25f
            p.typeface = Typeface.DEFAULT_BOLD

            when (respuesta.uppercase(Locale.US)) {
                "SI" -> canvas.drawText("✓", boxX + 12f, rowTop + boxH - 5f, p)
                "NO" -> canvas.drawText("X", boxX + boxW + 12f, rowTop + boxH - 7f, p)
                "CAMBIO" -> canvas.drawText("X", boxX + boxW * 2f + 12f, rowTop + boxH - 7f, p)
                else -> canvas.drawText("✓", boxX + 12f, rowTop + boxH - 5f, p)
            }
        }
    }

    private fun drawSupervisionCommentsBox(canvas: Canvas, p: Paint, json: JSONObject, folio: String, left: Float, top: Float, right: Float, bottom: Float, blue: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.BLACK
        canvas.drawRect(left, top, right, bottom, p)

        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.textSize = 10.5f
        p.typeface = Typeface.DEFAULT
        canvas.drawText("COMENTARIOS", left + 7f, top + 14f, p)

        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.45f
        p.color = Color.rgb(210, 210, 210)
        var lineY = top + 30f
        while (lineY < bottom - 9f) {
            canvas.drawLine(left + 5f, lineY, right - 5f, lineY, p)
            lineY += 18f
        }

        val qrX = right - 49f
        val qrY = top + 19f
        drawFolioQr(canvas, p, folio, qrX, qrY)

        p.style = Paint.Style.FILL
        p.color = blue
        p.textSize = 12.5f
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val commentLines = wrapText(jsonText(json, "comentarios"), 46).take(3)
        var cy = top + 32f
        commentLines.forEach { line ->
            canvas.drawText(line, left + 10f, cy, p)
            cy += 18f
        }
    }



    // -------------------------------------------------------------------------
    // TARJETA DE OBSERVACIÓN DE SEGURIDAD - Render visual de dos caras.
    // -------------------------------------------------------------------------

    private fun addTarjetaObservacionPages(doc: PdfDocument, json: JSONObject, folio: String) {
        val front = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val frontBitmap = renderTosFrontAsBitmap(json, folio)
        front.canvas.drawBitmap(frontBitmap, 0f, 0f, null)
        doc.finishPage(front)

        val back = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 2).create())
        val backBitmap = renderTosBackAsBitmap(json, folio)
        back.canvas.drawBitmap(backBitmap, 0f, 0f, null)
        doc.finishPage(back)
    }

    private fun renderTosFrontAsBitmap(json: JSONObject, folio: String): Bitmap {
        val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        val red = Color.rgb(190, 24, 35)
        val blue = Color.rgb(0, 118, 172)
        val ink = Color.rgb(35, 55, 165)
        val left = 58f
        val top = 32f
        val right = 537f

        drawTosHeader(canvas, p, left, top, right)

        val general = json.optJSONObject("datos_generales") ?: JSONObject()
        val tipoSeleccionado = general.optString("tipo_observacion")

        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.textSize = 10.5f
        p.typeface = Typeface.DEFAULT

        // Fecha y hora visibles del momento en que se genera la observación.
        // Se dibujan debajo del encabezado para evitar traslapes con el título.
        val fechaHoraY = 153f
        canvas.drawText("Fecha:", left, fechaHoraY, p)
        drawHandText(canvas, p, json.optString("fecha"), left + 45f, fechaHoraY + 1f, 125f, ink)
        canvas.drawText("Hora:", right - 170f, fechaHoraY, p)
        drawHandText(canvas, p, formatHoraAmPm(json.optString("hora")), right - 118f, fechaHoraY + 1f, 110f, ink)

        drawTosRedBar(canvas, p, "TIPO DE OBSERVACIÓN", left, 171f, right, red)

        val tiposLeft = listOf("Acto Inseguro", "Condición Insegura", "Casi Accidente", "Observación de Seguridad Realizada")
        val tiposRight = listOf("Acción Positiva", "Oportunidad de Mejora", "Intervención de Seguridad")
        var y = 194f
        tiposLeft.forEach { label ->
            drawSquareOption(canvas, p, left + 6f, y - 10f, label, label == tipoSeleccionado, blue, ink)
            y += 19f
        }
        y = 194f
        tiposRight.forEach { label ->
            drawSquareOption(canvas, p, 315f, y - 10f, label, label == tipoSeleccionado, blue, ink)
            y += 19f
        }

        canvas.drawText("Área/Departamento", left, 276f, p)
        drawHandText(canvas, p, general.optString("area_departamento"), left + 128f, 277f, 344f, ink)
        canvas.drawText("Actividad realizada:", left, 301f, p)
        drawHandText(canvas, p, general.optString("actividad_realizada"), left + 128f, 302f, 344f, ink)

        drawTosRedBar(canvas, p, "ACCIONES DE SEGURIDAD Y OBSERVACIONES", left, 318f, right, red)

        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 10f
        canvas.drawText("Seguro", 292f, 349f, p)
        canvas.drawText("Inseguro", 382f, 349f, p)
        canvas.drawText("No Aplica", 485f, 349f, p)

        val items = listOf(
            Pair("Conducta Personal", ""),
            Pair("Uso del EPP", "Conducta Personal"),
            Pair("Ojos en la Tarea", "Conducta Personal"),
            Pair("Posición de trabajo segura\n(línea de fuego)", "Conducta Personal"),
            Pair("Alzando/jalando/empujando/cargando", "Conducta Personal"),
            Pair("Ambiente de Trabajo", ""),
            Pair("Orden y Limpieza", "Ambiente de Trabajo"),
            Pair("Iluminación", "Ambiente de Trabajo"),
            Pair("Ventilación", "Ambiente de Trabajo"),
            Pair("Superficie Nivelada", "Ambiente de Trabajo"),
            Pair("Equipo/Herramientas", ""),
            Pair("Adecuadas para la tarea", "Equipo/Herramientas"),
            Pair("Bloqueos/Aislamientos", "Equipo/Herramientas"),
            Pair("Arnés/Línea de vida", "Equipo/Herramientas"),
            Pair("Procedimientos", ""),
            Pair("Análisis de Seguridad del\nTrabajo", "Procedimientos"),
            Pair("Permiso de Trabajo", "Procedimientos"),
            Pair("Instructivo de Trabajo", "Procedimientos"),
            Pair("Respuesta a Emergencias", "Procedimientos")
        )

        val checklist = json.optJSONArray("checklist") ?: JSONArray()
        var rowY = 368f
        items.forEachIndexed { idx, pair ->
            val label = pair.first
            val isGroup = pair.second.isBlank()
            if (isGroup && idx != 0) {
                p.style = Paint.Style.FILL
                p.color = red
                canvas.drawRect(left, rowY - 14f, right, rowY - 10f, p)
                rowY += 7f
            }

            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            p.typeface = if (isGroup) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            p.textSize = if (isGroup) 11.5f else 10.7f

            val lines = label.split("\n")
            lines.forEachIndexed { lineIdx, line ->
                canvas.drawText(line, left + 7f, rowY + (lineIdx * 12f), p)
            }

            if (!isGroup) {
                val answer = findTosAnswer(checklist, label.replace("\n", " "))
                drawTosAnswerBoxes(canvas, p, 300f, rowY - 14f, answer, blue, ink)
            }
            rowY += if (lines.size > 1) 25f else 20f
        }

        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT
        p.textSize = 9.5f
        val note = "Observe detenidamente la actividad que se esta realizando, tenga en cuenta: La posición de las personas, el uso de las herramientas y/o equipos, la limpieza y orden del área, las reacciones de las personas, el uso del EPP"
        var noteY = 770f
        wrapText(note, 82).take(3).forEach {
            canvas.drawText(it, left + 4f, noteY, p)
            noteY += 13f
        }

        // QR discreto: solo folio, no contiene JSON.
        drawFolioQr(canvas, p, folio, right - 42f, 792f)
        return bmp
    }

    private fun renderTosBackAsBitmap(json: JSONObject, folio: String): Bitmap {
        val bmp = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(Color.WHITE)

        val red = Color.rgb(190, 24, 35)
        val ink = Color.rgb(35, 55, 165)
        val left = 58f
        val top = 32f
        val right = 537f
        drawTosHeader(canvas, p, left, top, right)

        val obs = json.optJSONObject("observaciones") ?: JSONObject()
        val acciones = json.optJSONObject("acciones") ?: JSONObject()
        val reporta = json.optJSONObject("datos_reportante") ?: json.optJSONObject("reporta") ?: JSONObject()

        var y = 168f
        drawLinedTextArea(canvas, p, "Razón de la Observación", obs.optString("razon"), left, y, right, 3, ink)
        y += 118f
        drawLinedTextArea(canvas, p, "Detalles de la Observación", obs.optString("detalles"), left, y, right, 3, ink)
        y += 118f
        drawLinedTextArea(canvas, p, "Acciones Tomadas/Recomendaciones", acciones.optString("acciones_tomadas_recomendaciones"), left, y, right, 3, ink)

        y += 135f
        val punto = acciones.opt("punto_accion_generado").toString().equals("true", true) || acciones.optString("punto_accion_generado").equals("Sí", true)
        drawCheckBox(canvas, p, left + 4f, y - 12f, 14f, punto, Color.rgb(0, 118, 172), ink)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT
        p.textSize = 12f
        canvas.drawText("Punto de Acción Generado?", left + 26f, y, p)

        y += 34f
        canvas.drawText("Acción Asignada a:", left, y, p)
        drawHandText(canvas, p, acciones.optString("accion_asignada_a"), left + 115f, y + 1f, right - left - 120f, ink)

        y += 36f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 10.7f
        canvas.drawText("Datos de quien realiza la observación/Reporte", left, y, p)
        p.typeface = Typeface.DEFAULT
        p.textSize = 12f
        y += 24f
        canvas.drawText("Nombre:", left, y, p)
        drawHandText(canvas, p, reporta.optString("nombre"), left + 70f, y + 1f, right - left - 78f, ink)
        y += 24f
        canvas.drawText("Departamento:", left, y, p)
        drawHandText(canvas, p, reporta.optString("departamento"), left + 105f, y + 1f, right - left - 112f, ink)
        y += 24f
        canvas.drawText("Puesto de Trabajo:", left, y, p)
        drawHandText(canvas, p, reporta.optString("puesto_trabajo"), left + 132f, y + 1f, right - left - 140f, ink)

        // Bloque inferior PARE / PIENSE / ACTÚE.
        val baseY = 722f
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 19f
        p.color = red
        canvas.drawText("PARE", left + 20f, baseY, p)
        p.color = Color.rgb(230, 180, 0)
        canvas.drawText("PIENSE", left + 20f, baseY + 37f, p)
        p.color = Color.rgb(0, 125, 62)
        canvas.drawText("ACTÚE", left + 20f, baseY + 74f, p)

        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT
        p.textSize = 10.5f
        val pare = listOf(
            "Toma un momento para detectar conductas y/o situaciones de alerta",
            "Evalúa las decisiones y cambios que implementarás, maneja los sentimientos y toma control de la situación",
            "Exprésate y Actúa asertivamente ante la situación, habiendo evaluado alternativas, puedes tomar la decisión"
        )
        var textY = baseY - 4f
        pare.forEach { line ->
            wrapText(line, 58).take(2).forEach {
                canvas.drawText(it, left + 112f, textY, p)
                textY += 13f
            }
            textY += 6f
        }

        p.style = Paint.Style.FILL
        p.color = red
        canvas.drawRect(left, 790f, right, 817f, p)
        p.color = Color.WHITE
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        p.textSize = 14f
        drawCenteredText(canvas, p, "La seguridad no es negociable y depende de mi", left, right, 808f)

        drawFolioQr(canvas, p, folio, right - 42f, 742f)
        return bmp
    }

    private fun drawTosHeader(canvas: Canvas, p: Paint, left: Float, top: Float, right: Float) {
        // Encabezado de TOS con la misma estructura visual del formato físico:
        // logo | Sistema de Gestión de HSE | revisión/fecha y una fila inferior para el título.
        val headerBottom = top + 108f
        val logoRight = left + 165f
        val titleRight = right - 130f
        val titleRowTop = top + 72f

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.2f
        p.color = Color.BLACK
        canvas.drawRect(left, top, right, headerBottom, p)
        canvas.drawLine(logoRight, top, logoRight, headerBottom, p)
        canvas.drawLine(titleRight, top, titleRight, titleRowTop, p)
        canvas.drawLine(titleRight, top + 30f, right, top + 30f, p)
        canvas.drawLine(titleRight, top + 60f, right, top + 60f, p)
        canvas.drawLine(left, titleRowTop, right, titleRowTop, p)

        val logo = loadLogoBitmap()
        if (logo != null) {
            canvas.drawBitmap(logo, null, RectF(left + 8f, top + 8f, logoRight - 8f, titleRowTop - 7f), null)
        } else {
            p.style = Paint.Style.FILL
            p.color = Color.rgb(190, 24, 35)
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 24f
            canvas.drawText("SINOPEC", left + 38f, top + 43f, p)
        }

        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 19f
        drawCenteredText(canvas, p, "Sistema de", logoRight, titleRight, top + 28f)
        drawCenteredText(canvas, p, "Gestión de HSE", logoRight, titleRight, top + 55f)

        p.textSize = 14.2f
        drawCenteredText(canvas, p, "TARJETA DE OBSERVACIÓN DE SEGURIDAD", left, right, top + 92f)

        p.typeface = Typeface.DEFAULT
        p.textSize = 9.5f
        canvas.drawText("Revisión: 02", titleRight + 8f, top + 20f, p)
        canvas.drawText("Fecha:30/05/23", titleRight + 8f, top + 50f, p)
    }

    private fun drawTosRedBar(canvas: Canvas, p: Paint, text: String, left: Float, y: Float, right: Float, red: Int) {
        p.style = Paint.Style.FILL
        p.color = red
        canvas.drawRect(left, y, right, y + 18f, p)
        p.color = Color.WHITE
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 14f
        drawCenteredText(canvas, p, text, left, right, y + 14f)
    }

    private fun drawSquareOption(canvas: Canvas, p: Paint, x: Float, y: Float, label: String, checked: Boolean, blue: Int, ink: Int) {
        drawCheckBox(canvas, p, x, y, 14f, checked, blue, ink)
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT
        p.textSize = 10.5f
        canvas.drawText(label, x + 22f, y + 12f, p)
    }

    private fun drawTosAnswerBoxes(canvas: Canvas, p: Paint, x: Float, y: Float, respuesta: String, blue: Int, ink: Int) {
        val size = 15f
        val gap = 82f
        drawCheckBox(canvas, p, x, y, size, respuesta.equals("Seguro", true), blue, ink)
        drawCheckBox(canvas, p, x + gap, y, size, respuesta.equals("Inseguro", true), blue, ink)
        drawCheckBox(canvas, p, x + gap * 2f, y, size, respuesta.equals("No Aplica", true), blue, ink)
    }

    private fun drawCheckBox(canvas: Canvas, p: Paint, x: Float, y: Float, size: Float, checked: Boolean, blue: Int, ink: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.4f
        p.color = blue
        canvas.drawRect(x, y, x + size, y + size, p)
        if (checked) {
            p.style = Paint.Style.FILL
            p.color = ink
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = size + 8f
            canvas.drawText("✓", x + 1f, y + size + 2f, p)
        }
    }

    private fun drawHandText(canvas: Canvas, p: Paint, text: String, x: Float, baseline: Float, width: Float, ink: Int) {
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.rgb(150, 150, 150)
        canvas.drawLine(x, baseline + 3f, x + width, baseline + 3f, p)

        val cleanText = text.trim().take(48)
        p.style = Paint.Style.FILL
        p.color = ink
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        p.textSize = when {
            cleanText.length > 32 -> 12.8f
            cleanText.length > 22 -> 13.8f
            else -> 15f
        }

        // Centra el valor capturado dentro de su línea sin salirse de la celda.
        val measured = p.measureText(cleanText)
        val textX = if (measured < width - 6f) {
            x + ((width - measured) / 2f)
        } else {
            x + 3f
        }
        canvas.drawText(cleanText, textX, baseline, p)
    }

    /**
     * Fecha visible para PDF.
     * Mantiene el JSON intacto; solo cambia cómo se muestra en el documento.
     * Ejemplo: 2026-06-19 12:20 AM
     */
    private fun fechaHoraVisible(json: JSONObject): String {
        val fecha = json.optString("fecha", "").trim()
        val hora = formatHoraAmPm(json.optString("hora", ""))
        return listOf(fecha, hora).filter { it.isNotBlank() }.joinToString(" ")
    }

    private fun formatHoraAmPm(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""
        val parts = clean.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return clean.take(8)
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val suffix = if (hour >= 12) "PM" else "AM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%02d:%02d %s".format(hour12, minute, suffix)
    }

    private fun drawLinedTextArea(canvas: Canvas, p: Paint, label: String, text: String, left: Float, y: Float, right: Float, lines: Int, ink: Int) {
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 10.5f
        canvas.drawText(label, left, y, p)

        // La primera línea comienza después de la etiqueta.
        // En "Acciones Tomadas/Recomendaciones" la etiqueta es más larga,
        // por eso se reserva más espacio para evitar que el texto azul se monte encima.
        val labelOffset = if (label.length > 28) 182f else 125f
        val firstTextX = left + labelOffset + 6f

        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.rgb(80, 80, 80)
        var lineY = y + 4f
        repeat(lines) {
            canvas.drawLine(firstTextX, lineY, right, lineY, p)
            lineY += 28f
            canvas.drawLine(left, lineY, right, lineY, p)
        }

        p.style = Paint.Style.FILL
        p.color = ink
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        p.textSize = 17f
        var ty = y - 2f
        val firstLineChars = if (label.length > 28) 34 else 44
        wrapText(text, firstLineChars).take(lines + 1).forEachIndexed { idx, line ->
            canvas.drawText(line, if (idx == 0) firstTextX + 3f else left + 5f, ty, p)
            ty += 28f
        }
    }

    private fun findTosAnswer(checklist: JSONArray, label: String): String {
        val cleanLabel = label.uppercase(Locale("es", "MX"))
        for (i in 0 until checklist.length()) {
            val obj = checklist.optJSONObject(i) ?: continue
            val point = obj.optString("punto").replace("\n", " ").uppercase(Locale("es", "MX"))
            if (point == cleanLabel || point.contains(cleanLabel) || cleanLabel.contains(point)) {
                return obj.optString("respuesta", "No Aplica")
            }
        }
        return "No Aplica"
    }



    // -------------------------------------------------------------------------
    // LISTA DE CHEQUEO SUPERVISIÓN SEGURA SSM-HSE-F-20.
    // Render visual basado en el formato físico proporcionado.
    // -------------------------------------------------------------------------

    private fun addListaChequeoSupervisionPage(doc: PdfDocument, json: JSONObject, folio: String) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val bitmap = renderListaChequeoSupervisionAsBitmap(json, folio)

        // IMPORTANTE:
        // renderListaChequeoSupervisionAsBitmap usa escala interna alta para que el PNG
        // y el texto salgan nítidos. Aquí lo reducimos al tamaño real de la hoja PDF.
        page.canvas.drawBitmap(
            bitmap,
            null,
            RectF(0f, 0f, 595f, 842f),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        )
        doc.finishPage(page)
    }

    private fun renderListaChequeoSupervisionAsBitmap(json: JSONObject, folio: String): Bitmap {
        // -----------------------------------------------------------------
        // FORMATO SUPERVISIÓN SEGURA / LISTA DE CHEQUEO
        // Estrategia nueva: usar una imagen PNG del formato físico como fondo
        // y solo escribir encima los datos capturados, checks, observaciones y QR.
        // Esto evita estar dibujando todas las líneas/cuadros manualmente y hace
        // más fácil mantener futuros formatos.
        //
        // IMPORTANTE PARA ANDROID:
        // Coloca el PNG en:
        // app/src/main/res/drawable/lista_chequeo_supervision_segura.png
        // También acepta estos nombres alternativos:
        // - supervision_segura_formato.png
        // - supervicion_segura.png
        // - formato_supervision_segura.png
        // -----------------------------------------------------------------
        val scale = 4
        val bmp = Bitmap.createBitmap(595 * scale, 842 * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(scale.toFloat(), scale.toFloat())

        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawColor(Color.WHITE)

        // Carga de plantilla PNG desde drawable.
        val templateNames = listOf(
            "lista_chequeo_supervision_segura",
            "supervision_segura_formato",
            "supervicion_segura",
            "formato_supervision_segura"
        )

        var template: Bitmap? = null
        for (name in templateNames) {
            val resId = activity.resources.getIdentifier(name, "drawable", activity.packageName)
            if (resId != 0) {
                template = BitmapFactory.decodeResource(activity.resources, resId)
                break
            }
        }

        if (template != null) {
            // La plantilla se ajusta a toda la página PDF.
            // Si el PNG viene escaneado ligeramente deformado, este ajuste lo normaliza.
            canvas.drawBitmap(template, null, RectF(0f, 0f, 595f, 842f), p)
        } else {
            // Fallback visible para no generar una página vacía si olvidamos copiar el PNG.
            p.style = Paint.Style.FILL
            p.color = Color.rgb(225, 0, 0)
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 16f
            canvas.drawText("FALTA PLANTILLA PNG: lista_chequeo_supervision_segura.png", 35f, 60f, p)
        }

        val blue = Color.rgb(35, 55, 165)
        val general = json.optJSONObject("datos_generales") ?: JSONObject()
        val checklist = json.optJSONArray("checklist") ?: JSONArray()

        // -----------------------------------------------------------------
        // Datos generales escritos sobre la plantilla.
        // Coordenadas pensadas para la imagen proporcionada del formato físico.
        // Si se quiere afinar, se mueven únicamente estos valores X/Y.
        // -----------------------------------------------------------------
        p.style = Paint.Style.FILL
        p.color = blue
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)

        // Datos generales sobre la plantilla PNG.
        // Se acortan los textos de las primeras celdas para evitar que invadan
        // las etiquetas vecinas cuando el usuario captura valores largos.
        p.textSize = 9.5f
        canvas.drawText(general.optString("brigada").take(10), 62f, 132f, p)
        canvas.drawText(general.optString("proyecto").take(18), 154f, 137f, p)
        canvas.drawText(general.optString("departamento_supervisado").take(26), 382f, 137f, p)

        p.textSize = 10.5f
        canvas.drawText(general.optString("quien_supervisa").take(30), 155f, 177f, p)
        canvas.drawText(general.optString("puesto").take(24), 365f, 177f, p)

        p.textSize = 10.2f
        canvas.drawText(fitText(general.optString("supervisor_trabajo"), p, 170f),205f,218f,p)
        canvas.drawText(fechaHoraVisible(json).take(26), 395f, 218f, p)

        // -----------------------------------------------------------------
        // Checks SI/NO.
        // Se dibuja únicamente la marca sobre las cajas ya existentes del PNG.
        // -----------------------------------------------------------------
        p.color = blue
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 16f

        // Centros visuales de las columnas SI/NO sobre el PNG.
        //
        // IMPORTANTE:
        // Esta plantilla NO tiene separación uniforme en las 20 preguntas,
        // porque entre los grupos hay barras rojas de sección:
        // - Después de la pregunta 6
        // - Después de la pregunta 12
        //
        // Por eso NO usamos una sola fórmula simple i * rowH.
        // Sumamos sectionGap cuando el índice ya pasó una barra roja.
        val siX = 533f
        val noX = 563f

        // Posiciones Y medidas sobre la plantilla PNG real.
        // Cada valor corresponde al baseline visual de la marca para cada pregunta.
        // No usar incremento fijo: las barras rojas de sección agregan separación.
        val checkYs = listOf(
            250f, // 1
            266f, // 2
            282f, // 3
            297f, // 4
            313f, // 5
            329f, // 6

            361f, // 7
            377f, // 8
            393f, // 9
            409f, // 10
            425f, // 11
            440f, // 12

            478f, // 13
            502f, // 14
            522f, // 15
            541f, // 16
            561f, // 17
            581f, // 18
            600f, // 19
            620f  // 20
        )

        for (i in 0 until minOf(checklist.length(), checkYs.size)) {
            val obj = checklist.optJSONObject(i) ?: continue
            val respuesta = obj.optString("respuesta", "")
            val y = checkYs[i]

            when (respuesta.uppercase(Locale.US)) {
                "SI", "SÍ" -> canvas.drawText("✓", siX, y, p)
                "NO" -> canvas.drawText("X", noX, y - 1f, p)
            }
        }

        // -----------------------------------------------------------------
        // Observaciones sobre el área de líneas del formato.
        // -----------------------------------------------------------------
        p.color = blue
        p.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        p.textSize = 11.5f
        var obsY = 666f
        wrapText(json.optString("comentarios"), 78).take(4).forEach { line ->
            canvas.drawText(line, 35f, obsY, p)
            obsY += 18f
        }

        // QR discreto con folio. El JSON cifrado NO va en el QR; va embebido al final del PDF.
        drawFolioQr(canvas, p, folio, 550f, 780f)
        return bmp
    }

    private fun drawListaInfoCell(canvas: Canvas, p: Paint, label: String, value: String, left: Float, top: Float, right: Float, bottom: Float, boldValue: Boolean = false) {
        p.style = Paint.Style.FILL
        p.color = Color.BLACK
        p.typeface = if (boldValue) Typeface.DEFAULT_BOLD else Typeface.DEFAULT_BOLD
        p.textSize = 8.7f
        if (value.isBlank()) {
            drawCenteredText(canvas, p, label, left, right, top + 13f)
        } else {
            canvas.drawText(label, left + 7f, top + 13f, p)
            p.typeface = Typeface.DEFAULT
            canvas.drawText(value, right - 48f, top + 13f, p)
        }
    }

    private fun drawListaSectionHeader(canvas: Canvas, p: Paint, left: Float, y: Float, right: Float, itemW: Float, siX: Float, noX: Float, red: Int, title: String) {
        p.style = Paint.Style.FILL
        p.color = red
        canvas.drawRect(left, y, right, y + 18.5f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 0.8f
        p.color = Color.BLACK
        canvas.drawRect(left, y, right, y + 18.5f, p)
        canvas.drawLine(left + itemW, y, left + itemW, y + 18.5f, p)
        canvas.drawLine(siX, y, siX, y + 18.5f, p)
        canvas.drawLine(noX, y, noX, y + 18.5f, p)

        p.style = Paint.Style.FILL
        p.color = Color.WHITE
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 11f
        canvas.drawText("ITEM", left + 4f, y + 13f, p)
        drawCenteredText(canvas, p, title, left + itemW, siX, y + 13f)
        drawCenteredText(canvas, p, "SI", siX, noX, y + 13f)
        drawCenteredText(canvas, p, "NO", noX, right, y + 13f)
    }

    private fun drawListaQuestionRows(
        canvas: Canvas,
        p: Paint,
        checklist: JSONArray,
        questions: List<String>,
        startIndex: Int,
        startNumber: Int,
        y: Float,
        rowH: Float,
        left: Float,
        right: Float,
        itemW: Float,
        siX: Float,
        noX: Float,
        blue: Int
    ): Float {
        var currentY = y
        questions.forEachIndexed { idx, question ->
            p.style = Paint.Style.STROKE
            p.strokeWidth = 0.75f
            p.color = Color.BLACK
            canvas.drawRect(left, currentY, right, currentY + rowH, p)
            canvas.drawLine(left + itemW, currentY, left + itemW, currentY + rowH, p)
            canvas.drawLine(siX, currentY, siX, currentY + rowH, p)
            canvas.drawLine(noX, currentY, noX, currentY + rowH, p)

            p.style = Paint.Style.FILL
            p.color = Color.BLACK
            p.typeface = Typeface.DEFAULT
            p.textSize = 8.4f
            drawCenteredText(canvas, p, (startNumber + idx).toString(), left, left + itemW, currentY + 13f)
            canvas.drawText(question.take(90), left + itemW + 4f, currentY + 13f, p)

            val respuesta = checklist.optJSONObject(startIndex + idx)?.optString("respuesta", "") ?: ""
            p.color = blue
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 15f
            when (respuesta.uppercase(Locale.US)) {
                "SI", "SÍ" -> canvas.drawText("✓", siX + 8f, currentY + 15f, p)
                "NO" -> canvas.drawText("X", noX + 8f, currentY + 14f, p)
            }
            currentY += rowH
        }
        return currentY
    }


    // -------------------------------------------------------------------------
    // Páginas de fotos compartidas por todos los formatos.
    // -------------------------------------------------------------------------

    private fun addPhotoPages(doc: PdfDocument, dir: File, photos: List<PhotoItem>) {
        photos.take(10).forEachIndexed { idx, item ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, idx + 2).create())
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(Color.WHITE)
            paint.color = Color.BLACK
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textSize = 18f
            canvas.drawText("Evidencia fotográfica ${idx + 1}", 35f, 45f, paint)

            try {
                // Usa el compresor configurable en PhotoQuality.kt.
                val compressed = PhotoManager.compressForReport(activity, item.uri, File(dir, "fotos_tmp"))
                val bmp = BitmapFactory.decodeFile(compressed.absolutePath)
                    ?: MediaStore.Images.Media.getBitmap(activity.contentResolver, item.uri)
                val maxW = 525f
                val maxH = 720f
                val scale = minOf(maxW / bmp.width.toFloat(), maxH / bmp.height.toFloat())
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                val left = (595f - drawW) / 2f
                canvas.drawBitmap(
                    bmp,
                    null,
                    RectF(left, 80f, left + drawW, 80f + drawH),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
                )
            } catch (_: Exception) {
                paint.typeface = Typeface.DEFAULT
                paint.textSize = 12f
                canvas.drawText("No se pudo anexar esta foto.", 35f, 85f, paint)
            }
            doc.finishPage(page)
        }
    }

    // -------------------------------------------------------------------------
    // Render genérico temporal para los otros formatos.
    // Después se reemplazará por el diseño físico TOS y Lista de Chequeo.
    // -------------------------------------------------------------------------

    private fun addGenericPage(doc: PdfDocument, report: HseReport, visibleLines: List<String>, json: JSONObject) {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawGenericHeader(page.canvas, paint, report, json)
        var y = 165f
        visibleLines.filterNot { it.startsWith("Folio:", ignoreCase = true) }
            .filterNot { it.startsWith("Fecha:", ignoreCase = true) }
            .filterNot { it.startsWith("Hora:", ignoreCase = true) }
            .forEach { line ->
            paint.color = Color.BLACK
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 12f
            wrapText(line, 72).forEach {
                page.canvas.drawText(it, 35f, y, paint)
                y += 17f
            }
            y += 4f
        }
        doc.finishPage(page)
    }

    private fun drawGenericHeader(canvas: Canvas, p: Paint, report: HseReport, json: JSONObject) {
        canvas.drawColor(Color.WHITE)

        val left = 25f
        val top = 20f
        val right = 570f
        val headerBottom = 105f

        p.style = Paint.Style.STROKE
        p.strokeWidth = 1.3f
        p.color = Color.BLACK
        canvas.drawRect(left, top, right, headerBottom, p)

        p.style = Paint.Style.FILL
        val logo = loadLogoBitmap()
        if (logo != null) {
            canvas.drawBitmap(logo, null, RectF(40f, 34f, 145f, 72f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG))
        } else {
            p.color = Color.rgb(181, 30, 46)
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = 22f
            canvas.drawText("SINOPEC", 42f, 57f, p)
        }

        p.color = Color.BLACK
        p.typeface = Typeface.DEFAULT_BOLD
        p.textSize = 19f
        canvas.drawText("Formatos HSE Campo", 210f, 48f, p)
        p.textSize = 13f
        canvas.drawText(report.format.title, 210f, 75f, p)
        drawQr(canvas, report.folio, 515f, 35f, 45)

        val fecha = json.optString("fecha", "")
        val hora = json.optString("hora", "")
        p.typeface = Typeface.DEFAULT
        p.textSize = 10f
        canvas.drawText("Folio: ${report.folio}", 35f, 125f, p)
        canvas.drawText("Fecha: $fecha", 35f, 140f, p)
        canvas.drawText("Hora: $hora", 170f, 140f, p)
    }

    // -------------------------------------------------------------------------
    // Utilidades de dibujo y lectura de JSON.
    // -------------------------------------------------------------------------

    private fun jsonText(json: JSONObject, key: String): String = json.optString(key, "")

    private fun jsonGeneral(json: JSONObject, key: String): String {
        return json.optJSONObject("datos_generales")?.optString(key, "")
            ?: json.optString(key, "")
    }

    private fun loadLogoBitmap(): Bitmap? {
        val possibleNames = listOf("tarjeta_supervision", "sinopec_logo", "logo_sinopec", "logo")
        for (name in possibleNames) {
            val resId = activity.resources.getIdentifier(name, "drawable", activity.packageName)
            if (resId != 0) return BitmapFactory.decodeResource(activity.resources, resId)
        }
        return null
    }

    private fun drawFolioQr(canvas: Canvas, p: Paint, folio: String, x: Float, y: Float) {
        try {
            val qr = makeQr(folio, 34)
            canvas.drawBitmap(qr, x, y, null)
            p.color = Color.BLACK
            p.textSize = 4.4f
            p.typeface = Typeface.DEFAULT
            canvas.drawText(folio.take(18), x - 7f, y + 42f, p)
        } catch (_: Exception) {
        }
    }

    private fun drawQr(canvas: Canvas, text: String, x: Float, y: Float, size: Int) {
        val bmp = makeQr(text, size)
        canvas.drawBitmap(bmp, x, y, null)
    }

    private fun makeQr(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    private fun drawCenteredText(canvas: Canvas, p: Paint, text: String, left: Float, right: Float, y: Float) {
        val tw = p.measureText(text)
        canvas.drawText(text, left + ((right - left) - tw) / 2f, y, p)
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val clean = text.replace("\n", " ").trim()
        if (clean.isBlank()) return emptyList()
        val words = clean.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val next = if (current.isBlank()) word else "$current $word"
            if (next.length <= maxChars) current = next else {
                if (current.isNotBlank()) lines.add(current)
                current = word
            }
        }
        if (current.isNotBlank()) lines.add(current)
        return lines
    }

    @Suppress("unused")
    private fun buildShortHash(text: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02X".format(it) }
            .take(8)
    }
    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        val clean = text.trim()

        if (paint.measureText(clean) <= maxWidth) {
            return clean
        }

        var result = clean

        while (result.isNotEmpty() && paint.measureText("$result...") > maxWidth) {
            result = result.dropLast(1)
        }

        return "$result..."
    }
}
