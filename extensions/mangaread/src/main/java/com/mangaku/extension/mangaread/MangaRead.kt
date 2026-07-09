package com.mangaku.extension.mangaread

import eu.kanade.tachiyomi.multisrc.madara.Madara

/**
 * MangaRead (mangaread.org): sitio Madara enfocado en manga (no manhwa) en ingles. Toda la logica de
 * scraping esta en la plantilla [Madara]; esta fuente solo fija nombre/URL/idioma. Anadir mas sitios
 * Madara es tan simple como replicar esta clase con otra baseUrl.
 */
class MangaRead : Madara(
    name = "MangaRead",
    baseUrl = "https://www.mangaread.org",
    lang = "en",
)
