package uy.kohesive.injekt.api

/** Superficie minima de registro que consumen las extensiones al configurar Injekt. */
interface InjektRegistrar {
    fun <T : Any> addSingleton(instance: T)
    fun <T : Any> addSingletonForType(type: Class<T>, instance: T)
}

/** Recupera una dependencia registrada; espejo de `Injekt.get()` para llamadas `get<T>()`. */
inline fun <reified T : Any> InjektRegistrar.get(): T =
    uy.kohesive.injekt.Injekt.get(T::class.java)
