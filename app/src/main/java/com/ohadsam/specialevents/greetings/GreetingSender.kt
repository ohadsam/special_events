package com.ohadsam.specialevents.greetings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ohadsam.specialevents.data.SendChannel
import com.ohadsam.specialevents.domain.greetings.PhoneNumbers
import com.ohadsam.specialevents.domain.greetings.RichText
import com.ohadsam.specialevents.domain.greetings.Span

/**
 * Hands a greeting to WhatsApp, the mail app, the SMS app or any app. WhatsApp has no API for silent sending, so these open the
 * message ready to go and the user taps Send - which is also what keeps the app permission-free and safe.
 */
object GreetingSender {
    private val WHATSAPP_PACKAGES = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** Returns false when nothing could handle the request (e.g. no mail app installed). */
    fun send(
        context: Context,
        channel: SendChannel,
        text: String,
        phone: String?,
        email: String?,
        countryCode: String,
        subject: String,
        /** The styled greeting, when the text is still what the template produced; each app gets the styles it can show. */
        spans: List<Span>? = null,
    ): Boolean {
        val intents = when (channel) {
            SendChannel.WHATSAPP -> whatsApp(spans?.let(RichText::whatsApp) ?: text, phone, countryCode)
            SendChannel.EMAIL -> listOf(mail(text, email, subject, spans?.let(RichText::html)))
            SendChannel.SMS -> listOf(sms(text, phone))
            SendChannel.SHARE -> listOf(Intent.createChooser(plain(text), null))
        }
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: ActivityNotFoundException) {
                // try the next way
            }
        }
        return false
    }

    /**
     * Hands a recorded voice greeting to WhatsApp (straight to the person's chat when the number is known - the `jid` extra is
     * what WhatsApp reads for that) or, failing that, to any app. The user still taps Send.
     */
    fun sendVoice(context: Context, file: java.io.File, phone: String?, countryCode: String): Boolean {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
        fun audio() = Intent(Intent.ACTION_SEND).setType("audio/mp4").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val digits = PhoneNumbers.forWhatsApp(phone, countryCode)
        val intents = WHATSAPP_PACKAGES.map { pkg -> audio().setPackage(pkg).also { if (digits != null) it.putExtra("jid", "$digits@s.whatsapp.net") } } +
            Intent.createChooser(audio(), null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        for (intent in intents) {
            try {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return true
            } catch (e: ActivityNotFoundException) {
                // try the next way
            }
        }
        return false
    }

    private fun plain(text: String) = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

    /** A wa.me link opens the chat with the person when the number is known; otherwise the WhatsApp contact picker, otherwise any app. */
    private fun whatsApp(text: String, phone: String?, countryCode: String): List<Intent> {
        val digits = PhoneNumbers.forWhatsApp(phone, countryCode)
        if (digits != null) return listOf(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digits?text=${Uri.encode(text)}")))
        return WHATSAPP_PACKAGES.map { plain(text).setPackage(it) } + Intent.createChooser(plain(text), null)
    }

    private fun mail(text: String, email: String?, subject: String, html: String?): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).putExtra(Intent.EXTRA_EMAIL, arrayOf(email.orEmpty()).filter { it.isNotBlank() }.toTypedArray())
            .putExtra(Intent.EXTRA_SUBJECT, subject).putExtra(Intent.EXTRA_TEXT, text)
            .also { if (html != null) it.putExtra(Intent.EXTRA_HTML_TEXT, html) }

    private fun sms(text: String, phone: String?): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${PhoneNumbers.forDial(phone).orEmpty()}")).putExtra("sms_body", text)
}
