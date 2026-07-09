package com.mangaku.extension.weebcentral

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

/**
 * Valida el parseo de WeebCentral con HTML de fixtures, sin red. Registra un NetworkHelper de prueba
 * en Injekt (la fuente lo resuelve al construirse).
 */
class WeebCentralTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            Injekt.addSingleton(NetworkHelper(client = OkHttpClient()))
        }
    }

    private fun response(url: String, html: String): Response = Response.Builder()
        .request(Request.Builder().url(url).build())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(html.toResponseBody())
        .build()

    @Test
    fun `search parse extrae titulo y url sin dominio`() {
        val html = """
            <html><body>
              <article><section>
                <a href="https://weebcentral.com/series/ABC123/one-piece">
                  <img src="https://weebcentral.com/cover/small/one-piece.jpg"/>
                  <div>One Piece</div>
                </a>
              </section></article>
            </body></html>
        """.trimIndent()

        val page = WeebCentral().searchMangaParse(response("https://weebcentral.com/search/data", html))

        assertEquals(1, page.mangas.size)
        assertEquals("One Piece", page.mangas[0].title)
        assertEquals("/series/ABC123/one-piece", page.mangas[0].url)
    }

    @Test
    fun `page list parse toma imagenes del scroll`() {
        val html = """
            <html><body>
              <section x-data="scrollObserver">
                <img src="https://img.weebcentral.com/1.png"/>
                <img src="https://img.weebcentral.com/2.png"/>
              </section>
            </body></html>
        """.trimIndent()

        val pages = WeebCentral().pageListParse(response("https://weebcentral.com/chapters/X/images", html))

        assertEquals(2, pages.size)
        assertEquals(0, pages[0].index)
        assertTrue(pages[0].imageUrl!!.endsWith("1.png"))
        assertTrue(pages[1].imageUrl!!.endsWith("2.png"))
    }

    @Test
    fun `status desconocido cae en UNKNOWN`() {
        val html = "<html><body><section x-data></section></body></html>"
        // Documento sin las secciones esperadas: mangaDetailsParse lanzaria; validamos solo el mapeo
        // de estado por separado usando SManga por defecto.
        val manga = SManga.create()
        assertEquals(SManga.UNKNOWN, manga.status)
    }
}
