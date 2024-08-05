package wisp.perseanchronicles.telos.pt1_deliveryToEarth

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.json.JSONObject
import org.lwjgl.util.vector.Vector2f
import wisp.perseanchronicles.MOD_ID
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.game
import wisp.perseanchronicles.isOkForQuest
import wisp.perseanchronicles.telos.TelosCommon
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.QGHubMissionWithBarEvent
import wisp.questgiver.v2.json.optQuery
import wisp.questgiver.v2.json.query
import wisp.questgiver.v2.spriteName
import wisp.questgiver.wispLib.*
import java.awt.Color
import java.util.*

/**
 * The first part of the Telos arc.
 */
class Telos1HubMission : QGHubMissionWithBarEvent(MISSION_ID) {
    companion object {
        val MISSION_ID = "telosPt1"

        var part1Json: JSONObject = TelosCommon.readJson()
            .query("/$MOD_ID/telos/part1_deliveryToEarth")
            private set

        /**
         *  Static state for this mission.
         */
        val state = State(PersistentMapData<String, Any?>(key = "telosPt1State").withDefault { null })
        val tags = setOf(Tags.INTEL_STORY, Tags.INTEL_ACCEPTED)
    }

    /**
     * State object for this mission. Saved in [MemoryAPI].
     */
    class State(val map: MutableMap<String, Any?>) {
        var seed: Random? by map
        var startDateMillis: Long? by map
        var startLocation: SectorEntityToken? by map
        var karengoPlanet: SectorEntityToken? by map
        var completeDateInMillis: Long? by map

        val karengoSystem: StarSystemAPI?
            get() = karengoPlanet?.starSystem
    }

    init {
        missionId = MISSION_ID
    }

    /**
     * Show if we haven't started this one yet.
     * Show at markets that are independent, size 4+, and not hyperspace.
     */
    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        return state.startDateMillis == null
                && market?.isOkForQuest() == true
                && market.factionId in listOf(Factions.INDEPENDENT)
                && market.size >= 4
    }

    override fun onGameLoad(isNewGame: Boolean) {
        super.onGameLoad(isNewGame)

        if (isDevMode())
            part1Json = TelosCommon.readJson()
                .query("/$MOD_ID/telos/part1_deliveryToEarth")
    }

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["telosCredits"] = { Misc.getDGSCredits(creditsReward.toFloat()) }
        text.globalReplacementGetters["telosPt1Stg1DestPlanet"] = { state.karengoPlanet?.name }
        text.globalReplacementGetters["telosPt1Stg1DestSystem"] = { state.karengoSystem?.baseName }
        text.globalReplacementGetters["telosStarName"] = { state.karengoPlanet?.starSystem?.star?.name }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        // Ignore warning, there are two overrides and it's complaining about just one of them.
        @Suppress("ABSTRACT_SUPER_CALL_WARNING")
        super.create(createdAt, barEvent)
        state.seed = genRandom

        startingStage = Stage.GoToSectorEdge
        setSuccessStage(Stage.Completed)
        setAbandonStage(Stage.Abandoned)

        name = part1Json.optQuery("/strings/title")
        setGiverFaction(PerseanChroniclesNPCs.kellyMcDonald.faction.id) // Rep reward.
        personOverride = PerseanChroniclesNPCs.kellyMcDonald // Shows on intel, needed for rep reward or else crash.

        setIconName(IInteractionLogic.Portrait(category = "wisp_perseanchronicles_telos", id = "intel").spriteName(game))

        state.startLocation = createdAt?.primaryEntity

        state.karengoPlanet = SystemFinder(includeHiddenSystems = false)
            .requireSystemOnFringeOfSector()
            .requireSystemHasAtLeastNumJumpPoints(min = 1)
            .requirePlanetNotGasGiant()
            .requirePlanetNotStar()
            .preferMarketConditions(ReqMode.ALL, Conditions.HABITABLE)
            .preferEntityUndiscovered()
//            .preferPlanetWithRuins()
            // If we're in Perseus Arm and up faces in direction of galactic spin, Sol is to bottom-left.
            .preferSystemInDirectionFrom(Vector2f(0f, 0f), 220f, 45f)
            .preferSystemNotPulsar()
            .preferSystemTags(ReqMode.NOT_ANY, Tags.THEME_REMNANT, Tags.THEME_UNSAFE)
            .pickPlanet()
            ?: kotlin.run { game.logger.w { "Unable to find a planet for ${this.name}." }; return false }
        val planet = state.karengoPlanet ?: return false

        setRewardMult((planet.distanceFromPlayerInHyperspace / 20f).coerceAtLeast(1f))
        setCreditReward(CreditReward.VERY_HIGH) // 95k ish + distance bonus, we want the player to take this.

        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.acceptImpl(dialog, memoryMap)

        val startLocation = dialog?.interactionTarget
            ?: Misc.findNearestLocalMarket(game.sector.playerFleet, 1000f) { true }?.preferredConnectedEntity
            ?: game.sector.starSystems.first { it.star != null }.star

        state.startLocation = startLocation
        game.logger.i { "${this.name} start location set to ${startLocation.fullName} in ${startLocation.starSystem.baseName}" }
        state.startDateMillis = game.sector.clock.timestamp

        // Sets the system as the map objective.
        makeImportant(state.karengoSystem?.hyperspaceAnchor, null, Stage.GoToSectorEdge)
        makePrimaryObjective(state.karengoSystem?.hyperspaceAnchor)

        // Complete Part 1, show conclusion dialog.
        trigger {
            beginWithinHyperspaceRangeTrigger(state.karengoSystem, 1f, true, Stage.GoToSectorEdge)
            triggerCustomAction(OnReachedSectorEdge())
        }
    }

    class OnReachedSectorEdge : TriggerAction {
        override fun doAction(context: MissionTrigger.TriggerActionContext?) {
            Telo1CompleteDialog().build().show(game.sector.campaignUI, game.sector.playerFleet)
            game.sector.playerFleet.clearAssignments()
        }
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)

        state.completeDateInMillis = game.sector.clock.timestamp

        // Credit reward is automatically given and shown.
    }

    override fun endAbandonImpl() {
        super.endAbandonImpl()
        game.logger.i { "Abandoning ${this.name} quest." }

        state.map.clear()
        setCurrentStage(null, null, null)
    }

    /**
     * Bullet points on left side of intel.
     */
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        return when (currentStage) {
            Stage.GoToSectorEdge -> {
                info.addPara(pad, tc) {
                    part1Json.optQuery<String>("/stages/deliveryToEarth/intel/subtitle")?.qgFormat() ?: ""
                }
                true
            }

            else -> false
        }
    }

    override fun getIntelTags(map: SectorMapAPI?) =
        (super.getIntelTags(map) + tags)

    /**
     * Description on right side of intel.
     */
    override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.GoToSectorEdge -> {
                info.addPara { part1Json.query<String>("/stages/deliveryToEarth/intel/desc").qgFormat() }
            }

            Stage.Completed -> {
                info.addPara { part1Json.query<String>("/stages/deliveryDropoff/intel/desc").qgFormat() }
            }
        }
    }

    enum class Stage {
        GoToSectorEdge,
        Completed,
        Abandoned,
    }
}