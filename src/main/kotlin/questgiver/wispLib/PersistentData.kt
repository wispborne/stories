package wisp.questgiver.wispLib

import wisp.questgiver.Questgiver.MOD_PREFIX
import wisp.questgiver.Questgiver.game
import kotlin.reflect.KProperty

object PersistentDataWrapper {
    operator fun get(key: String): Any? {
        val keyWithPrefix = createPrefixedKey(key)
        return game.sector.persistentData[keyWithPrefix] as? Any?
    }

    /**
     * Automatically adds mod prefix.
     */
    operator fun set(key: String, value: Any?) {
        game.sector.persistentData[createPrefixedKey(key)] = value
    }

    fun unset(key: String) {
        game.sector.persistentData.remove(createPrefixedKey(key))
    }

    private fun createPrefixedKey(key: String) = if (key.startsWith('$')) key else "$${MOD_PREFIX}_$key"
}

class PersistentNullableData<T>(private val key: String, private val defaultValue: () -> T? = { null }) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    fun get() = game.persistentData[key] as? T ?: defaultValue().also { default -> if (default != null) set(default) }

    fun set(value: T) {
        game.persistentData[key] = value
    }
}

class PersistentData<T>(private val key: String, private val defaultValue: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return get()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        set(value)
    }

    fun get() = game.persistentData[key] as? T ?: defaultValue().also { default -> set(default) }

    fun set(value: T) {
        game.persistentData[key] = value
    }
}

class PersistentObservableData<T>(key: String, defaultValue: () -> T) : Observable<T>(defaultValue()) {
    private var innerValue: T by PersistentData(key, defaultValue)

    override var value: T
        get() {
            return innerValue
        }
        set(value) {
            innerValue = value
            observers.forEach { it.value(value) }
        }
}

class PersistentObservableNullableData<T>(key: String, defaultValue: () -> T?) : Observable<T?>(defaultValue()) {
    private var innerValue: T? by PersistentNullableData(key, defaultValue)

    override var value: T?
        get() {
            return innerValue
        }
        set(value) {
            innerValue = value
            observers.forEach { it.value(value) }
        }
}

/**
 * A map that is stored in Starsector's PersistentData.
 *
 * @param key The key of the map in PersistentData.
 * @param startingValues The default values in the map.
 */
class PersistentMapData<K, V>(private val key: String) : MutableMap<K, V> {

    private fun getMap(): MutableMap<K, V> = (game.persistentData[key] as? MutableMap<K, V>)
        ?: kotlin.run {
            game.persistentData[key] = mutableMapOf<K, V>()
            getMap()
        }

    override val size: Int
        get() = getMap().size

    override fun containsKey(key: K): Boolean = getMap().containsKey(key)

    override fun containsValue(value: V): Boolean = getMap().containsValue(value)

    override fun get(key: K): V? = getMap().get(key)

    override fun isEmpty(): Boolean = getMap().isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = getMap().entries

    override val keys: MutableSet<K>
        get() = getMap().keys

    override val values: MutableCollection<V>
        get() = getMap().values

    override fun clear() {
        game.persistentData[key] = mutableMapOf<K, V>()
    }

    override fun put(key: K, value: V): V? {
        val newMap = getMap()
        val oldValue = newMap.put(key, value)
        game.persistentData[this@PersistentMapData.key] = newMap
        return oldValue
    }

    override fun putAll(from: Map<out K, V>) {
        game.persistentData[this@PersistentMapData.key] = getMap().apply { putAll(from) }
    }

    override fun remove(key: K): V? {
        val newMap = getMap()
        val oldValue = newMap.remove(key)
        game.persistentData[this@PersistentMapData.key] = newMap
        return oldValue
    }
}