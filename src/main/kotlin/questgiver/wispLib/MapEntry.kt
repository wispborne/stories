package wisp.questgiver.wispLib

import kotlin.reflect.KProperty

class MapEntry<V, V1 : V>(private val map: MutableMap<in String, V>, private val default: () -> V1) {

    /**
     * Returns the value of the property for the given object from this mutable map.
     * @param thisRef the object for which the value is requested (not used).
     * @param property the metadata for the property, used to get the name of property and lookup the value corresponding to this name in the map.
     * @return the property value.
     *
     * @throws NoSuchElementException when the map doesn't contain value for the property name and doesn't provide an implicit default (see [withDefault]).
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): V1 =
        @Suppress("UNCHECKED_CAST") (map[property.name] as? V1 ?: run {
            val value = default()
            map[property.name] = value
            value
        })

    /**
     * Stores the value of the property for the given object in this mutable map.
     * @param thisRef the object for which the value is requested (not used).
     * @param property the metadata for the property, used to get the name of property and store the value associated with that name in the map.
     * @param value the value to set.
     */
    operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        value: V
    ) {
        map[property.name] = value
    }
}

fun <K, V> MutableMap<K, V>.setDefault(default: () -> V): MutableMap<K, V> =
    this.withDefault {
        val defaultValue = default()
        this[it] = defaultValue
        defaultValue
    }