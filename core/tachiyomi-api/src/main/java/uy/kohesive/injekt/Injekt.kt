package uy.kohesive.injekt

import uy.kohesive.injekt.api.InjektRegistrar

/**
 * Shim minimo del service locator Injekt que usan las extensiones de Tachiyomi. La app registra las
 * dependencias que las fuentes esperan (sobre todo NetworkHelper y Json/Gson) al inicializar el host
 * de extensiones; las fuentes las recuperan con [Injekt.get] o [injectLazy].
 */
object Injekt : InjektRegistrar {
    private val singletons = HashMap<Class<*>, Any>()

    override fun <T : Any> addSingleton(instance: T) {
        singletons[instance::class.java] = instance
    }

    override fun <T : Any> addSingletonForType(type: Class<T>, instance: T) {
        singletons[type] = instance
    }

    fun <T : Any> get(type: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return singletons[type] as? T
            ?: throw IllegalStateException("No hay dependencia registrada en Injekt para ${type.name}")
    }

    inline fun <reified T : Any> get(): T = get(T::class.java)
}

/** Registro perezoso, equivalente al helper `injectLazy()` de las extensiones. */
inline fun <reified T : Any> injectLazy(): Lazy<T> = lazy { Injekt.get(T::class.java) }
