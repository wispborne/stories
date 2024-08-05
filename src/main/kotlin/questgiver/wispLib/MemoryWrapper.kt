package wisp.questgiver.wispLib

import com.fs.starfarer.api.campaign.rules.MemoryAPI
import wisp.questgiver.Questgiver
import wisp.questgiver.Questgiver.MOD_PREFIX
import wisp.questgiver.Questgiver.game
import kotlin.reflect.KProperty

/**
 * Simpler access to [MemoryAPI]. Automatically adds your mod id (set in [Questgiver.init] and the `$`).
 */
class MemoryWrapper(private val memoryApi: MemoryAPI) {
    operator fun get(key: String): Any? {
        val keyWithPrefix = createPrefixedKey(key)
        return memoryApi[keyWithPrefix] as? Any?
    }

    /**
     * Simpler access to [MemoryAPI]. Automatically adds your mod id (set in [Questgiver.init] and the `$`).
     */
    operator fun set(key: String, value: Any?) {
        memoryApi[createPrefixedKey(key)] = value
    }

    fun set (key: String, value: Any?, duration: Float) {
        memoryApi[createPrefixedKey(key)] = value
        memoryApi.expire(createPrefixedKey(key), duration)
    }

    fun unset(key: String) {
        memoryApi.unset(createPrefixedKey(key))
    }

    private fun createPrefixedKey(key: String) = if (key.startsWith('$')) key else "$${MOD_PREFIX}_${key}"
}

class NullableMemory<T>(private val key: String?, private val defaultValue: () -> T? = { null }) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return game.memory[key ?: property.name] as? T ?: defaultValue()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        game.memory[key ?: property.name] = value
    }
}

class Memory<T>(private val key: String?, private val defaultValue: () -> T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return game.memory[key ?: property.name] as? T ?: defaultValue()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        game.memory[key ?: property.name] = value
    }
}