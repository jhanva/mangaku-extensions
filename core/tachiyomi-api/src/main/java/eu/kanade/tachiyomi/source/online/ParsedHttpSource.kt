package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * Fuente HTTP basada en scraping con selectores CSS (Jsoup). Cubre el patron mas comun de las
 * extensiones: listas de resultados definidas por un selector de items, un selector de "siguiente
 * pagina" y un mapeo elemento -> modelo. Las subclases solo declaran selectores y mapeos.
 */
abstract class ParsedHttpSource : HttpSource() {

    protected abstract fun popularMangaSelector(): String
    protected abstract fun popularMangaFromElement(element: Element): SManga
    protected abstract fun popularMangaNextPageSelector(): String?

    override fun popularMangaParse(response: Response): MangasPage = mangasPage(
        document = response.asJsoup(),
        itemSelector = popularMangaSelector(),
        nextSelector = popularMangaNextPageSelector(),
        fromElement = ::popularMangaFromElement,
    )

    protected abstract fun searchMangaSelector(): String
    protected abstract fun searchMangaFromElement(element: Element): SManga
    protected abstract fun searchMangaNextPageSelector(): String?

    override fun searchMangaParse(response: Response): MangasPage = mangasPage(
        document = response.asJsoup(),
        itemSelector = searchMangaSelector(),
        nextSelector = searchMangaNextPageSelector(),
        fromElement = ::searchMangaFromElement,
    )

    protected open fun latestUpdatesSelector(): String = throw UnsupportedOperationException()
    protected open fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException()
    protected open fun latestUpdatesNextPageSelector(): String? = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response): MangasPage = mangasPage(
        document = response.asJsoup(),
        itemSelector = latestUpdatesSelector(),
        nextSelector = latestUpdatesNextPageSelector(),
        fromElement = ::latestUpdatesFromElement,
    )

    override fun mangaDetailsParse(response: Response): SManga = mangaDetailsParse(response.asJsoup())
    protected abstract fun mangaDetailsParse(document: Document): SManga

    protected abstract fun chapterListSelector(): String
    protected abstract fun chapterFromElement(element: Element): SChapter

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        return document.select(chapterListSelector()).map { chapterFromElement(it) }
    }

    override fun imageUrlParse(response: Response): String = imageUrlParse(response.asJsoup())
    protected open fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()

    private inline fun mangasPage(
        document: Document,
        itemSelector: String,
        nextSelector: String?,
        fromElement: (Element) -> SManga,
    ): MangasPage {
        val mangas = document.select(itemSelector).map(fromElement)
        val hasNext = nextSelector?.let { document.selectFirst(it) != null } ?: false
        return MangasPage(mangas, hasNext)
    }
}
