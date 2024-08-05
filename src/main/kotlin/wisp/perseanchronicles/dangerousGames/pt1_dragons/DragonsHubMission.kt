package wisp.perseanchronicles.dangerousGames.pt1_dragons

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger
import com.fs.starfarer.api.impl.campaign.missions.hub.MissionTrigger.TriggerAction
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.game
import wisp.perseanchronicles.isOkForQuest
import wisp.questgiver.spriteName
import wisp.questgiver.starSystemsAllowedForQuests
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.QGHubMissionWithBarEvent
import wisp.questgiver.v2.spriteName
import wisp.questgiver.wispLib.*
import java.awt.Color
import java.util.*


class DragonsHubMission : QGHubMissionWithBarEvent(missionId = MISSION_ID) {
    companion object {
        const val MISSION_ID = "dragons"
        val state = State(PersistentMapData<String, Any?>(key = "dragonState").withDefault { null })
        val tags = setOf(Tags.INTEL_STORY, Tags.INTEL_ACCEPTED)

        private const val minimumDistanceFromPlayerInLightYearsToPlaceDragonPlanet = 5
        private val DRAGON_PLANET_TYPES = listOf(
            "terran",
            "terran-eccentric",
            "jungle",
            "US_jungle" // Unknown Skies
        )
        val icon = IInteractionLogic.Portrait("wisp_perseanchronicles_dragonriders", "icon")
        val intelDetailHeaderImage =
            IInteractionLogic.Illustration("wisp_perseanchronicles_dragonriders", "intelPicture")
        val dragonPlanetImage =
            IInteractionLogic.Illustration("wisp_perseanchronicles_dragonriders", "planetIllustration")

        // Gilead is a paradise world, one of the few planets where it makes sense for dragons to live.
        val gilead: PlanetAPI?
            get() = game.sector.getStarSystem("canaan")?.planets?.firstOrNull { it.id == "gilead" }
    }

    val karengo
        get() = PerseanChroniclesNPCs.karengo

    class State(val map: MutableMap<String, Any?>) {
        var seed: Random? by map
        var startDateMillis: Long? by map
        var completeDateInMillis: Long? by map
        var dragonPlanet: SectorEntityToken? by map
        var startingPlanet: SectorEntityToken? by map
        val dragonSystem: StarSystemAPI?
            get() = dragonPlanet?.starSystem

    }

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        market ?: return false

        return DragonsBarEventWiring().shouldBeAddedToBarEventPool()
                && market.isOkForQuest()
                && market.factionId.lowercase() !in listOf(Factions.LUDDIC_CHURCH, Factions.LUDDIC_PATH)
                && market.starSystem != null
                // and not near gilead
                &&
                (if (gilead != null)
                    market.starSystem.distanceFrom(gilead!!.starSystem) > minimumDistanceFromPlayerInLightYearsToPlaceDragonPlanet
                else true)
                && market.size >= 3
//                && state.dragonPlanet != null
    }

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["dragonPlanet"] = { state.dragonPlanet?.name }
        text.globalReplacementGetters["dragonSystem"] = { state.dragonPlanet?.starSystem?.name }
        text.globalReplacementGetters["startPlanet"] = { state.startingPlanet?.name }
        text.globalReplacementGetters["startSystem"] = { state.startingPlanet?.starSystem?.name }
        text.globalReplacementGetters["dragonsInitialCreditOffer"] = { Misc.getDGSCredits(creditsReward.toFloat()) }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        super.create(createdAt, barEvent)
        state.seed = genRandom

        state.dragonPlanet = findAndTagDragonPlanet(createdAt?.starSystem)

        startingStage = Stage.GoToPlanet
        setSuccessStage(Stage.Done)
        setFailureStage(Stage.FailedByAbandoningDragonriders)
        setAbandonStage(Stage.Abandoned)

        name = game.text["dg_dr_intel_title"]
        setCreditReward(CreditReward.HIGH)
        setGiverFaction(karengo.faction.id) // Rep reward.
        personOverride = karengo // Shows on intel, needed for rep reward or else crash.

        setIconName(DragonsHubMission.icon.spriteName(game))

        state.startingPlanet = createdAt?.primaryEntity

        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.acceptImpl(dialog, memoryMap)

        val startLocation = dialog?.interactionTarget
            ?: kotlin.run {
                game.logger.e { "Aborting acceptance of ${this.name} because dialog was null." }
                abort()
                return
            }

        state.startingPlanet = startLocation
        game.logger.i { "${this.name} start location set to ${startLocation.fullName} in ${startLocation.starSystem.baseName}" }
        state.startDateMillis = game.sector.clock.timestamp
        PerseanChroniclesNPCs.isKarengoInFleet = true

        // Sets the system as the map objective.
        makeImportant(state.dragonPlanet, null, Stage.GoToPlanet)
        makeImportant(state.startingPlanet, null, Stage.ReturnToStart)
        makePrimaryObjective(state.dragonPlanet)

        trigger {
            beginStageTrigger(Stage.ReturnToStart)
            makePrimaryObjective(state.startingPlanet)
        }

        trigger {
            beginStageTrigger(Stage.ReturnedToStart)
            triggerCustomAction(SetCreditRewardAction())
        }
    }

    class SetCreditRewardAction : TriggerAction {
        override fun doAction(context: MissionTrigger.TriggerActionContext?) {
            val mission = context?.mission as? BaseHubMission
            mission?.setCreditReward(mission.creditsReward + 5000) // Add 5000 credits to reward as other dragonriders' share.
        }
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        state.completeDateInMillis = game.sector.clock.timestamp
        PerseanChroniclesNPCs.isKarengoInFleet = false
        // Credit reward is automatically given and shown.
    }

    override fun endFailureImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endFailureImpl(dialog, memoryMap)
        PerseanChroniclesNPCs.isKarengoInFleet = false
    }

    override fun endAbandonImpl() {
        super.endAbandonImpl()
        game.logger.i { "Abandoning ${this.name} quest." }

        state.map.clear()
        PerseanChroniclesNPCs.isKarengoInFleet = false
        setCurrentStage(Stage.NotStarted, null, null)
    }

    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
        return when {
            // Land on planet with dragons
            interactionTarget.hasSameMarketAs(state.dragonPlanet)
                    && currentStage == Stage.GoToPlanet -> {
                PluginPick(
                    Dragons_Stage2_Dialog().build(),
                    CampaignPlugin.PickPriority.MOD_SPECIFIC
                )
            }

            // Finish Dragonriders by landing at quest-giving planet
            interactionTarget.hasSameMarketAs(state.startingPlanet)
                    && currentStage == Stage.ReturnToStart -> {
                PluginPick(
                    Dragons_Stage3_Dialog().build(),
                    CampaignPlugin.PickPriority.MOD_SPECIFIC
                )
            }

            else -> super.pickInteractionDialogPlugin(interactionTarget)
        }
    }

    /**
     * Bullet points on left side of intel.
     */
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        when (currentStage) {
            Stage.GoToPlanet -> {
                info.addPara(pad, tc) {
                    game.text["dg_dr_intel_subtitle_stg1"]
                }
            }

            Stage.ReturnToStart -> {
                info.addPara(pad, tc) {
                    game.text["dg_dr_intel_subtitle_stg2"]
                }
            }

            else -> return false
        }

        return true
    }

    /**
     * Description on right side of intel.
     */
    override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float) {
        val part1Color =
            if (currentStage != Stage.GoToPlanet) Misc.getGrayColor()
            else Misc.getTextColor()

        info.addImage(intelDetailHeaderImage.spriteName(game), width, Padding.DESCRIPTION_PANEL)
        info.addPara(textColor = part1Color) {
            game.text["dg_dr_intel_desc_para1"]
        }
        val part2Color =
            if (currentStage == Stage.Done)
                Misc.getGrayColor()
            else
                Misc.getTextColor()

        if (currentStage == Stage.ReturnToStart || currentStage == Stage.Done) {
            info.addPara(textColor = part2Color) {
                game.text["dg_dr_intel_desc_stg2"]
            }
        }

        if (currentStage == Stage.Done) {
            info.addPara { game.text["dg_dr_intel_desc_stg3"] }
        }

        if (currentStage == Stage.FailedByAbandoningDragonriders) {
            info.addPara(textColor = part2Color) {
                game.text["dg_dr_intel_desc_stg-failedByAbandon"]
            }
        }
    }

    /**
     * Ideally Gilead, but use a fallback for random sector.
     * Find a planet with life somewhere near the center, excluding player's current location.
     */
    private fun findAndTagDragonPlanet(playersCurrentStarSystem: StarSystemAPI?): PlanetAPI? {
        if (gilead != null) {
            return gilead
        }

        return try {
            game.sector.starSystemsAllowedForQuests
                .filter { it.id != playersCurrentStarSystem?.id }
                .filter { system ->
                    system.solidPlanets
                        .any { planet -> DRAGON_PLANET_TYPES.any { it == planet.typeId } }
                }
                .prefer { it.distanceFromPlayerInHyperspace > minimumDistanceFromPlayerInLightYearsToPlaceDragonPlanet }
                .sortedBy { it.distanceFromCenterOfSector }
                .flatMap { it.solidPlanets }
                .filter { planet -> DRAGON_PLANET_TYPES.any { it == planet.typeId } }
                .toList()
                .getNonHostileOnlyIfPossible()
                .run {
                    // Take all planets from the top third of the list,
                    // which is sorted by proximity to the center.
                    this.take((this.size / 3).coerceAtLeast(1))
                }
                .random()
        } catch (e: Exception) {
            // If no planets matching the criteria are found
            game.logger.i(e) { "No planets matching the criteria found for Dragons" }
            return null
        }
    }

    override fun getIntelTags(map: SectorMapAPI?): Set<String> {
        return (super.getIntelTags(map) + tags)
    }

    enum class Stage {
        NotStarted,
        GoToPlanet,
        ReturnToStart,
        ReturnedToStart,
        FailedByAbandoningDragonriders,
        Abandoned,
        Done,
    }
}