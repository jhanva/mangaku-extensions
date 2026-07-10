package com.mangaku.extension.senmanga

import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.model.FilterList
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

class SenMangaTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    private fun response(url: String, json: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(json.toResponseBody())
        .build()

    @Test
    fun `directory parse mapea series y paginacion`() {
        val json = """
            {"currentPage":1,"totalPages":3,"series":[
              {"title":"ワンピース","slug":"one-piece","cover":"c.jpg","status":"Ongoing","genre":"Action"}
            ]}
        """.trimIndent()

        val page = SenManga().popularMangaParse(response("https://raw.senmanga.com/api/directory", json))

        assertEquals(1, page.mangas.size)
        assertEquals("ワンピース", page.mangas[0].title)
        assertEquals("one-piece", page.mangas[0].url)
        assertTrue(page.hasNextPage)
    }

    @Test
    fun `search request usa el parametro query de la API nueva`() {
        // El sitio se reconstruyo como SPA en 2026: /api/directory ignora "s" y filtra con "query".
        val request = SenManga().searchMangaRequest(page = 1, query = "naruto", filters = FilterList())

        assertEquals("naruto", request.url.queryParameter("query"))
        assertEquals(null, request.url.queryParameter("s"))
    }

    @Test
    fun `chapter list construye url compuesta manga-capitulo`() {
        val json = """
            {"title":"X","slug":"one-piece","chapterList":[
              {"title":"Chapter 1","url":"1","datetime":"2024-01-02T03:04:05Z"}
            ]}
        """.trimIndent()

        val chapters = SenManga().chapterListParse(response("https://raw.senmanga.com/api/manga/one-piece", json))

        assertEquals(1, chapters.size)
        assertEquals("one-piece/1", chapters[0].url)
        assertEquals("Chapter 1", chapters[0].name)
        assertTrue(chapters[0].date_upload > 0)
    }

    @Test
    fun `page list mapea urls de imagen`() {
        val json = """{"pages":["https://raw.senmanga.com/1.jpg","https://raw.senmanga.com/2.jpg"]}"""

        val pages = SenManga().pageListParse(response("https://raw.senmanga.com/api/read/one-piece/1", json))

        assertEquals(2, pages.size)
        assertEquals(0, pages[0].index)
        assertTrue(pages[1].imageUrl!!.endsWith("2.jpg"))
    }
}
