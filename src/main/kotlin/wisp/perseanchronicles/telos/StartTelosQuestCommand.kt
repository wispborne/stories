package wisp.perseanchronicles.telos

import com.fs.starfarer.api.util.Misc
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console
import wisp.perseanchronicles.dangerousGames.pt2_depths.DepthsHubMission
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.pt1_deliveryToEarth.Telos1HubMission
import wisp.perseanchronicles.telos.pt2_dart.Telos2HubMission
import wisp.perseanchronicles.telos.pt3_arrow.Telos3HubMission
import wisp.questgiver.v2.HubMissionBarEventWrapperWithoutRules

class StartTelosQuestCommand : BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (!context.isInCampaign) {
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }

        if (DepthsHubMission.state.completeDateInMillis == null) {
            Console.showMessage("You must complete the first two Karengo quests (Dragons & Depths) before starting the Telos quest.");
            return BaseCommand.CommandResult.ERROR
        }

        Telos1HubMission.state.map.clear()
        Telos2HubMission.state.map.clear()
        Telos3HubMission.state.map.clear()

        val market = Misc.findNearestLocalMarket(game.sector.playerFleet, 10000f, null)
        val wrapper = object : HubMissionBarEventWrapperWithoutRules<Telos1HubMission>(Telos1HubMission.MISSION_ID) {
            override fun createMission(): Telos1HubMission = Telos1HubMission()
        }

            wrapper.apply {
                this.shouldShowAtMarket(market)
//                this.addPromptAndOption(, memoryMap))
            }
            .mission.run { this as Telos1HubMission }
            .apply {
            if (create(market, false)) {
                accept(null, null)
            } else {
                return BaseCommand.CommandResult.ERROR
            }
        }
        return BaseCommand.CommandResult.SUCCESS
    }
}