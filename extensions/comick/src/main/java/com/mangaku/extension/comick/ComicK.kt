package com.mangaku.extension.comick

import com.google.gson.Gson
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response

/**
 * Fuente ComicK basada en su API JSON (api.comick.dev), el agregador que espeja MangaDex. Una
 * instancia por idioma (ver [ComicKFactory]); la API filtra capitulos por [lang] en el servidor.
 *
 * OJO con el dominio: comick.io responde 301 a comick.dev pero PIERDE el subdominio "api.", asi
 * que una peticion a api.comick.io acaba redirigida al frontend web y devuelve 404. Hay que ir
 * directo a api.comick.dev. La API esta detras de un challenge de Cloudflare (cf-mitigated), que
 * resuelve el CloudflareInterceptor de la app anfitriona.
 *
 * Las imagenes se sirven desde el CDN por su `b2key`. El detalle/capitulos/paginas usan el `hid`
 * como clave estable (guardado en SManga.url / SChapter.url).
 */
class ComicK(override val lang: String) : HttpSource() {

    override val name = "ComicK"
    override val baseUrl = "https://api.comick.dev"
    override val supportsLatest = false

    private val gson = Gson()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$WEB_URL/")

    // ============================== Populares ==============================

    override fun popularMangaRequest(page: Int): Request = searchUrl(query = null, page = page)

    override fun popularMangaParse(response: Response): MangasPage = parseSearch(response)

    // =============================== Busqueda ==============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request =
        searchUrl(query = query.trim().takeIf { it.isNotBlank() }, page = page)

    public override fun searchMangaParse(response: Response): MangasPage = parseSearch(response)

    private fun searchUrl(query: String?, page: Int): Request {
        val url = "$baseUrl/v1.0/search".toHttpUrl().newBuilder().apply {
            addQueryParameter("tachiyomi", "true")
            addQueryParameter("limit", SEARCH_LIMIT.toString())
            addQueryParameter("page", page.toString())
            if (query != null) addQueryParameter("q", query)
        }.build()
        return GET(url, headers)
    }

    private fun parseSearch(response: Response): MangasPage {
        val items = response.parseAs(Array<ComicKSearchItemDto>::class.java)
        val mangas = items.mapNotNull { item ->
            val hid = item.hid?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val title = item.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            SManga.create().apply {
                url = hid
                this.title = title
                thumbnail_url = item.mdCovers.coverUrl()
            }
        }
        return MangasPage(mangas, hasNextPage = items.size >= SEARCH_LIMIT)
    }

    // =========================== Detalle de manga ==========================

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET("$baseUrl/comic/${manga.url}?tachiyomi=true", headers)

    public override fun mangaDetailsParse(response: Response): SManga {
        val comic = response.parseAs(ComicKComicDetailDto::class.java).comic
            ?: throw Exception("Detalle no disponible")
        return SManga.create().apply {
            url = comic.hid.orEmpty()
            title = comic.title.orEmpty()
            description = comic.desc
            thumbnail_url = comic.mdCovers.coverUrl()
            genre = comic.genres.orEmpty()
                .mapNotNull { it.mdGenres?.name?.takeIf { name -> name.isNotBlank() } }
                .joinToString()
            status = when (comic.status) {
                1 -> SManga.ONGOING
                2 -> SManga.COMPLETED
                3 -> SManga.CANCELLED
                4 -> SManga.ON_HIATUS
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun getMangaUrl(manga: SManga): String = "$WEB_URL/comic/${manga.url}"

    // ============================== Capitulos ==============================

    // ComicK pagina los capitulos; se recorren todas las paginas para devolver la lista completa.
    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        var page = 1
        while (true) {
            val request = GET(
                "$baseUrl/comic/${manga.url}/chapters?lang=$lang&page=$page&limit=$CHAPTER_LIMIT&tachiyomi=true",
                headers,
            )
            val dto = client.newCall(request).awaitSuccess().use { it.parseAs(ComicKChaptersDto::class.java) }
            val batch = dto.chapters.orEmpty()
            batch.forEach { chapter ->
                val hid = chapter.hid?.takeIf { it.isNotBlank() } ?: return@forEach
                chapters += SChapter.create().apply {
                    url = hid
                    name = buildChapterName(chapter)
                    chapter_number = chapter.chap?.toFloatOrNull() ?: -1f
                    scanlator = chapter.groupName?.joinToString()
                }
            }
            val total = dto.total ?: batch.size
            if (batch.isEmpty() || chapters.size >= total || page >= MAX_CHAPTER_PAGES) break
            page++
        }
        return chapters
    }

    override fun chapterListParse(response: Response): List<SChapter> =
        throw UnsupportedOperationException("getChapterList pagina manualmente")

    override fun getChapterUrl(chapter: SChapter): String = "$WEB_URL/chapter/${chapter.url}"

    // =============================== Paginas ===============================

    override fun pageListRequest(chapter: SChapter): Request =
        GET("$baseUrl/chapter/${chapter.url}?tachiyomi=true", headers)

    public override fun pageListParse(response: Response): List<Page> {
        val images = response.parseAs(ComicKChapterPagesDto::class.java).chapter?.mdImages.orEmpty()
        return images.mapIndexedNotNull { index, image ->
            val b2key = image.b2key?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            Page(index, imageUrl = "$IMAGE_CDN/$b2key")
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    // ============================== Utilidades =============================

    private fun buildChapterName(chapter: ComicKChapterDto): String = buildString {
        chapter.vol?.takeIf { it.isNotBlank() }?.let { append("Vol. $it ") }
        val chap = chapter.chap?.takeIf { it.isNotBlank() }
        if (chap != null) append("Cap. $chap") else append("Oneshot")
        chapter.title?.takeIf { it.isNotBlank() }?.let { append(" - $it") }
    }.trim()

    private fun List<ComicKCoverDto>?.coverUrl(): String? =
        this?.firstNotNullOfOrNull { it.b2key?.takeIf { key -> key.isNotBlank() } }?.let { "$IMAGE_CDN/$it" }

    private fun <T> Response.parseAs(clazz: Class<T>): T = use {
        gson.fromJson(it.body!!.charStream(), clazz)
    }

    private companion object {
        const val WEB_URL = "https://comick.dev"
        const val IMAGE_CDN = "https://meo.comick.pictures"
        const val SEARCH_LIMIT = 30
        const val CHAPTER_LIMIT = 100
        const val MAX_CHAPTER_PAGES = 50
    }
}
