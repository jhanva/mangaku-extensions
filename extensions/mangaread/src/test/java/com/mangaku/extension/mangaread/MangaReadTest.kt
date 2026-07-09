package com.mangaku.extension.mangaread

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import uy.kohesive.injekt.Injekt

/**
 * Valida el parseo de la plantilla Madara con HTML de fixtures (estructura real de mangaread.org),
 * sin red. Expone los metodos protegidos via una subclase local.
 */
class MangaReadTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    /** Subclase que expone los parse protegidos para poder testearlos. */
    private class TestMangaRead : eu.kanade.tachiyomi.multisrc.madara.Madara("MangaRead", "https://www.mangaread.org", "en") {
        fun popular(resp: Response) = popularMangaParse(resp)
        fun details(resp: Response) = mangaDetailsParse(resp)
        fun pages(resp: Response) = pageListParse(resp)
        fun chapters(resp: Response) = chapterListParse(resp)
        fun pageRequest(chapter: SChapter) = pageListRequest(chapter)
    }

    private fun response(url: String, html: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(html.toResponseBody())
        .build()

    @Test
    fun `popular parse extrae titulo, url y portada lazy-load`() {
        val html = """
            <html><body>
              <div class="page-item-detail">
                <div class="post-title"><h3><a href="https://www.mangaread.org/manga/berserk/">Berserk</a></h3></div>
                <img data-src="https://www.mangaread.org/cover/berserk.jpg" src="lazy.gif"/>
              </div>
            </body></html>
        """.trimIndent()

        val page = TestMangaRead().popular(response("https://www.mangaread.org/manga/?m_orderby=views", html))

        assertEquals(1, page.mangas.size)
        assertEquals("Berserk", page.mangas[0].title)
        assertEquals("/manga/berserk/", page.mangas[0].url)
        assertEquals("https://www.mangaread.org/cover/berserk.jpg", page.mangas[0].thumbnail_url)
    }

    @Test
    fun `details parse mapea estado ongoing y generos`() {
        val html = """
            <html><body>
              <div class="post-title"><h1>One Piece</h1></div>
              <div class="author-content"><a>Oda</a></div>
              <div class="summary_image"><img src="https://www.mangaread.org/op.jpg"/></div>
              <div class="summary-heading">Status</div><div class="summary-content">OnGoing</div>
              <div class="genres-content"><a>Action</a><a>Adventure</a></div>
              <div class="description-summary"><div class="summary__content">Pirates.</div></div>
            </body></html>
        """.trimIndent()

        val manga = TestMangaRead().details(response("https://www.mangaread.org/manga/one-piece/", html))

        assertEquals("One Piece", manga.title)
        assertEquals(SManga.ONGOING, manga.status)
        assertTrue(manga.genre!!.contains("Action"))
    }

    @Test
    fun `chapter list guarda la url sin dominio para que baseUrl + url sea valida`() {
        val html = """
            <html><body>
              <ul>
                <li class="wp-manga-chapter">
                  <a href="https://www.mangaread.org/manga/berserk/chapter-2/">Chapter 2</a>
                  <span class="chapter-release-date">July 1, 2026</span>
                </li>
                <li class="wp-manga-chapter">
                  <a href="https://www.mangaread.org/manga/berserk/chapter-1/">Chapter 1</a>
                </li>
              </ul>
            </body></html>
        """.trimIndent()

        val chapters = TestMangaRead().chapters(response("https://www.mangaread.org/manga/berserk/", html))

        assertEquals(2, chapters.size)
        assertEquals("Chapter 2", chapters[0].name)
        // La URL guardada NO debe llevar dominio: HttpSource antepone baseUrl al pedir las paginas
        // (una URL absoluta acabaria en "https://www.mangaread.orghttps://..." y un fallo de DNS).
        assertEquals("/manga/berserk/chapter-2/?style=list", chapters[0].url)
    }

    @Test
    fun `pageListRequest tolera capitulos antiguos guardados con url absoluta`() {
        val legacy = SChapter.create().apply {
            url = "https://www.mangaread.org/manga/berserk/chapter-1/?style=list"
        }
        val relative = SChapter.create().apply { url = "/manga/berserk/chapter-1/?style=list" }

        val source = TestMangaRead()

        assertEquals(
            "https://www.mangaread.org/manga/berserk/chapter-1/?style=list",
            source.pageRequest(legacy).url.toString(),
        )
        assertEquals(
            "https://www.mangaread.org/manga/berserk/chapter-1/?style=list",
            source.pageRequest(relative).url.toString(),
        )
    }

    @Test
    fun `page list tolera src con saltos de linea como sirve mangaread`() {
        // mangaread.org emite el atributo src con tabs y saltos de linea alrededor de la URL.
        val html = "<html><body><div class=\"reading-content\">" +
            "<div class=\"page-break no-gaps\"><img id=\"image-0\" src=\"\t\n\thttps://www.mangaread.org/wp-content/uploads/WP-manga/data/x/0.jpg\" class=\"wp-manga-chapter-img\"></div>" +
            "</div></body></html>"

        val pages = TestMangaRead().pages(response("https://www.mangaread.org/manga/x/ch-1/?style=list", html))

        assertEquals(1, pages.size)
        assertEquals(
            "https://www.mangaread.org/wp-content/uploads/WP-manga/data/x/0.jpg",
            pages[0].imageUrl,
        )
    }

    @Test
    fun `page list parse toma imagenes del reading-content`() {
        val html = """
            <html><body>
              <div class="reading-content">
                <div class="page-break"><img data-src="https://cdn.mangaread.org/1.jpg" src="l.gif"/></div>
                <div class="page-break"><img data-src="https://cdn.mangaread.org/2.jpg" src="l.gif"/></div>
              </div>
            </body></html>
        """.trimIndent()

        val pages: List<Page> = TestMangaRead().pages(response("https://www.mangaread.org/manga/x/ch-1/", html))

        assertEquals(2, pages.size)
        assertEquals("https://cdn.mangaread.org/1.jpg", pages[0].imageUrl)
        assertTrue(pages[1].imageUrl!!.endsWith("2.jpg"))
    }
}
