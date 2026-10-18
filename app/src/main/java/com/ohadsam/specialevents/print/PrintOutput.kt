package com.ohadsam.specialevents.print

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import com.ohadsam.specialevents.contacts.PhotoStore
import com.ohadsam.specialevents.domain.print.Paper
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Android side of printing: a locked-down WebView, the system print dialog (which also saves PDF), a picture of the page, a photo as a data URI. */
object PrintOutput {
    private const val IMAGE_WIDTH_PX = 1080
    private const val MAX_IMAGE_HEIGHT_PX = 6000
    private const val PHOTO_SIDE_PX = 480
    private const val JPEG_QUALITY = 85
    private const val RENDER_DELAY_MS = 350L

    /** Shows the generated page and nothing else: no script, no files, no network. */
    fun newWebView(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = false
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.blockNetworkLoads = true
        settings.setSupportZoom(false)
        setBackgroundColor(Color.WHITE)
    }

    fun load(webView: WebView, html: String) {
        webView.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
    }

    /** Opens the system print dialog for what [webView] shows; "Save as PDF" is one of its printers. */
    fun print(context: Context, webView: WebView, jobName: String, paper: Paper) {
        val manager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val size = if (paper == Paper.A5) PrintAttributes.MediaSize.ISO_A5 else PrintAttributes.MediaSize.ISO_A4
        manager.print(jobName, webView.createPrintDocumentAdapter(jobName), PrintAttributes.Builder().setMediaSize(size).build())
    }

    /** A small round-able JPEG of an event's photo as a `data:` URI, or null when there is none. */
    fun photoDataUri(uri: String?): String? {
        val bitmap = uri?.let { PhotoStore.thumb(it, PHOTO_SIDE_PX) } ?: return null
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Draws [html] into a PNG in [dir]. Best effort: WebView drawing off-screen can come out blank on some devices, in which case
     * [onDone] gets null and the user is pointed to "print / save as PDF".
     */
    fun renderToPng(context: Context, html: String, dir: File, onDone: (File?) -> Unit) {
        val web = newWebView(context)
        web.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                view.postDelayed({ onDone(capture(view, dir)) }, RENDER_DELAY_MS)
            }
        }
        web.measure(View.MeasureSpec.makeMeasureSpec(IMAGE_WIDTH_PX, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        web.layout(0, 0, IMAGE_WIDTH_PX, max(web.measuredHeight, 1))
        load(web, html)
    }

    private fun capture(web: WebView, dir: File): File? = runCatching {
        val height = (web.contentHeight * web.resources.displayMetrics.density).toInt().coerceIn(1, MAX_IMAGE_HEIGHT_PX)
        web.measure(View.MeasureSpec.makeMeasureSpec(IMAGE_WIDTH_PX, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        web.layout(0, 0, IMAGE_WIDTH_PX, height)
        val bitmap = Bitmap.createBitmap(IMAGE_WIDTH_PX, height, Bitmap.Config.ARGB_8888)
        web.draw(Canvas(bitmap))
        web.destroy()
        if (isBlank(bitmap)) return null
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        File(dir, "card.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }.getOrNull()

    /** True when the picture is all one colour, i.e. the WebView drew nothing. */
    private fun isBlank(bitmap: Bitmap): Boolean {
        val first = bitmap.getPixel(0, 0)
        val stepX = max(1, bitmap.width / 24)
        val stepY = max(1, bitmap.height / 24)
        for (x in 0 until bitmap.width step stepX) for (y in 0 until bitmap.height step stepY) if (bitmap.getPixel(x, y) != first) return false
        return true
    }
}
