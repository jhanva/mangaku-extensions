package com.mangaku.extension.manganato

import eu.kanade.tachiyomi.multisrc.mangabox.MangaBox

/**
 * Manganato: uno de los mayores catalogos de manga en ingles. Toda la logica de scraping/API esta en
 * la plantilla [MangaBox]; esta fuente solo fija nombre/URL/idioma. El sitio migra de dominio con
 * frecuencia (natomanga, nelomanga, manganato.gg...); si cae, basta con actualizar [baseUrl].
 */
class Manganato : MangaBox() {
    override val name = "Manganato"
    override val baseUrl = "https://www.natomanga.com"
    override val lang = "en"
}
