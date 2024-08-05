package wisp.questgiver.v2.json

import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission
import org.json.JSONObject
import wisp.questgiver.v2.*
import wisp.questgiver.wispLib.TextExtensions
import wisp.questgiver.wispLib.qgFormat

private const val BAR_EVENT = "barEvent"

/**
 * @param barEventJson eg `Global.getSettings().getMergedJSONForMod(jsonPath, modId).query("/$questName/stages/index/barEvent")`
 */
fun <S : IInteractionLogic<S>> InteractionPromptFromJson(
    barEventJson: JSONObject,
): CreateInteractionPrompt<S> {
    return {
        para {
            barEventJson
                .getString("prompt")
                .qgFormat()
        }
    }
}

/**
 * @param barEventJson eg `Global.getSettings().getMergedJSONForMod(jsonPath, modId).query("/$questName/stages/index/barEvent")`
 */
fun <S : IInteractionLogic<S>> TextToStartInteractionFromJson(
    barEventJson: JSONObject,
): TextToStartInteraction<S> {
    return {
        val highlightData = TextExtensions.getTextHighlightData(
            barEventJson
                .getString("optionText")
                .qgFormat()
        )
        BarEventLogic.Option(
            text = highlightData.newString,
            textColor = highlightData.replacements.firstOrNull()?.highlightColor,
            tooltip = barEventJson.optString("tooltip", null)
        )
    }
}