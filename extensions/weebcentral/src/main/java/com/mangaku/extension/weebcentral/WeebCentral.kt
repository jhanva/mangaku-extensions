package com.mangaku.extension.weebcentral

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.seconds

/**
 * Portado de la extension WeebCentral de keiyoushi/extensions-source (Apache 2.0). Se conservan los
 * selectores y la construccion de peticiones originales; se sustituyen las piezas especificas de
 * Keiyoushi (anotacion @Source, filtros UriFilter, API RxJava) por la API suspend de esta app. La
 * busqueda usa solo el texto; los filtros avanzados quedan fuera de alcance por ahora.
 */
class WeebCentral : HttpSource() {

    override val name = "WeebCentral"
    override val lang = "en"
    override val baseUrl = "https://weebcentral.com"
    override val supportsLatest = true

    private val baseUrlHost by lazy { baseUrl.toHttpUrl().host }

    override val client: OkHttpClient = network.client.newBuilder()
        .rateLimit(1, 2.seconds) { it.host == baseUrlHost }
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // ============================== Populares ==============================

    override fun popularMangaRequest(page: Int): Request = searchRequest(page, "", "Popularity")

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    // ============================ Actualizaciones ==========================

    override fun latestUpdatesRequest(page: Int): Request = searchRequest(page, "", "Latest Updates")

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)

    // =============================== Busqueda ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        searchRequest(page, query, "Best Match")

    private fun searchRequest(page: Int, query: String, sort: String): Request {
        val url = "$baseUrl/search/data".toHttpUrl().newBuilder().apply {
            addQueryParameter("text", query.replace(excludedSearchCharacters, " ").trim())
            addQueryParameter("sort", sort)
            addQueryParameter("order", "Descending")
            addQueryParameter("official", "Any")
            addQueryParameter("limit", FETCH_LIMIT.toString())
            addQueryParameter("offset", ((page - 1) * FETCH_LIMIT).toString())
            addQueryParameter("display_mode", "Full Display")
        }.build()
        return GET(url, headers)
    }

    public override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select("article > section > a").map { element ->
            SManga.create().apply {
                thumbnail_url = element.sourceImg()
                title = element.selectFirst("div:not([class]):last-child")!!.text()
                setUrlWithoutDomain(element.attr("abs:href"))
            }
        }
        val hasNextPage = document.selectFirst("button") != null
        return MangasPage(mangas, hasNextPage)
    }

    // =========================== Detalle de manga ==========================

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()

        with(document.select("section[x-data] > section")[0]) {
            thumbnail_url = sourceImg()
            author = select("ul > li:has(strong:contains(Author)) > span > a").joinToString { it.text() }
            genre = select("ul > li:has(strong:contains(Tag),strong:contains(Type)) a").joinToString { it.text() }
            status = selectFirst("ul > li:has(strong:contains(Status)) > a").parseStatus()
        }

        with(document.select("section[x-data] > section")[1]) {
            title = selectFirst("h1")!!.text()
            description = buildString {
                selectFirst("li:has(strong:contains(Description)) > p")?.text()?.let {
                    append(it.replace("NOTE: ", "\n\nNOTE: "))
                }
                val alternateTitles = select("li:has(strong:contains(Associated Name)) li")
                if (alternateTitles.isNotEmpty()) {
                    append("\n\nAssociated Name(s):")
                    alternateTitles.forEach { append("\n- ${it.text()}") }
                }
            }
        }

        setUrlWithoutDomain(document.location())
    }

    private fun Element?.parseStatus(): Int = when (this?.text()?.lowercase()) {
        "ongoing" -> SManga.ONGOING
        "complete" -> SManga.COMPLETED
        "hiatus" -> SManga.ON_HIATUS
        "canceled" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    // ============================== Capitulos ==============================

    override fun chapterListRequest(manga: SManga): Request {
        val url = (baseUrl + manga.url).toHttpUrl().newBuilder().apply {
            removePathSegment(2)
            addPathSegment("full-chapter-list")
        }.build()
        return GET(url, headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select("div[x-data] > a").map { element ->
            SChapter.create().apply {
                name = element.selectFirst("span.flex > span")!!.text()
                setUrlWithoutDomain(element.attr("abs:href"))
                element.selectFirst("time[datetime]")?.also {
                    date_upload = parseDate(it.attr("datetime"))
                }
                element.selectFirst("svg")?.attr("stroke")?.also { stroke ->
                    scanlator = when (stroke) {
                        "#d8b4fe" -> "Official"
                        "#4C4D54" -> "Unknown"
                        else -> null
                    }
                }
            }
        }
    }

    // =============================== Paginas ===============================

    override fun pageListRequest(chapter: SChapter): Request {
        val newUrl = (baseUrl + chapter.url)
            .toHttpUrlOrNull()
            ?.newBuilder()
            ?.addPathSegment("images")
            ?.addQueryParameter("is_prev", "False")
            ?.addQueryParameter("reading_style", "long_strip")
            ?.build()
            ?.toString()
            ?: (baseUrl + chapter.url)
        return GET(newUrl, headers)
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    public override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        return document.select("section[x-data~=scroll] > img").mapIndexed { index, element ->
            Page(index, imageUrl = element.attr("abs:src"))
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    // ============================== Utilidades =============================

    private fun Element.sourceImg(): String? =
        selectFirst("source")?.attr("srcset")?.replace("small", "normal")
            ?: selectFirst("img")?.attr("abs:src")

    private fun parseDate(value: String): Long = runCatching { dateFormat.parse(value)?.time ?: 0L }.getOrDefault(0L)

    companion object {
        const val FETCH_LIMIT = 32
        private val excludedSearchCharacters = "[!#:(),-]".toRegex()
    }
}
