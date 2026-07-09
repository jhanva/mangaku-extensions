package com.mangaku.extension.inmanga

import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import uy.kohesive.injekt.Injekt

class InMangaTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    private fun source() = InManga()

    private fun response(url: String, body: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody())
        .build()

    @Test
    fun `searchMangaParse extrae titulo, ruta y portada`() {
        val html = """
            <body>
              <a href="https://inmanga.com/ver/manga/Berserk/abc-123">
                <h4 class="m0">Berserk</h4>
                <img data-src="https://cdn1.intomanga.com/cover/berserk.jpg"/>
              </a>
            </body>
        """.trimIndent()

        val page = source().searchMangaParse(response("https://inmanga.com/manga/getMangasConsultResult", html))

        assertEquals(1, page.mangas.size)
        assertEquals("Berserk", page.mangas[0].title)
        assertEquals("/ver/manga/Berserk/abc-123", page.mangas[0].url)
    }

    @Test
    fun `chapterListParse desanida el JSON de data y ordena descendente`() {
        // El campo "data" es a su vez una cadena JSON (nota el escapado de comillas).
        val json = """
            {"data":"{\"success\":true,\"result\":[
              {\"Number\":1.0,\"RegistrationDate\":\"2020-01-01\",\"Identification\":\"id-1\",\"FriendlyChapterNumber\":\"1\"},
              {\"Number\":2.0,\"RegistrationDate\":\"2020-02-01\",\"Identification\":\"id-2\",\"FriendlyChapterNumber\":\"2\"}
            ]}"}
        """.trimIndent().replace("\n", "")

        val chapters = source().chapterListParse(response("https://inmanga.com/chapter/getall", json))

        assertEquals(2, chapters.size)
        // Ordenado descendente: el capitulo 2 va primero.
        assertEquals(2f, chapters[0].chapter_number)
        assertEquals("/chapter/chapterIndexControls?identification=id-2", chapters[0].url)
        assertEquals("Capitulo 2", chapters[0].name)
    }

    @Test
    fun `chapterListParse devuelve vacio si data viene nulo`() {
        val chapters = source().chapterListParse(response("https://inmanga.com/chapter/getall", """{"data":null}"""))
        assertEquals(0, chapters.size)
    }

    @Test
    fun `pageListParse construye las urls del CDN con manga y chapter id`() {
        val html = """
            <html><body>
            <input id="ChapterIdentification" value="chap-9"/>
            <input id="MangaIdentification" value="manga-7"/>
            <img class="ImageContainer" id="page-1"/>
            <img class="ImageContainer" id="page-2"/>
            </body></html>
        """.trimIndent()

        val pages = source().pageListParse(response("https://inmanga.com/chapter/chapterIndexControls", html))

        assertEquals(2, pages.size)
        assertEquals("https://cdn1.intomanga.com/i/m/manga-7/c/chap-9/o/page-1.jpg", pages[0].imageUrl)
        assertEquals("https://cdn1.intomanga.com/i/m/manga-7/c/chap-9/o/page-2.jpg", pages[1].imageUrl)
    }
}
