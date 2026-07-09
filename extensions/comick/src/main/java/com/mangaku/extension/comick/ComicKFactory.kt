package com.mangaku.extension.comick

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

/**
 * ComicK es multiidioma. Al estilo de Mihon, se expone una fuente por idioma: el host instancia
 * todas y el usuario elige la que quiera. La clase de entrada declarada en el AndroidManifest es
 * esta factory.
 */
class ComicKFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        ComicK(lang = "en"),
        ComicK(lang = "es"),
    )
}
