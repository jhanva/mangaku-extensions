package eu.kanade.tachiyomi.multisrc.madara

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Plantilla multisrc "Madara": la base de tema WordPress mas reutilizada del ecosistema Tachiyomi.
 * Cubre cientos de sitios de manga/manhwa que comparten esta estructura HTML. Una fuente concreta
 * solo necesita fijar [baseUrl], [name] y [lang]; los selectores y el flujo estan aqui y son
 * sobreescribibles.
 *
 * Portada de keiyoushi/extensions-source (Apache 2.0). Se omiten, respecto al original, los filtros
 * avanzados, load-more, i18n y el descifrado de paginas protegidas (casos borde); el flujo comun
 * (populares, busqueda, detalle, capitulos via AJAX y paginas) esta cubierto.
 */
abstract class Madara(
    override val name: String,
    override val baseUrl: String,
    override val lang: String,
    protected val dateFormat: SimpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US),
) : ParsedHttpSource() {

    override val supportsLatest = true

    /** Segmento de ruta del catalogo; en algunos sitios es "manga", en otros "series", "comics"... */
    protected open val mangaSubString = "manga"

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private val xhrHeaders: Headers by lazy {
        headersBuilder().add("X-Requested-With", "XMLHttpRequest").build()
    }

    // ============================== Populares ==============================

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl/$mangaSubString/page/$page/?m_orderby=views", headers)

    override fun popularMangaSelector(): String = "div.page-item-detail, .manga__item"

    protected open val mangaUrlSelector = "div.post-title a"
    protected open val mangaThumbnailSelector = "img"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        element.selectFirst(mangaUrlSelector)!!.let {
            setUrlWithoutDomain(it.attr("abs:href"))
            title = it.ownText().ifBlank { it.text() }
        }
        thumbnail_url = element.selectFirst(mangaThumbnailSelector)?.let { imageFromElement(it) }
    }

    override fun popularMangaNextPageSelector(): String? = "div.nav-previous, nav.navigation-ajax, a.nextpostslink"

    // =============================== Latest ================================

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/$mangaSubString/page/$page/?m_orderby=latest", headers)

    override fun latestUpdatesSelector(): String = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)
    override fun latestUpdatesNextPageSelector(): String? = popularMangaNextPageSelector()

    // =============================== Busqueda ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        GET("$baseUrl/page/$page/?s=${query.trim()}&post_type=wp-manga", headers)

    override fun searchMangaSelector(): String = "div.c-tabs-item__content, .manga__item, div.page-item-detail"

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector(): String? = popularMangaNextPageSelector()

    // =========================== Detalle de manga ==========================

    protected open val detailsTitleSelector = "div.post-title h3, div.post-title h1, #manga-title > h1"
    protected open val detailsAuthorSelector = "div.author-content > a, div.manga-authors > a"
    protected open val detailsArtistSelector = "div.artist-content > a"
    protected open val detailsStatusSelector = "div.summary-content, div.summary-heading:contains(Status) + div"
    protected open val detailsDescriptionSelector =
        "div.description-summary div.summary__content, div.summary_content div.post-content_item > h5 + div, div.summary_content div.manga-excerpt"
    protected open val detailsThumbnailSelector = "div.summary_image img"
    protected open val detailsGenreSelector = "div.genres-content a"

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst(detailsTitleSelector)?.text().orEmpty()
        author = document.select(detailsAuthorSelector).joinToString { it.text() }
        artist = document.select(detailsArtistSelector).joinToString { it.text() }
        description = document.selectFirst(detailsDescriptionSelector)?.text()
        genre = document.select(detailsGenreSelector).joinToString { it.text() }
        thumbnail_url = document.selectFirst(detailsThumbnailSelector)?.let { imageFromElement(it) }
        status = document.selectFirst(detailsStatusSelector)?.text().parseStatus()
    }

    protected open fun String?.parseStatus(): Int = when {
        this == null -> SManga.UNKNOWN
        contains("ongoing", true) || contains("en cours", true) || contains("actualizandose", true) -> SManga.ONGOING
        contains("completed", true) || contains("completo", true) || contains("finalizado", true) -> SManga.COMPLETED
        contains("hiatus", true) || contains("pausa", true) -> SManga.ON_HIATUS
        contains("cancel", true) || contains("dropped", true) -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    // ============================== Capitulos ==============================

    protected open fun chapterListSelectorInternal() = "li.wp-manga-chapter"
    override fun chapterListSelector(): String = chapterListSelectorInternal()

    protected open val chapterUrlSuffix = "?style=list"

    /**
     * Los capitulos pueden venir en el HTML del detalle o cargarse por AJAX. Se intenta el HTML y,
     * si no hay, se pide el endpoint `ajax/chapters` (el moderno de la mayoria de sitios Madara).
     */
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        var elements = document.select(chapterListSelector())

        if (elements.isEmpty()) {
            val holder = document.selectFirst("div[id^=manga-chapters-holder]")
            val mangaUrl = document.location().removeSuffix("/")
            if (holder != null || mangaUrl.isNotEmpty()) {
                val ajax = POST("$mangaUrl/ajax/chapters", xhrHeaders)
                elements = client.newCall(ajax).execute().use { it.asJsoup().select(chapterListSelector()) }
            }
        }
        return elements.map { chapterFromElement(it) }
    }

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        element.selectFirst("a")!!.let { link ->
            val href = link.attr("abs:href").substringBefore("?style=paged")
            url = if (href.endsWith(chapterUrlSuffix)) href else href + chapterUrlSuffix
            name = link.text()
        }
        date_upload = parseChapterDate(element.selectFirst("span.chapter-release-date")?.text())
    }

    // =============================== Paginas ===============================

    protected open val pageListSelector =
        "div.page-break img, li.blocks-gallery-item img, .reading-content img"

    override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        return document.select(pageListSelector).mapIndexedNotNull { index, element ->
            val imageUrl = imageFromElement(element) ?: return@mapIndexedNotNull null
            Page(index, document.location(), imageUrl)
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    // ============================== Utilidades =============================

    /** Extrae la mejor URL de imagen manejando los multiples atributos lazy-load usados por Madara. */
    protected open fun imageFromElement(element: Element): String? = when {
        element.hasAttr("data-src") -> element.attr("abs:data-src")
        element.hasAttr("data-lazy-src") -> element.attr("abs:data-lazy-src")
        element.hasAttr("srcset") -> element.attr("abs:srcset").substringBefore(" ")
        element.hasAttr("data-cfsrc") -> element.attr("abs:data-cfsrc")
        else -> element.attr("abs:src")
    }.takeIf { it.isNotBlank() }

    protected open fun parseChapterDate(date: String?): Long {
        if (date.isNullOrBlank()) return 0
        parseRelativeDate(date)?.let { return it }
        return runCatching { dateFormat.parse(date)?.time ?: 0L }.getOrDefault(0L)
    }

    /** Fechas relativas ("2 hours ago", "hace 3 dias"...) en los idiomas mas comunes de Madara. */
    private fun parseRelativeDate(date: String): Long? {
        val number = Regex("""(\d+)""").find(date)?.value?.toIntOrNull() ?: return null
        val cal = Calendar.getInstance()
        val lower = date.lowercase()
        return when {
            lower.containsAny("day", "dia", "día", "jour", "hari") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number) }.timeInMillis
            lower.containsAny("hour", "hora", "heure", "jam") -> cal.apply { add(Calendar.HOUR, -number) }.timeInMillis
            lower.containsAny("min", "minuto", "minute") -> cal.apply { add(Calendar.MINUTE, -number) }.timeInMillis
            lower.containsAny("second", "segundo") -> cal.apply { add(Calendar.SECOND, -number) }.timeInMillis
            lower.containsAny("week", "semana") -> cal.apply { add(Calendar.DAY_OF_MONTH, -number * 7) }.timeInMillis
            lower.containsAny("month", "mes") -> cal.apply { add(Calendar.MONTH, -number) }.timeInMillis
            lower.containsAny("year", "año", "ano") -> cal.apply { add(Calendar.YEAR, -number) }.timeInMillis
            else -> null
        }
    }

    private fun String.containsAny(vararg words: String): Boolean = words.any { contains(it) }
}
