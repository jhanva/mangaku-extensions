package com.mangaku.extension.senmanga

import com.google.gson.Gson
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Portado de la extension SenManga de keiyoushi/extensions-source (Apache 2.0). SenManga sirve raws
 * en japones a traves de una API JSON, ideal para la traduccion de imagenes y el modo estudio. Se
 * sustituye kotlinx.serialization/keiyoushi.utils por Gson; los filtros avanzados quedan fuera de
 * alcance (la busqueda usa solo el texto).
 */
class SenManga : HttpSource() {

    override val name = "SenManga (Raw)"
    override val lang = "ja"
    override val baseUrl = "https://raw.senmanga.com"
    override val supportsLatest = true

    private val apiUrl = "$baseUrl/api"
    private val gson = Gson()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override fun popularMangaRequest(page: Int): Request =
        GET("$apiUrl/directory?order=Popular&page=$page", headers)

    public override fun popularMangaParse(response: Response): MangasPage {
        val data = response.parseAs(DirectoryResponse::class.java)
        val mangas = data.series.map { it.toSManga() }
        val hasNext = (data.currentPage ?: 1) < (data.totalPages ?: 1)
        return MangasPage(mangas, hasNext)
    }

    override fun latestUpdatesRequest(page: Int): Request = GET("$apiUrl/home?page=$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val data = response.parseAs(HomeResponse::class.java)
        val mangas = data.series.map { it.toSManga() }
        return MangasPage(mangas, mangas.isNotEmpty())
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiUrl/directory".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
        if (query.isNotBlank()) {
            url.addQueryParameter("s", query)
        }
        return GET(url.build(), headers)
    }

    public override fun searchMangaParse(response: Response): MangasPage = popularMangaParse(response)

    override fun mangaDetailsRequest(manga: SManga): Request = GET("$apiUrl/manga/${manga.url}", headers)

    override fun mangaDetailsParse(response: Response): SManga = response.parseAs(SeriesDto::class.java).toSManga()

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    public override fun chapterListParse(response: Response): List<SChapter> {
        val data = response.parseAs(SeriesDto::class.java)
        val mangaSlug = data.slug
        return data.chapterList?.mapNotNull { chapter ->
            val chapterUrl = chapter.url ?: return@mapNotNull null
            SChapter.create().apply {
                url = "$mangaSlug/$chapterUrl"
                name = chapter.title.orEmpty()
                date_upload = parseDate(chapter.datetime)
            }
        }.orEmpty()
    }

    override fun pageListRequest(chapter: SChapter): Request = GET("$apiUrl/read/${chapter.url}", headers)

    public override fun pageListParse(response: Response): List<Page> {
        val data = response.parseAs(ReadResponse::class.java)
        return data.pages.mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/manga/${manga.url}"

    override fun getChapterUrl(chapter: SChapter): String {
        val mangaSlug = chapter.url.substringBefore("/")
        val chapterSlug = chapter.url.substringAfter("/")
        return "$baseUrl/manga/$mangaSlug/chapter-$chapterSlug/"
    }

    private fun <T> Response.parseAs(clazz: Class<T>): T = use {
        gson.fromJson(it.body!!.charStream(), clazz)
    }

    private fun parseDate(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        return runCatching { dateFormat.parse(value)?.time ?: 0L }.getOrDefault(0L)
    }

    private companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
