package wisp.questgiver.wispLib

import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.apache.log4j.Logger
import wisp.questgiver.wispLib.TextExtensionsConstants.italicsTag
import java.awt.Color

private object TextExtensionsConstants {
    const val startTag = "=="
    const val endTag = "=="
    val highlightRegex = """$startTag(.*?)$endTag""".toRegex(RegexOption.DOT_MATCHES_ALL)

    val italicsTag = "__"
    // BIG NOTE: We can only italicize an entire paragraph, not just a few words.
    val italicsRegexAlt = """$italicsTag(.*?)$italicsTag""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Faction text color. `$f:pirates{text goes here}`
     * Group 1 is the faction id. Group 2 is the opening `{`, whose position can be used in a call to [textInsideSurroundingChars].
     */
    val factionColorPattern = """\$${'f'}:([^\\]+?)(\{).+?[^\\]}""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Custom text color. `$c:#FFFFFF{white text goes here}`
     * Group 1 is the hex/color code. Group 2 is the opening `{`, whose position can be used in a call to [textInsideSurroundingChars].
     */
    val customColorPattern = """\$${'c'}:([^\\]+?)(\{).+?[^\\]}""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Custom text color variable. `$cv:varname{text goes here}`
     */
    val customColorVariablePattern = """\$${"cv"}:([^\\]+?)(\{).+?[^\\]}""".toRegex(RegexOption.DOT_MATCHES_ALL)
}

object ColorVariables {
    val colors: MutableMap<String, Color> = mutableMapOf()
}

/**
 * Calls `TextPanelAPI.addPara` after looking for and adding highlighting.
 *
 * Does not replace variables, such as `PLAYER_NAME`; set up and use [IText] to read strings from file with variables replaced,
 * then use this method to add them to the UI with highlighting.
 *
 * Supported:
 * - Highlight using `==text==`.
 * - Faction color using `$f:factionId{text goes here}`.
 * - Color code (hex) using `$c:#FFFFFF{text goes here}`.
 *
 * @param textColor The non-highlight text color.
 * @param highlightColor The typical highlight color.
 * @param stringMaker A function that returns a string with placeholder variables replaced.
 */
fun TextPanelAPI.addPara(
    textColor: Color = Misc.getTextColor(),
    highlightColor: Color = Misc.getHighlightColor(),
    stringMaker: ParagraphText.() -> String
): LabelAPI? {
    val string = stringMaker(ParagraphText)
    val isItalics = TextExtensionsConstants.italicsRegexAlt.containsMatchIn(string)
    val stringAfterItalics = if (isItalics) string.replace(italicsTag, "") else string
    val hlDatas = TextExtensions.getTextHighlightData(stringAfterItalics, highlightColor)

    return this.addPara(hlDatas.newString, textColor)
        .also {
            it.setHighlightColors(*hlDatas.replacements.map { it.highlightColor }.toTypedArray())
            it.setHighlight(*hlDatas.replacements.map { it.replacement }.toTypedArray())
            if (isItalics) it.italicize()
        }
}

fun TooltipMakerAPI.addPara(
    padding: Float = 10f,
    textColor: Color = Misc.getTextColor(),
    highlightColor: Color = Misc.getHighlightColor(),
    stringMaker: ParagraphText.() -> String
): LabelAPI? {
    val string = stringMaker(ParagraphText)
    val hlDatas = TextExtensions.getTextHighlightData(string, highlightColor)
    val isItalics = TextExtensionsConstants.italicsRegexAlt.containsMatchIn(string)

    return this.addPara(
        hlDatas.newString,
        textColor,
        padding,
    )
        .also {
            it.setHighlightColors(*hlDatas.replacements.map { it.highlightColor }.toTypedArray())
            it.setHighlight(*hlDatas.replacements.map { it.replacement }.toTypedArray())
            if (isItalics) it.italicize()
        }
}

object TextExtensions {
    /**
     * Extracts any highlighting, faction colors, and custom colors from the string and returns a [TextHighlightData] object.
     */
    fun getTextHighlightData(
        string: String,
        defaultHighlightColor: Color = Misc.getHighlightColor()
    ): TextHighlightData {
        fun getPositionOfOpeningBracket(matchResult: MatchResult) =
            string.substring(matchResult.groups[2]!!.range.first)

        val highlights = TextExtensionsConstants.highlightRegex.findAll(string)
            .map {
                TextHighlightData.Replacements(
                    indices = it.range,
                    textToReplace = it.value,
                    replacement = it.groupValues[1],
                    highlightColor = defaultHighlightColor
                )
            }

        val factionColors = TextExtensionsConstants.factionColorPattern.findAll(string)
            .map {
                TextHighlightData.Replacements(
                    indices = it.range,
                    textToReplace = it.value,
                    replacement = getPositionOfOpeningBracket(it)
                        .textInsideSurroundingChars(openChar = '{', closeChar = '}'),
                    highlightColor = StringAutocorrect.findBestFactionMatch(it.groupValues[1])?.color
                        ?: defaultHighlightColor
                )
            }

        val customColors = TextExtensionsConstants.customColorPattern.findAll(string)
            .map {
                TextHighlightData.Replacements(
                    indices = it.range,
                    textToReplace = it.value,
                    replacement = getPositionOfOpeningBracket(it)
                        .textInsideSurroundingChars(openChar = '{', closeChar = '}'),
                    highlightColor = Color.decode(it.groupValues[1])
                        ?: defaultHighlightColor
                )
            }

        val customColorVariables = TextExtensionsConstants.customColorVariablePattern.findAll(string)
            .map {
                TextHighlightData.Replacements(
                    indices = it.range,
                    textToReplace = it.value,
                    replacement = getPositionOfOpeningBracket(it)
                        .textInsideSurroundingChars(openChar = '{', closeChar = '}'),
                    highlightColor = ColorVariables.colors.getOrElse(it.groupValues[1]) {
                        Logger.getLogger("TextExtensions")
                            .warn("No color found for color variable ${it.groupValues[1]}. Displaying it in red instead.")
                        Color.RED
                    }
                        ?: defaultHighlightColor
                )
            }
        return highlights
            .plus(factionColors)
            .plus(customColors)
            .plus(customColorVariables)
            .sortedBy { it.indices.first }
            .toList()
            .let { hlDatas ->
                TextHighlightData(
                    originalString = string,
                    newString = hlDatas.fold(string) { str, hlData ->
                        str.replace(hlData.textToReplace, hlData.replacement)
                    },
                    replacements = hlDatas
                )
            }
    }

}

data class TextHighlightData(
    val originalString: String,
    val newString: String,
    val replacements: List<Replacements>
) {
    data class Replacements(
        val indices: IntRange,
        val textToReplace: String,
        val replacement: String,
        val highlightColor: Color
    )
}


object ParagraphText {
    fun highlight(string: String) = "${TextExtensionsConstants.startTag}$string${TextExtensionsConstants.endTag}"
    fun mark(string: String) = highlight(string)
}

object Padding {
    /**
     * The amount of padding used on the intel description panel (on the right side).
     */
    const val DESCRIPTION_PANEL = 10f

    /**
     * The amount of padding used to display intel subtitles (left side of intel panel, underneath the intel name).
     */
    const val SUBTITLE = 3f
}