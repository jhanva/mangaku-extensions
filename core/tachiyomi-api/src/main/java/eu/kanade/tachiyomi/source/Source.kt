package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga

/**
 * Contrato base de una fuente en la API de Tachiyomi. Las operaciones son suspend (extensions-lib
 * 1.5+). Una fuente tiene un [id] estable (hash de nombre+lang+version en la API original) y expone
 * detalle de manga, lista de capitulos y lista de paginas.
 */
interface Source {
    val id: Long
    val name: String
    val lang: String

    suspend fun getMangaDetails(manga: SManga): SManga
    suspend fun getChapterList(manga: SManga): List<SChapter>
    suspend fun getPageList(chapter: SChapter): List<Page>
}
