package wisp.questgiver.wispLib

import wisp.questgiver.Questgiver.game
import java.util.*

/**
 * A string localization interface. Implementations choose the source of the strings (eg property file).
 */
interface IText {
    companion object {
        private val placeholderPattern = """\$\{(\w+)}""".toRegex().toPattern()
    }

    var shouldThrowExceptionOnMissingValue: Boolean

    val globalReplacementGetters: MutableMap<String, (String) -> Any?>

    /**
     * Get a value by its key and formats it with the provided substitutions.
     */
    fun getf(key: String, vararg values: Pair<String, Any?>): String

    /**
     * Get a value by its key and formats it with the provided substitutions.
     */
    fun getf(key: String, values: Map<String, Any?>): String {
        return getf(key, *values.map { it.key to it.value }.toTypedArray())
    }

    /**
     * Get a value by its key.
     */
    operator fun get(key: String): String = formatString(getf(key))

    /**
     * Formats a given string with the provided substitutions.
     * Use `$stringKey` as a placeholder for variables.
     */
    fun formatString(format: String, values: Map<String, Any?> = emptyMap()): String {
        val formatter = StringBuilder(format)
        val valueList = mutableListOf<Any>()
        val matcher = placeholderPattern.matcher(format)

        while (matcher.find()) {
            val key: String = matcher.group(1)
            val formatKey = String.format("\${%s}", key)
            val index = formatter.indexOf(formatKey)

            if (index != -1) {
                formatter.replace(index, index + formatKey.length, "%s")
                val value = if (values.containsKey(key)) {
                    // If values contains the key but the value is null, return string "null"
                    // instead of checking the global map
                    values[key] ?: "null"
                } else {
                    val function = globalReplacementGetters[key]

                    if (function == null) {
                        val errMsg = "Error: missing value for \'$key\'"
                        if (shouldThrowExceptionOnMissingValue) throw NullPointerException(errMsg)
                        else errMsg
                    } else {
                        // If the key exists, run the getter. If the getter returns null, return "null" as a string
                        function.invoke(key) ?: "null"
                    }
                }

                valueList.add(value)
            }
        }

        try {
            return String.format(formatter.toString(), *valueList.toTypedArray())
        } catch (e: IllegalFormatException) {
            throw RuntimeException("Invalid format. To get '%', use '%%' instead.", e)
        }
    }

    /**
     * Same as `Locale.setDefault(locale)`.
     */
    fun setLocale(locale: Locale) = Locale.setDefault(locale)
}

/**
 * Replace placeholders with values.
 */
fun String.qgFormat(values: Map<String, Any?> = emptyMap()) = game.text.formatString(this, values)