package wisp.questgiver.wispLib

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignUIAPI
import java.awt.Color

class CrashReporter(private val modName: String, private val modAuthor: String?, private val game: ServiceLocator) {
    /**
     * Originally created by Sundog in
     * [Starship Legends](https://bitbucket.org/Nate_NBJ/starship-legends/src/default/src/starship_legends/ModPlugin.java)
     */
    fun reportCrash(throwable: Throwable): Boolean {
        try {
            val message = "$modName encountered an error!\nPlease let ${modAuthor ?: "the mod author"} know."

            game.logger.e(throwable)

            if (game.combatEngine != null && Global.getCurrentState() === GameState.COMBAT) {
                game.combatEngine?.combatUI?.addMessage(1, Color.ORANGE, throwable.message)
                game.combatEngine?.combatUI?.addMessage(2, Color.RED, message)
            } else if (game.sector != null && Global.getCurrentState() === GameState.CAMPAIGN) {
                val ui: CampaignUIAPI = game.sector.campaignUI
                ui.addMessage(message, Color.RED)
                ui.addMessage(throwable.message, Color.ORANGE)
                ui.showConfirmDialog(message + "\n\n" + throwable.message, "Ok", null, null, null)

                if (ui.currentInteractionDialog != null) ui.currentInteractionDialog.dismiss()
            } else return false
            return true
        } catch (e: Exception) {
            return false
        }
    }
}