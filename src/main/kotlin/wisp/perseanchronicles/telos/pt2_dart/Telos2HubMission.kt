package wisp.perseanchronicles.telos.pt2_dart

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.CampaignPlugin
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.AddRemoveCommodity
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.Faction
import org.json.JSONArray
import org.json.JSONObject
import org.magiclib.util.MagicCampaign
import wisp.perseanchronicles.MOD_ID
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.TelosCommon
import wisp.perseanchronicles.telos.pt1_deliveryToEarth.Telos1HubMission
import wisp.perseanchronicles.telos.pt2_dart.battle.Telos2PirateFleetInteractionDialogPluginImpl
import wisp.questgiver.InteractionDefinition
import wisp.questgiver.spriteName
import wisp.questgiver.v2.IQGHubMission
import wisp.questgiver.v2.QGHubMission
import wisp.questgiver.v2.json.query
import wisp.questgiver.wispLib.*
import java.awt.Color

class Telos2HubMission : QGHubMission(), IQGHubMission {
    companion object {
        // Hardcode because it's being used in rules.csv.
        val MISSION_ID = "wisp_perseanchronicles_telosPt2"

        var part2Json: JSONObject =
            TelosCommon.readJson().query("/$MOD_ID/telos/part2_dart") as JSONObject
            private set

        val tags = setOf(Tags.INTEL_STORY, Tags.INTEL_ACCEPTED)

        val state = State(PersistentMapData<String, Any?>(key = "telosPt2State").withDefault { null })
        val choices = Choices(PersistentMapData<String, Any?>(key = "telosPt2Choices").withDefault { null })
        const val badFleetDefeatTrigger = "wisp_perseanchronicles_telosPt2_badfleetdefeated"

        private val PIRATE_FLEET_TAG = MISSION_ID + "_pirateFleet"

        /**
         * Quotes from Captain Eugel in battle. In chronological order.
         */
        fun getEugelBattleQuotes(): List<String> = part2Json.query<JSONArray>("/stages/battle/quotes").toStringList()
        fun getAllyPhase1BattleQuotes(): List<String> = part2Json.query<JSONArray>("/stages/battle/telosQuotesPhase1").toStringList()
        fun getAllyPhase2BattleQuotes(): List<String> = part2Json.query<JSONArray>("/stages/battle/telosQuotesPhase2").toStringList()
        fun getBattleVictoryQuote(): String = part2Json.query("/stages/battle/victoryQuote")
        fun getEugelShipName(): String = part2Json.query("/stages/battle/flagshipName")
    }

    class State(val map: MutableMap<String, Any?>) {
        var startDateMillis: Long? by map
        var completeDateInMillis: Long? by map

        var talkedToPirateFleet: Boolean? by map

        // If they won this, they cheated.
        var wonRecordedBattle: Boolean? by map
    }

    class Choices(val map: MutableMap<String, Any?>) {
        var askedForMoreEtherInfo: Boolean? by map
        var toldKarengoToTakeEtherFirst: Boolean? by map
        var checkedKarengo: Boolean? by map
        var queriedSystem: Boolean? by map
        var injectedSelf: Boolean? by map // Null if choice not made yet.
    }

    init {
        missionId = MISSION_ID
    }

    override fun onGameLoad(isNewGame: Boolean) {
        super.onGameLoad(isNewGame)

        // Reload json if devmode reload.
        if (isDevMode())
            part2Json = TelosCommon.readJson()
                .query("/$MOD_ID/telos/part2_dart") as JSONObject
    }

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["telosPt1Stg1DestPlanet"] = { Telos1HubMission.state.karengoPlanet?.name }
        text.globalReplacementGetters["telosPt1Stg1DestSystem"] = { Telos1HubMission.state.karengoSystem?.name }
        text.globalReplacementGetters["telosStarName"] =
            { Telos1HubMission.state.karengoPlanet?.starSystem?.star?.name }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        // if already accepted by the player, abort
        if (!setGlobalReference("$$MISSION_ID")) {
            return false
        }

        // Ignore warning, there are two overrides and it's complaining about just one of them.
        @Suppress("ABSTRACT_SUPER_CALL_WARNING")
        super<IQGHubMission>.create(createdAt, barEvent)
        setGenRandom(Telos1HubMission.state.seed ?: Misc.random)

        setStartingStage(Stage.DestroyFleet)
        setSuccessStage(Stage.Completed)

        name = part2Json.query("/strings/title")
        personOverride = PerseanChroniclesNPCs.karengo // Shows on intel, needed for rep reward or else crash.

        setIconName(InteractionDefinition.Portrait(category = "wisp_perseanchronicles_telos", id = "intel").spriteName(game))

        val badFleetFlag = "$${MISSION_ID}_badfleet"
        val badFleetImportantFlag = "${badFleetFlag}_important"

        trigger {
            beginStageTrigger(Stage.DestroyFleet)
            triggerCreateFleet(
                FleetSize.MEDIUM,
                FleetQuality.LOWER,
                Factions.PIRATES,
                FleetTypes.SCAVENGER_MEDIUM,
                Telos1HubMission.state.karengoPlanet
            )
            triggerMakeHostile()
            triggerMakeFleetIgnoreOtherFleetsExceptPlayer()
            triggerFleetNoAutoDespawn()
            triggerFleetNoJump()
            triggerMakeFleetIgnoredByOtherFleets()
            triggerSetPirateFleet()
            triggerAutoAdjustFleetStrengthModerate()
            triggerPickLocationAroundEntity(Telos1HubMission.state.karengoPlanet, 1f)
            triggerSpawnFleetAtPickedLocation(badFleetFlag, null)
            triggerOrderFleetPatrol(false, Telos1HubMission.state.karengoPlanet)
            triggerFleetMakeImportant(badFleetImportantFlag, Stage.DestroyFleet)
            triggerFleetAddDefeatTrigger(badFleetDefeatTrigger)
            triggerFleetAddTags(PIRATE_FLEET_TAG)
        }

        // When you land on the planet, Karengo joins your fleet.
        trigger {
            beginStageTrigger(Stage.LandOnPlanetFirst)
            triggerCustomAction {
                PerseanChroniclesNPCs.isKarengoInFleet = true
            }
        }

        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.acceptImpl(dialog, memoryMap)

        state.startDateMillis = game.sector.clock.timestamp
        setCurrentStage(Stage.DestroyFleet, null, null)
        makeImportant(
            Telos1HubMission.state.karengoPlanet,
            null,
            Stage.DestroyFleet,
            Stage.LandOnPlanetFirst,
            Stage.LandOnPlanetSecondEther,
            Stage.LandOnPlanetSecondNoEther,
        )
        makePrimaryObjective(Telos1HubMission.state.karengoPlanet)
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)

        state.completeDateInMillis = game.sector.clock.timestamp
    }

    override fun endAbandonImpl() {
        super.endAbandonImpl()
        game.logger.i { "Abandoning ${this.name} quest." }
        PerseanChroniclesNPCs.isKarengoInFleet = false

        state.map.clear()
        currentStage = null
    }


    override fun callAction(
        action: String?,
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        // Trigger set in `create` (triggerFleetAddDefeatTrigger), fired off to rules.csv when fleet dies, listen for it here.
        if (action == badFleetDefeatTrigger) {
            setCurrentStage(Stage.LandOnPlanetFirst, null, null)
            return true
        }

        return super.callAction(action, ruleId, dialog, params, memoryMap)
    }

    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
        return when {
            interactionTarget.hasTag(PIRATE_FLEET_TAG) ->
                PluginPick(Telos2PirateFleetInteractionDialogPluginImpl(), CampaignPlugin.PickPriority.MOD_SPECIFIC)

            interactionTarget.id == Telos1HubMission.state.karengoPlanet?.id -> {
                when (currentStage) {
                    Stage.LandOnPlanetFirst -> PluginPick(
                        Telos2FirstLandingDialog().build(),
                        CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )

                    Stage.LandOnPlanetSecondEther,
                    Stage.LandOnPlanetSecondNoEther -> PluginPick(
                        Telos2SecondLandingDialog().build(),
                        CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )

                    else -> null
                }
            }

            else -> null
        }
    }

    /**
     * Bullet points on left side of intel.
     */
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        return when (currentStage) {
            Stage.DestroyFleet -> {
                info.addPara(padding = pad, textColor = tc) {
                    part2Json.query<String>("/stages/destroyFleet/intel/subtitle").qgFormat()
                }
                true
            }

            Stage.LandOnPlanetFirst -> {
                info.addPara(padding = pad, textColor = tc) {
                    part2Json.query<String>("/stages/landOnPlanetFirst/intel/subtitle").qgFormat()
                }
                true
            }

            Stage.LandOnPlanetSecondEther -> {
                info.addPara(padding = pad, textColor = tc) {
                    part2Json.query<String>("/stages/landOnPlanetSecondEther/intel/subtitle").qgFormat()
                }
                true
            }

            else -> false
        }
    }

    /**
     * Description on right side of intel.
     */
    override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float) {
        when (currentStage) {
            Stage.DestroyFleet -> {
                info.addPara { part2Json.query<String>("/stages/destroyFleet/intel/desc").qgFormat() }
            }

            Stage.LandOnPlanetFirst -> {
                info.addPara { part2Json.query<String>("/stages/landOnPlanetFirst/intel/desc").qgFormat() }
            }

            Stage.LandOnPlanetSecondEther -> {
                info.addPara { part2Json.query<String>("/stages/landOnPlanetSecondEther/intel/desc").qgFormat() }
            }

            Stage.PostBattle,
            Stage.Completed -> {
                info.addPara { part2Json.query<String>("/stages/completed/intel/subtitle").qgFormat() }

                if (TelosCommon.isPhase1) {
                    info.addPara(textColor = Misc.getHighlightColor()) { "Karengo and the Telos will continue in Persean Chronicles v3.2." }
                }
            }
        }
    }

    override fun getIntelTags(map: SectorMapAPI?) =
        (super.getIntelTags(map) + tags)


    fun giveShipOrPutInOrbit(dialog: InteractionDialogAPI) {
        val ship = game.factory.createFleetMember(FleetMemberType.SHIP, TelosCommon.AVALOK_ID)

        if (game.sector.playerFleet.numShips >= game.settings.maxShipsInFleet) {
            val fleet =
                game.factory.createEmptyFleet(Faction.NO_FACTION.id, "Telos ${TelosCommon.DART_NAME}", false).apply {
                    fleetData.addFleetMember(ship)
                }

            val planet = Telos1HubMission.state.karengoPlanet!!
            fleet.containingLocation = planet.containingLocation
            fleet.setLocation(planet.location.x, planet.location.y)
            MagicCampaign.placeOnStableOrbit(fleet, false)
        } else {
            game.sector.playerFleet.fleetData.addFleetMember(ship)
            AddRemoveCommodity.addFleetMemberGainText(ship, dialog.textPanel)
        }
    }

    enum class Stage {
        DestroyFleet,
        LandOnPlanetFirst,
        LandOnPlanetSecondEther,
        LandOnPlanetSecondNoEther,
        PostBattle,
        Completed,
    }
}