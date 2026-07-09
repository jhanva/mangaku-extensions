package eu.kanade.tachiyomi.source

/**
 * Una extension puede exponer varias fuentes (por ejemplo, una por idioma). Cuando la clase de
 * entrada declarada en el APK implementa [SourceFactory], el host llama a [createSources] en vez de
 * tratar la clase como una unica [Source].
 */
interface SourceFactory {
    fun createSources(): List<Source>
}
