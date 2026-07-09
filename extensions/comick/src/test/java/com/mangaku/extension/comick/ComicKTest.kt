package com.mangaku.extension.comick

import eu.kanade.tachiyomi.network.NetworkHelper
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

class ComicKTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    private fun source() = ComicK(lang = "en")

    private fun response(url: String, json: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(json.toResponseBody())
        .build()

    @Test
    fun `search parse mapea hid y portada del CDN`() {
        val json = """
            [
              {"hid":"abc","slug":"one-piece","title":"One Piece","md_covers":[{"b2key":"cover.jpg"}]},
              {"hid":"","title":"sin hid"}
            ]
        """.trimIndent()

        val page = source().searchMangaParse(response("https://api.comick.io/v1.0/search", json))

        assertEquals(1, page.mangas.size)
        assertEquals("abc", page.mangas[0].url)
        assertEquals("One Piece", page.mangas[0].title)
        assertEquals("https://meo.comick.pictures/cover.jpg", page.mangas[0].thumbnail_url)
    }

    @Test
    fun `detail parse mapea estado y generos`() {
        val json = """
            {"comic":{"hid":"abc","title":"Berserk","desc":"dark","status":2,
              "md_covers":[{"b2key":"c.jpg"}],
              "md_comic_md_genres":[{"md_genres":{"name":"Seinen"}},{"md_genres":{"name":"Action"}}]}}
        """.trimIndent()

        val manga = source().mangaDetailsParse(response("https://api.comick.io/comic/abc", json))

        assertEquals("Berserk", manga.title)
        assertEquals(SManga.COMPLETED, manga.status)
        assertEquals("Seinen, Action", manga.genre)
    }

    @Test
    fun `page list construye urls del CDN por b2key`() {
        val json = """{"chapter":{"hid":"ch1","md_images":[{"b2key":"1.jpg"},{"b2key":"2.jpg"}]}}"""

        val pages = source().pageListParse(response("https://api.comick.io/chapter/ch1", json))

        assertEquals(2, pages.size)
        assertEquals("https://meo.comick.pictures/1.jpg", pages[0].imageUrl)
        assertTrue(pages[1].imageUrl!!.endsWith("2.jpg"))
    }

    @Test
    fun `las peticiones usan el dominio nuevo api-comick-dev`() {
        // comick.io responde 301 a comick.dev PERDIENDO el subdominio "api.", asi que la peticion
        // redirigida acaba en el frontend web y devuelve 404. La API debe consultarse directa.
        val stub = SManga.create().apply { url = "abc" }

        val request = source().mangaDetailsRequest(stub)

        assertTrue(
            "la API vive en api.comick.dev, no en ${request.url}",
            request.url.toString().startsWith("https://api.comick.dev/"),
        )
    }

    @Test
    fun `las urls publicas de manga apuntan al dominio nuevo comick-dev`() {
        val manga = SManga.create().apply { url = "abc" }

        assertEquals("https://comick.dev/comic/abc", source().getMangaUrl(manga))
    }

    @Test
    fun `factory expone una fuente por idioma`() {
        val sources = ComicKFactory().createSources()
        assertEquals(2, sources.size)
        assertEquals(setOf("en", "es"), sources.map { it.lang }.toSet())
    }
}
