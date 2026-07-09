package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient

/**
 * Provee los clientes OkHttp que usan las fuentes. La app registra una instancia en Injekt al
 * arrancar el host de extensiones; el [client] normal y el [cloudflareClient] comparten el mismo
 * OkHttpClient base con rate limiting y reintentos (ver core:network). El interceptor de Cloudflare
 * via WebView puede engancharse aqui mas adelante sin tocar las fuentes.
 */
class NetworkHelper(
    val client: OkHttpClient,
    val cloudflareClient: OkHttpClient = client,
    private val userAgent: String = DEFAULT_USER_AGENT,
) {
    fun defaultUserAgentProvider(): String = userAgent

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
}
