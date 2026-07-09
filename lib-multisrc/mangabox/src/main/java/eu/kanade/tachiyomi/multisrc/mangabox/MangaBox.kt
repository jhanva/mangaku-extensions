package eu.kanade.tachiyomi.multisrc.mangabox

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Plantilla multisrc "MangaBox": la familia Manganato / MangaKakalot / Mangabat y sus mirrors
 * (natomanga, nelomanga, mangakakalot.gg...). Comparten estructura HTML para catalogo/detalle y una
 * API JSON para la lista de capitulos; las imagenes de cada capitulo se leen de un array `cdns`/
 * `chapterImages` embebido en un <script> de la pagina del capitulo.
 *
 * Portada de keiyoushi/extensions-source (Apache 2.0), version enfocada: se omiten respecto al
 * original los filtros por genero/estado, la union de imagenes partidas (preferencia "merge", por
 * defecto desactivada), el fallback de CDN alternativo y las preferencias configurables. El flujo
 * comun (populares, ultimas, busqueda por texto, detalle, capitulos via API y paginas) esta cubierto.
 * Los sitios estan tras Cloudflare; el reto lo resuelve el CloudflareInterceptor de la app anfitriona.
 */
abstract class MangaBox : HttpSource() {

    override val supportsLatest = true

    private val gson = Gson()

    protected open val dateFormat: SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    protected open val popularUrlPath = "manga-list/hot-manga?page="
    protected open val latestUrlPath = "manga-list/latest-manga?page="
    protected open val simpleQueryPath = "search/story/"

    // ============================== Populares ==============================

    protected open fun popularMangaSelector() =
        ":is(div.truyen-list > div.list-truyen-item-wrap, div.comic-list > .list-comic-item-wrap):has(a[data-id])"

    protected open fun popularMangaNextPageSelector() =
        "div.group_page, div.group-page a:not([href]) + a:not(:contains(Last))"

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/$popularUrlPath$page", headers)

    override fun popularMangaParse(response: Response): MangasPage =
        parseMangaList(response.asJsoup(), popularMangaSelector(), popularMangaNextPageSelector())

    // ============================== Ultimas ================================

    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/$latestUrlPath$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage =
        parseMangaList(response.asJsoup(), popularMangaSelector(), popularMangaNextPageSelector())

    // =============================== Busqueda ==============================

    protected open fun searchMangaSelector() =
        ".panel_story_list .story_item, div.list-truyen-item-wrap, div.list-comic-item-wrap"

    protected open fun searchMangaNextPageSelector() =
        "a.page_select + a:not(.page_last), a.page-select + a:not(.page-last)"

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isBlank()) return popularMangaRequest(page)
        val url = "$baseUrl/$simpleQueryPath".toHttpUrl().newBuilder()
            .addPathSegment(normalizeSearchQuery(query))
            .addQueryParameter("page", page.toString())
            .build()
        return GET(url, headers)
    }

    public override fun searchMangaParse(response: Response): MangasPage =
        parseMangaList(response.asJsoup(), searchMangaSelector(), searchMangaNextPageSelector())

    private fun parseMangaList(document: Document, itemSelector: String, nextSelector: String): MangasPage {
        val mangas = document.select(itemSelector).map { mangaFromElement(it) }
        val hasNext = nextSelector.isNotEmpty() && document.selectFirst(nextSelector) != null
        return MangasPage(mangas, hasNext)
    }

    private fun mangaFromElement(element: Element, urlSelector: String = "h3 a"): SManga = SManga.create().apply {
        val link = element.selectFirst(urlSelector)!!
        // Se guarda solo la ruta (sin dominio) para sobrevivir a cambios de mirror.
        url = link.attr("abs:href").substringAfter(baseUrl)
        title = link.text()
        thumbnail_url = element.selectFirst("img")?.attr("abs:src")
    }

    // ============================== Detalle ================================

    protected open val mangaDetailsMainSelector = "div.manga-info-top, div.panel-story-info"
    protected open val thumbnailSelector = "div.manga-info-pic img, span.info-image img"
    protected open val descriptionSelector = "div#noidungm, div#panel-story-info-description, div#contentBox"

    override fun mangaDetailsRequest(manga: SManga): Request {
        if (manga.url.startsWith("http")) return GET(manga.url, headers)
        return super.mangaDetailsRequest(manga)
    }

    override fun mangaDetailsParse(response: Response): SManga = mangaDetailsParse(response.asJsoup())

    protected open fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val info = document.selectFirst(mangaDetailsMainSelector)
        if (info != null) {
            title = info.selectFirst("h1, h2")?.text().orEmpty()
            author = info.select("li:contains(author) a, td:containsOwn(author) + td a").eachText().joinToString()
            status = parseStatus(info.select("li:contains(status), td:containsOwn(status) + td").text())
            genre = info.selectFirst("div.manga-info-top li:contains(genres)")?.select("a")?.joinToString { it.text() }
                ?: info.select("td:containsOwn(genres) + td a").joinToString { it.text() }
        }
        description = document.selectFirst(descriptionSelector)?.ownText()
            ?.replace("""<\s*br\s*/?>""".toRegex(), "\n")
            ?.replace("<[^>]*>".toRegex(), "")
        thumbnail_url = document.selectFirst(thumbnailSelector)?.attr("abs:src")
    }

    private fun parseStatus(status: String?): Int = when {
        status == null -> SManga.UNKNOWN
        status.contains("Ongoing", ignoreCase = true) -> SManga.ONGOING
        status.contains("Completed", ignoreCase = true) -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    // ============================== Capitulos ==============================

    /** La lista de capitulos viene de una API JSON paginada; se recorren todas las paginas. */
    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val slug = manga.url.trimEnd('/').substringAfterLast("/")
        val chapters = mutableListOf<SChapter>()
        var offset = 0
        var page = 0
        while (true) {
            val url = "$baseUrl/api/manga/$slug/chapters?limit=$CHAPTER_LIST_TAKE&offset=$offset"
            val body = client.newCall(GET(url, headers)).awaitSuccess().use { it.body!!.charStream().readText() }
            chapters += chaptersFromApi(body, slug)
            val hasMore = gson.fromJson(body, ApiResponse::class.java)?.data?.pagination?.hasMore == true
            if (!hasMore || ++page >= MAX_CHAPTER_PAGES) break
            offset += CHAPTER_LIST_TAKE
        }
        return chapters
    }

    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException("getChapterList consume la API paginada")

    /** Mapea la respuesta JSON de la API de capitulos a [SChapter]. Puro, para tests. */
    fun chaptersFromApi(json: String, slug: String): List<SChapter> {
        val result = gson.fromJson(json, ApiResponse::class.java) ?: return emptyList()
        return result.data?.chapters.orEmpty().mapNotNull { ch ->
            val chapterSlug = ch.chapterSlug?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            SChapter.create().apply {
                name = ch.chapterName.orEmpty().ifBlank { "Chapter" }
                url = "/manga/$slug/$chapterSlug"
                chapter_number = ch.chapterNum ?: -1f
                date_upload = ch.updatedAt?.let { parseDate(it) } ?: 0L
            }
        }
    }

    // =============================== Paginas ===============================

    override fun pageListRequest(chapter: SChapter): Request {
        if (chapter.url.startsWith("http")) return GET(chapter.url, headers)
        return super.pageListRequest(chapter)
    }

    override fun pageListParse(response: Response): List<Page> = pageListParse(response.asJsoup())

    /** Extrae las imagenes del <script> (cdns + chapterImages) con fallback al HTML. Puro, para tests. */
    fun pageListParse(document: Document): List<Page> {
        val content = document.select("script:containsData(cdns =)").joinToString("\n") { it.data() }
        val cdns = extractArray(content, CDNS_REGEX) + extractArray(content, BACKUP_IMAGE_REGEX)
        val chapterImages = extractArray(content, CHAPTER_IMAGES_REGEX)

        val imageUrls = if (chapterImages.isNotEmpty() && cdns.isNotEmpty()) {
            val cdn = cdns[0].toHttpUrl()
            chapterImages.map { path ->
                cdn.newBuilder().encodedPath("/$path".replace("//", "/")).build().toString()
            }
        } else {
            document.select("div.container-chapter-reader > img").map { it.absUrl("src") }
        }

        return imageUrls.mapIndexed { index, url -> Page(index, url = document.location(), imageUrl = url) }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    // ============================== Utilidades =============================

    private fun extractArray(script: String, regex: Regex): List<String> =
        regex.find(script)?.groupValues?.get(1)
            ?.split(",")
            ?.map { it.trim().removeSurrounding("\"").replace("\\/", "/").removeSuffix("/") }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

    private fun parseDate(value: String): Long =
        runCatching { dateFormat.parse(value.substringBefore("."))?.time ?: 0L }.getOrDefault(0L)

    /** Normaliza la query como la funcion change_alias del sitio (quita diacriticos y simbolos). */
    open fun normalizeSearchQuery(query: String): String {
        var str = query.lowercase()
        str = str.replace("[àáạảãâầấậẩẫăằắặẳẵ]".toRegex(), "a")
        str = str.replace("[èéẹẻẽêềếệểễ]".toRegex(), "e")
        str = str.replace("[ìíịỉĩ]".toRegex(), "i")
        str = str.replace("[òóọỏõôồốộổỗơờớợởỡ]".toRegex(), "o")
        str = str.replace("[ùúụủũưừứựửữ]".toRegex(), "u")
        str = str.replace("[ỳýỵỷỹ]".toRegex(), "y")
        str = str.replace("đ".toRegex(), "d")
        str = str.replace("""!|@|%|\^|\*|\(|\)|\+|=|<|>|\?|/|,|\.|:|;|'| |"|&|#|\[|]|~|-|$|_""".toRegex(), "_")
        str = str.replace("_+_".toRegex(), "_")
        str = str.replace("""^_+|_+$""".toRegex(), "")
        return str
    }

    private class ApiResponse(val data: ApiData?)
    private class ApiData(val chapters: List<ApiChapter>?, val pagination: ApiPagination?)
    private class ApiChapter(
        @SerializedName("chapter_name") val chapterName: String?,
        @SerializedName("chapter_slug") val chapterSlug: String?,
        @SerializedName("chapter_num") val chapterNum: Float?,
        @SerializedName("updated_at") val updatedAt: String?,
    )
    private class ApiPagination(@SerializedName("has_more") val hasMore: Boolean = false)

    private companion object {
        const val CHAPTER_LIST_TAKE = 1000
        const val MAX_CHAPTER_PAGES = 50

        val CDNS_REGEX = Regex("""cdns\s*=\s*\[([^]]+)]""")
        val BACKUP_IMAGE_REGEX = Regex("""backupImage\s*=\s*\[([^]]+)]""")
        val CHAPTER_IMAGES_REGEX = Regex("""chapterImages\s*=\s*\[([^]]+)]""")
    }
}
