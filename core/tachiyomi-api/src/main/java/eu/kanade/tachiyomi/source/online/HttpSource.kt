package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.security.MessageDigest

/**
 * Fuente HTTP: base de casi todas las extensiones. Reimplementa la superficie de la API de Tachiyomi
 * usada por las fuentes portadas y por las extensiones cargadas de forma dinamica. El flujo es
 * siempre "construir Request -> ejecutar con [client] -> parsear Response", con metodos abiertos que
 * cada fuente sobreescribe.
 */
abstract class HttpSource : CatalogueSource {

    protected val network: NetworkHelper by injectLazy()

    abstract val baseUrl: String

    open val versionId: Int = 1

    override val id: Long by lazy { generateId(name, lang, versionId) }

    open val client: OkHttpClient by lazy { network.client }

    val headers: Headers by lazy { headersBuilder().build() }

    protected open fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", network.defaultUserAgentProvider())

    // ================================ Populares ================================

    protected abstract fun popularMangaRequest(page: Int): Request
    protected abstract fun popularMangaParse(response: Response): MangasPage

    override suspend fun getPopularManga(page: Int): MangasPage =
        client.newCall(popularMangaRequest(page)).awaitSuccess().use { popularMangaParse(it) }

    // ============================ Ultimas actualizaciones =======================

    protected open fun latestUpdatesRequest(page: Int): Request =
        throw UnsupportedOperationException("Not supported")

    protected open fun latestUpdatesParse(response: Response): MangasPage =
        throw UnsupportedOperationException("Not supported")

    override suspend fun getLatestUpdates(page: Int): MangasPage =
        client.newCall(latestUpdatesRequest(page)).awaitSuccess().use { latestUpdatesParse(it) }

    // ================================= Busqueda ================================

    protected abstract fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request
    protected abstract fun searchMangaParse(response: Response): MangasPage

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage =
        client.newCall(searchMangaRequest(page, query, filters)).awaitSuccess().use { searchMangaParse(it) }

    override fun getFilterList(): FilterList = FilterList()

    // ============================== Detalle de manga ===========================

    open fun mangaDetailsRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    abstract fun mangaDetailsParse(response: Response): SManga

    override suspend fun getMangaDetails(manga: SManga): SManga =
        client.newCall(mangaDetailsRequest(manga)).awaitSuccess().use { mangaDetailsParse(it) }

    open fun getMangaUrl(manga: SManga): String = baseUrl + manga.url

    // ================================= Capitulos ===============================

    open fun chapterListRequest(manga: SManga): Request = GET(baseUrl + manga.url, headers)

    abstract fun chapterListParse(response: Response): List<SChapter>

    override suspend fun getChapterList(manga: SManga): List<SChapter> =
        client.newCall(chapterListRequest(manga)).awaitSuccess().use { chapterListParse(it) }

    open fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    // =================================== Paginas ===============================

    open fun pageListRequest(chapter: SChapter): Request = GET(baseUrl + chapter.url, headers)

    abstract fun pageListParse(response: Response): List<Page>

    override suspend fun getPageList(chapter: SChapter): List<Page> =
        client.newCall(pageListRequest(chapter)).awaitSuccess().use { pageListParse(it) }

    abstract fun imageUrlParse(response: Response): String

    open fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headers)

    // ================================= Utilidades ==============================

    fun SManga.setUrlWithoutDomain(url: String) { this.url = getUrlWithoutDomain(url) }

    fun SChapter.setUrlWithoutDomain(url: String) { this.url = getUrlWithoutDomain(url) }

    private fun getUrlWithoutDomain(orig: String): String {
        val url = orig.toHttpUrlOrNull() ?: return orig
        return buildString {
            append(url.encodedPath)
            if (url.encodedQuery != null) append("?").append(url.encodedQuery)
            if (url.encodedFragment != null) append("#").append(url.encodedFragment)
        }
    }

    private fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).fold(0L) { acc, i -> acc or ((bytes[i].toLong() and 0xff) shl (8 * (7 - i))) } and Long.MAX_VALUE
    }
}
