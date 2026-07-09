package eu.kanade.tachiyomi.network

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Ejecuta la llamada de forma asincrona y suspende hasta obtener la respuesta. Equivalente a
 * `Call.await()` de la API de Tachiyomi (basada en coroutines en extensions-lib 1.5+).
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation {
        runCatching { cancel() }
    }
}

/** Suspende y verifica que la respuesta sea 2xx, cerrando el cuerpo y lanzando si no lo es. */
suspend fun Call.awaitSuccess(): Response {
    val response = await()
    if (!response.isSuccessful) {
        response.close()
        throw IOException("HTTP error ${response.code}")
    }
    return response
}
