package com.mangaku.extension.manganato

import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import uy.kohesive.injekt.Injekt

class ManganatoTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    private val baseUrl = "https://www.natomanga.com"
    private fun source() = Manganato()

    private fun htmlResponse(url: String, html: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(html.toResponseBody())
        .build()

    @Test
    fun `normalizeSearchQuery imita change_alias (minusculas, guion bajo, sin simbolos)`() {
        assertEquals("mato_seihei_no_slave", source().normalizeSearchQuery("Mato Seihei no Slave!"))
        assertEquals("berserk", source().normalizeSearchQuery("Berserk"))
    }

    @Test
    fun `chaptersFromApi mapea el formato con paginacion (natomanga)`() {
        val json = """
            {"data":{"chapters":[
              {"chapter_name":"Chapter 182","chapter_slug":"chapter-182","chapter_num":182,"updated_at":"2026-06-28T00:00:00"},
              {"chapter_name":"Chapter 1","chapter_slug":"chapter-1","chapter_num":1,"updated_at":"2020-01-01T00:00:00"}
            ],"pagination":{"has_more":false}}}
        """.trimIndent()

        val chapters = source().chaptersFromApi(json, "mato-seihei-no-slave")

        assertEquals(2, chapters.size)
        assertEquals("Chapter 182", chapters[0].name)
        assertEquals("/manga/mato-seihei-no-slave/chapter-182", chapters[0].url)
        assertEquals(182f, chapters[0].chapter_number)
    }

    @Test
    fun `chaptersFromApi tolera el formato con success sin paginacion (kakalot)`() {
        val json = """
            {"success":true,"data":{"chapters":[
              {"chapter_name":"Chapter 5","chapter_slug":"chapter-5","chapter_num":5,"updated_at":"2026-01-01T00:00:00"}
            ]}}
        """.trimIndent()

        val chapters = source().chaptersFromApi(json, "some-manga")

        assertEquals(1, chapters.size)
        assertEquals("/manga/some-manga/chapter-5", chapters[0].url)
    }

    @Test
    fun `pageListParse arma las urls desde cdns y chapterImages del script`() {
        val html = """
            <html><body>
            <script>var cdns = ["https://cdn1.example.com/","https://cdn2.example.com/"];
            var chapterImages = ["images/1.webp","images/2.webp"];</script>
            </body></html>
        """.trimIndent()
        val document = Jsoup.parse(html, "$baseUrl/manga/x/chapter-1")

        val pages = source().pageListParse(document)

        assertEquals(2, pages.size)
        assertEquals("https://cdn1.example.com/images/1.webp", pages[0].imageUrl)
        assertEquals("https://cdn1.example.com/images/2.webp", pages[1].imageUrl)
    }

    @Test
    fun `pageListParse cae al HTML cuando no hay script de cdns`() {
        val html = """
            <html><body><div class="container-chapter-reader">
            <img src="https://cdn.example.com/1.jpg"/>
            <img src="https://cdn.example.com/2.jpg"/>
            </div></body></html>
        """.trimIndent()
        val document = Jsoup.parse(html, "$baseUrl/manga/x/chapter-1")

        val pages = source().pageListParse(document)

        assertEquals(2, pages.size)
        assertTrue(pages[0].imageUrl!!.endsWith("1.jpg"))
    }

    @Test
    fun `searchMangaParse extrae mangas con ruta sin dominio`() {
        val html = """
            <html><body>
            <div class="list-truyen-item-wrap">
              <a href="/manga/mato-seihei-no-slave"><img src="https://cdn.example.com/cover.jpg"/></a>
              <h3><a href="/manga/mato-seihei-no-slave">Mato Seihei no Slave</a></h3>
            </div>
            </body></html>
        """.trimIndent()

        val page = source().searchMangaParse(htmlResponse("$baseUrl/search/story/mato_seihei", html))

        assertEquals(1, page.mangas.size)
        assertEquals("Mato Seihei no Slave", page.mangas[0].title)
        assertEquals("/manga/mato-seihei-no-slave", page.mangas[0].url)
    }
}
