package com.mangaku.extension.inmanga

import com.google.gson.Gson
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Portado de la extension InManga de keiyoushi/extensions-source (Apache 2.0). InManga (es) expone su
 * catalogo por una API que responde HTML/JSON a peticiones POST con formulario. La lista de capitulos
 * llega como un JSON anidado (un campo `data` que es a su vez una cadena JSON). Se sustituye
 * kotlinx.serialization/keiyoushi.utils por Gson; el sitio esta tras Cloudflare (lo resuelve el host).
 */
class InManga : HttpSource() {

    override val name = "InManga"
    override val baseUrl = "https://inmanga.com"
    override val lang = "es"
    override val supportsLatest = true

    private val gson = Gson()

    private val postHeaders = headers.newBuilder()
        .add("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        .add("X-Requested-With", "XMLHttpRequest")
        .build()

    private val imageCDN = "https://cdn1.intomanga.com"

    private fun requestBodyBuilder(page: Int, isPopular: Boolean): RequestBody =
        ("filter%5Bgeneres%5D%5B%5D=-1&filter%5BqueryString%5D=&filter%5Bskip%5D=${(page - 1) * 10}" +
            "&filter%5Btake%5D=10&filter%5Bsortby%5D=${if (isPopular) "1" else "3"}" +
            "&filter%5BbroadcastStatus%5D=0&filter%5BonlyFavorites%5D=false&d=")
            .toRequestBody(null)

    override fun popularMangaRequest(page: Int): Request =
        POST("$baseUrl/manga/getMangasConsultResult", postHeaders, requestBodyBuilder(page, true))

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    override fun latestUpdatesRequest(page: Int): Request =
        POST("$baseUrl/manga/getMangasConsultResult", postHeaders, requestBodyBuilder(page, false))

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val skip = (page - 1) * 10
        val body = ("filter%5Bgeneres%5D%5B%5D=-1&filter%5BqueryString%5D=$query&filter%5Bskip%5D=$skip" +
            "&filter%5Btake%5D=10&filter%5Bsortby%5D=1&filter%5BbroadcastStatus%5D=0&filter%5BonlyFavorites%5D=false&d=")
            .toRequestBody(null)
        return POST("$baseUrl/manga/getMangasConsultResult", postHeaders, body)
    }

    public override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val elements = document.select("body > a")
        val mangas = elements.map { element ->
            SManga.create().apply {
                setUrlWithoutDomain(element.attr("href"))
                title = element.select("h4.m0").text()
                thumbnail_url = element.select("img").attr("abs:data-src")
            }
        }
        return MangasPage(mangas, elements.size == 10)
    }

    override fun mangaDetailsParse(response: Response): SManga = SManga.create().apply {
        val document = response.asJsoup()
        document.select("div.col-md-3 div.panel.widget").let { info ->
            thumbnail_url = info.select("img").attr("abs:src")
            status = parseStatus(info.select("a.list-group-item:contains(estado) span").text())
        }
        document.select("div.col-md-9").let { info ->
            title = info.select("h1").text()
            description = info.select("div.panel-body").text()
        }
    }

    private fun parseStatus(status: String): Int = when {
        status.contains("En emisión") -> SManga.ONGOING
        status.contains("Finalizado") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    override fun chapterListRequest(manga: SManga): Request =
        GET("$baseUrl/chapter/getall?mangaIdentification=${manga.url.substringAfterLast("/")}", headers)

    public override fun chapterListParse(response: Response): List<SChapter> {
        val outer = gson.fromJson(response.body!!.charStream(), InMangaResultDto::class.java)
        val inner = outer?.data?.takeIf { it.isNotBlank() } ?: return emptyList()
        val result = gson.fromJson(inner, InMangaChapterResult::class.java)
        if (!result.success) return emptyList()
        return result.result
            .map { chapterFromObject(it) }
            .sortedByDescending { it.chapter_number }
    }

    private fun chapterFromObject(chapter: InMangaChapterDto): SChapter = SChapter.create().apply {
        url = "/chapter/chapterIndexControls?identification=${chapter.identification}"
        name = "Capitulo ${chapter.friendlyChapterNumber}"
        chapter_number = chapter.number?.toFloat() ?: 0f
        date_upload = parseDate(chapter.registrationDate)
    }

    public override fun pageListParse(response: Response): List<Page> {
        val document = response.asJsoup()
        val chapterId = document.select("input#ChapterIdentification").attr("value")
        val mangaId = document.select("input#MangaIdentification").attr("value")
        return document.select("img.ImageContainer").mapIndexed { i, img ->
            Page(i, imageUrl = "$imageCDN/i/m/$mangaId/c/$chapterId/o/${img.attr("id")}.jpg")
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getFilterList() = FilterList()

    private fun parseDate(value: String): Long =
        runCatching { DATE_FORMATTER.parse(value)?.time ?: 0L }.getOrDefault(0L)

    private companion object {
        private val DATE_FORMATTER = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
