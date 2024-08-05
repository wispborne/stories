package wisp.questgiver.wispLib

import java.util.*

class AggregateResourceBundle(val resourceBundles: List<ResourceBundle>) : ResourceBundle() {
    private val keys: List<String> = resourceBundles.flatMap { it.keys.toList() }

    override fun handleGetObject(key: String): Any {
        resourceBundles.forEach {
            if (it.containsKey(key))
                return it.getObject(key)
        }

        throw MissingResourceException(
            "Can't find resource for bundle ${this.javaClass.name}, key $key",
            this.javaClass.name,
            key
        )
    }

    override fun getKeys(): Enumeration<String> = Collections.enumeration(keys)
}