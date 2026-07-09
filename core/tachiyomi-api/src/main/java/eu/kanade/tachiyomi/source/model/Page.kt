package eu.kanade.tachiyomi.source.model

/**
 * Pagina de un capitulo. En la API original hereda de ProgressListener; aqui basta con la
 * informacion que consumen las fuentes: indice, url de la pagina y url de imagen resuelta.
 */
open class Page(
    val index: Int,
    val url: String = "",
    var imageUrl: String? = null,
) {
    val number: Int get() = index + 1
}
