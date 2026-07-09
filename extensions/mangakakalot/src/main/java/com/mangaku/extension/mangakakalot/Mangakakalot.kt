package com.mangaku.extension.mangakakalot

import eu.kanade.tachiyomi.multisrc.mangabox.MangaBox

/**
 * Mangakakalot: gran catalogo de manga en ingles, hermano de Manganato (comparten la plantilla
 * [MangaBox]). Solo fija nombre/URL/idioma. Si el dominio cae, actualizar [baseUrl] (mirror conocido:
 * https://www.mangakakalove.com).
 */
class Mangakakalot : MangaBox() {
    override val name = "Mangakakalot"
    override val baseUrl = "https://www.mangakakalot.gg"
    override val lang = "en"
}
