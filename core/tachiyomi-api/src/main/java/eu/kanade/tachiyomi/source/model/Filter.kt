package eu.kanade.tachiyomi.source.model

/**
 * Jerarquia de filtros de catalogo de la API de Tachiyomi. Las fuentes definen sus filtros
 * extendiendo estas clases; la app puede inspeccionar el estado para construir la UI o, de momento,
 * usar los valores por defecto.
 */
sealed class Filter<T>(val name: String, var state: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Filter<*>
        return name == other.name && state == other.state
    }

    override fun hashCode(): Int = name.hashCode() + 31 * state.hashCode()

    abstract class Header(name: String) : Filter<Any?>(name, null)
    abstract class Separator(name: String = "") : Filter<Any?>(name, null)
    abstract class Select<V>(name: String, val values: Array<V>, state: Int = 0) : Filter<Int>(name, state)
    abstract class Text(name: String, state: String = "") : Filter<String>(name, state)
    abstract class CheckBox(name: String, state: Boolean = false) : Filter<Boolean>(name, state)
    abstract class TriState(name: String, state: Int = STATE_IGNORE) : Filter<Int>(name, state) {
        fun isIgnored() = state == STATE_IGNORE
        fun isIncluded() = state == STATE_INCLUDE
        fun isExcluded() = state == STATE_EXCLUDE

        companion object {
            const val STATE_IGNORE = 0
            const val STATE_INCLUDE = 1
            const val STATE_EXCLUDE = 2
        }
    }

    abstract class Group<V>(name: String, val filters: List<V>) : Filter<List<V>>(name, filters)
    abstract class Sort(
        name: String,
        val values: Array<String>,
        state: Selection? = null,
    ) : Filter<Sort.Selection?>(name, state) {
        data class Selection(val index: Int, val ascending: Boolean)
    }
}
