package eu.kanade.tachiyomi.network

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Rate limit por ventana deslizante, reimplementado sobre un interceptor. Las fuentes lo aplican con
 * `client.newBuilder().rateLimit(permits, period).build()`.
 *
 * Solo se retiene el lock el tiempo minimo para calcular cuanto esperar; la espera ([Thread.sleep])
 * ocurre FUERA del lock, de modo que no se serializan todas las peticiones ni se bloquea un hilo de
 * OkHttp reteniendo el monitor. Es un interceptor bloqueante (corre en el dispatcher de OkHttp, nunca
 * en el hilo principal), lo que es correcto para esta API.
 */
fun OkHttpClient.Builder.rateLimit(
    permits: Int,
    period: Duration,
    shouldLimit: (HttpUrl) -> Boolean = { true },
): OkHttpClient.Builder = addInterceptor(RateLimitInterceptor(permits, period.inWholeMilliseconds, shouldLimit))

private class RateLimitInterceptor(
    private val permits: Int,
    private val periodMillis: Long,
    private val shouldLimit: (HttpUrl) -> Boolean,
) : Interceptor {
    private val lock = Any()
    private val timestamps = ArrayDeque<Long>(permits)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!shouldLimit(request.url)) return chain.proceed(request)

        val sleepMillis = synchronized(lock) {
            val now = System.currentTimeMillis()
            // Descarta las marcas fuera de la ventana.
            while (timestamps.isNotEmpty() && now - timestamps.first() >= periodMillis) {
                timestamps.removeFirst()
            }
            if (timestamps.size < permits) {
                timestamps.addLast(now)
                0L
            } else {
                // Hay que esperar hasta que la marca mas antigua salga de la ventana.
                val wait = periodMillis - (now - timestamps.first())
                // Reserva el turno: la peticion arrancara justo cuando expire la ventana.
                timestamps.removeFirst()
                timestamps.addLast(now + wait)
                wait.coerceAtLeast(0L)
            }
        }

        if (sleepMillis > 0L) {
            runCatching { TimeUnit.MILLISECONDS.sleep(sleepMillis) }
        }
        return chain.proceed(request)
    }
}
