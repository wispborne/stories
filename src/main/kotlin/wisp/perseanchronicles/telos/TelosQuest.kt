package wisp.perseanchronicles.telos

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.pt1_deliveryToEarth.Telos_Stage1_BarEventCreator
import wisp.questgiver.AutoQuestFacilitator
import wisp.questgiver.InteractionDefinition
import wisp.questgiver.Questgiver
import wisp.questgiver.calculateCreditReward
import wisp.questgiver.wispLib.PersistentData
import wisp.questgiver.wispLib.PersistentMapData
import wisp.questgiver.wispLib.Text

object TelosQuest : AutoQuestFacilitator(
    stageBackingField = PersistentData(key = "telosStage", defaultValue = { Stage.NotStarted }),
    autoBarEventInfo = AutoBarEventInfo(
        barEventCreator = Telos_Stage1_BarEventCreator(),
        shouldGenerateBarEvent = { true },
        shouldOfferFromMarket = { market ->
            true
//            market.factionId.toLowerCase() in listOf(Factions.INDEPENDENT.toLowerCase())
//                    && market.starSystem != null // No prism freeport
//                    && market.size > 3
//                    && TelosQuest.state.destPlanet != null
        }),
    autoIntelInfo = null
//    AutoIntelInfo(TelosIntel::class.java) {
//        TelosIntel(TelosQuest.state.startLocation, TelosQuest.state.destPlanet)
//    }
) {
    val REWARD_CREDITS: Float
        get() = Questgiver.calculateCreditReward(state.startLocation, state.destPlanet, scaling = 1.3f)

    //    val icon = InteractionDefinition.Portrait(category = "wisp_perseanchronicles_telos", id = "davidRengel")
    val background = InteractionDefinition.Illustration(category = "wisp_perseanchronicles_telos", id = "background")

    val state = State(PersistentMapData<String, Any?>(key = "telosState").withDefault { null })

    class State(val map: MutableMap<String, Any?>) {
        /**
         * In millis.
         */
        var startDate: Long? by map
        var startLocation: SectorEntityToken? by map
        var destPlanet: SectorEntityToken? by map
        var completeDateInMillis: Long? by map
        var secretCompleteDateInMillis: Long? by map

        val destSystem: StarSystemAPI?
            get() = destPlanet?.starSystem
    }

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["telosCredits"] = { Misc.getDGSCredits(REWARD_CREDITS) }
        text.globalReplacementGetters["telosDestPlanet"] = { state.destPlanet?.name }
        text.globalReplacementGetters["telosDestSystem"] = { state.destSystem?.name }
        text.globalReplacementGetters["telosStarName"] = { state.destPlanet?.starSystem?.star?.name }
    }

    override fun regenerateQuest(interactionTarget: SectorEntityToken, market: MarketAPI?) {
        state.startLocation = interactionTarget

    }

    fun start(startLocation: SectorEntityToken) {
        game.logger.i { "Telos start location set to ${startLocation.fullName} in ${startLocation.starSystem.baseName}" }
        stage = Stage.GoToPlanet
        state.startDate = game.sector.clock.timestamp
    }

    fun complete() {
        stage = Stage.Completed
        state.completeDateInMillis = game.sector.clock.timestamp

        game.sector.playerFleet.cargo.credits.add(REWARD_CREDITS)
    }

    fun restartQuest() {
        game.logger.i { "Restarting Telos quest." }

        state.map.clear()
        stage = Stage.NotStarted
    }

    abstract class Stage(progress: Progress) : AutoQuestFacilitator.Stage(progress) {
        object NotStarted : Stage(Progress.NotStarted)
        object GoToPlanet : Stage(Progress.InProgress)
        object Completed : Stage(Progress.Completed)
        object CompletedSecret : Stage(Progress.Completed)
    }
}