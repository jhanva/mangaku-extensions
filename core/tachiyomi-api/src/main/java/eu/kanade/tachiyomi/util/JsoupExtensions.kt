package eu.kanade.tachiyomi.util

import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/** Convierte el cuerpo de la respuesta en un documento Jsoup, usando la url final como base. */
fun Response.asJsoup(html: String? = null): Document {
    return Jsoup.parse(html ?: body!!.string(), request.url.toString())
}
