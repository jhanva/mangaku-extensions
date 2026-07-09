package eu.kanade.tachiyomi.source

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage

/**
 * Fuente navegable: ademas del detalle/capitulos/paginas de [Source], expone descubrimiento
 * (populares, ultimas actualizaciones y busqueda con filtros).
 */
interface CatalogueSource : Source {
    val supportsLatest: Boolean

    fun getFilterList(): FilterList

    suspend fun getPopularManga(page: Int): MangasPage
    suspend fun getLatestUpdates(page: Int): MangasPage
    suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage
}
