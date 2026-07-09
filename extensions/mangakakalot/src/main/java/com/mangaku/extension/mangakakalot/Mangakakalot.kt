package com.mangaku.extension.mangakakalot

import eu.kanade.tachiyomi.multisrc.mangabox.MangaBox

/**
 * Mangakakalot: gran catalogo de manga en ingles, hermano de Manganato (comparten la plantilla
 * [MangaBox]). Solo fija nombre/URL/idioma. Si el dominio cae, actualizar [baseUrl].
 *
 * www.mangakakalot.gg quedo tras un challenge de Cloudflare en todo el sitio (HTTP 403 "Just a
 * moment..."); www.mangakakalove.com sirve el mismo backend sin challenge salvo en /search, donde
 * actua el CloudflareInterceptor de la app.
 */
class Mangakakalot : MangaBox() {
    override val name = "Mangakakalot"
    override val baseUrl = "https://www.mangakakalove.com"
    override val lang = "en"
}
