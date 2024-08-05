package wisp.questgiver

import wisp.questgiver.wispLib.Text

interface QuestFacilitator {

    /**
     * An idempotent method to configure `game.text.globalReplacementGetters`.
     *
     * Usage:
     * ```kt
     * override fun updateTextReplacements(text: Text) {
     *   text.globalReplacementGetters["depthsSourcePlanet"] = { startingPlanet?.name }
     *   text.globalReplacementGetters["depthsDestPlanet"] = { destPlanet?.name }
     * }
     * ```
     */
    fun updateTextReplacements(text: Text)
}