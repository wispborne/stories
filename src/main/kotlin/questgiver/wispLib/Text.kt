package wisp.questgiver.wispLib

import java.util.*

/**
 * A string localization class. Loads strings from a [ResourceBundle] and formats them with provided/configured substitutions.
 * Chooses the correct resource bundle based on the current [Locale].
 *
 * @param resourceBundle The [ResourceBundle] from which to load strings.
 * @param shouldThrowExceptionOnMissingValue Whether a missing value should throw an exception or merely print an error.
 * @param globalReplacementGetters A map of key-value pairs that should be read whenever getting a string by key.
 *  The value is a function that returns a key; this is convenient for getting strings that may change (such as player name)
 *  without needing to manually update the map.
 *
 */
class Text(
    resourceBundles: List<ResourceBundle>,
    override var shouldThrowExceptionOnMissingValue: Boolean = true,
    override val globalReplacementGetters: MutableMap<String, (String) -> Any?> = mutableMapOf()
) : IText {

    val resourceBundles: MutableList<ResourceBundle> = ObservableList(resourceBundles.distinct().toMutableList())
        .apply {
            this.addObserver { _, arg ->
                resourceBundle = AggregateResourceBundle((arg as List<ResourceBundle>).distinct())
            }
        }

    private var resourceBundle = AggregateResourceBundle(resourceBundles)

    /**
     * Get a value by its key and formats it with the provided substitutions.
     */
    override fun getf(key: String, vararg values: Pair<String, Any?>): String {
        return formatString(resourceBundle.getString(key), values.toMap())
    }
}