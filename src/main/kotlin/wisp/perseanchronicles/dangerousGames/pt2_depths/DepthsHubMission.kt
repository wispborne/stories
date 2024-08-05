package wisp.perseanchronicles.dangerousGames.pt2_depths

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.*
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.dangerousGames.pt1_dragons.DragonsHubMission
import wisp.perseanchronicles.game
import wisp.perseanchronicles.isOkForQuest
import wisp.questgiver.isPopulated
import wisp.questgiver.starSystemsAllowedForQuests
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.QGHubMissionWithBarEvent
import wisp.questgiver.v2.spriteName
import wisp.questgiver.wispLib.*
import java.awt.Color
import java.util.*


class DepthsHubMission : QGHubMissionWithBarEvent(missionId = MISSION_ID) {
    companion object {
        const val MISSION_ID = "depths"
        val state = State(PersistentMapData<String, Any?>(key = "depthsState").withDefault { null })
        val tags = setOf(Tags.INTEL_STORY, Tags.INTEL_ACCEPTED)

        private val DEPTHS_PLANET_TYPES = listOf(
            "terran",
            "terran-eccentric",
            "water",
            "US_water", // Unknown Skies
            "US_waterB", // Unknown Skies
            "US_continent" // Unknown Skies
        )

        val icon = IInteractionLogic.Portrait(category = "wisp_perseanchronicles_depths", id = "icon")
        val diveIllustration =
            IInteractionLogic.Illustration(category = "wisp_perseanchronicles_depths", id = "diveIllustration")
        val subIllustration = IInteractionLogic.Illustration(category = "wisp_perseanchronicles_depths", id = "sub")
        val intelIllustration =
            IInteractionLogic.Illustration(category = "wisp_perseanchronicles_depths", id = "intelIllustration")

        const val minimumDistanceFromPlayerInLightYearsToPlaceDepthsPlanet = 5


        val karengo: PersonAPI
            get() = PerseanChroniclesNPCs.karengo
    }

    class State(val map: MutableMap<String, Any?>) {
        var seed: Random? by map
        var startDateMillis: Long? by map
        var completeDateInMillis: Long? by map
        var depthsPlanet: SectorEntityToken? by map
        var startingPlanet: SectorEntityToken? by map
    }

    val choices: Choices =
        Choices(PersistentMapData<String, Any?>(key = "depthsChoices").withDefault { null })

    /**
     * All choices that can be made.
     * Leave `map` public and accessible so it can be cleared if the quest is restarted.
     */
    class Choices(val map: MutableMap<String, Any?>) {
        var riddle1Choice: Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice? by map
        var riddle2Choice: Depths_Stage2_RiddleDialog.RiddleChoice.Riddle2Choice? by map
        var riddle3Choice: Depths_Stage2_RiddleDialog.RiddleChoice.Riddle3Choice? by map
    }

    val riddleAnswers
        get() = listOf(choices.riddle1Choice, choices.riddle2Choice, choices.riddle3Choice)

    val riddleSuccessesCount: Int
        get() = riddleAnswers.count { it?.wasSuccessful() == true }

    val wallCrashesCount: Int
        get() = (if (choices.riddle1Choice is Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice.WestWall) 1 else 0) +
                (if (choices.riddle2Choice is Depths_Stage2_RiddleDialog.RiddleChoice.Riddle2Choice.WestWall) 1 else 0) +
                (if (choices.riddle3Choice is Depths_Stage2_RiddleDialog.RiddleChoice.Riddle3Choice.EastWall) 1 else 0)

    val didAllCrewDie: Boolean
        get() = riddleAnswers.all { it?.wasSuccessful() == false }

    fun isPlanetColonized() = state.depthsPlanet?.market.isPopulated()

    override fun updateTextReplacements(text: Text) {
        text.globalReplacementGetters["dragonPlanet"] = { DragonsHubMission.state.dragonPlanet?.name }
        text.globalReplacementGetters["dragonSystem"] = { DragonsHubMission.state.dragonPlanet?.starSystem?.name }
        text.globalReplacementGetters["depthsSourcePlanet"] = { state.startingPlanet?.name }
        text.globalReplacementGetters["depthsSourceSystem"] = { state.startingPlanet?.starSystem?.name }
        text.globalReplacementGetters["depthsPlanet"] = { state.depthsPlanet?.name }
        text.globalReplacementGetters["depthsSystem"] = { state.depthsPlanet?.starSystem?.name }
        text.globalReplacementGetters["depthsCreditReward"] = { Misc.getDGSCredits(creditsReward.toFloat()) }
    }

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        market ?: return false

        return DepthsBarEventWiring().shouldBeAddedToBarEventPool()
                && market.isOkForQuest()
                && market.factionId.lowercase() !in listOf("luddic_church", "luddic_path")
                && market.size >= 4
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        super.create(createdAt, barEvent)
        state.seed = genRandom

        state.depthsPlanet = findAndTagDepthsPlanet(createdAt?.starSystem) ?: return false
        val planet = state.depthsPlanet ?: return false
        game.logger.i { "Set Depths quest destination to ${planet.fullName} in ${planet.starSystem?.baseName}" }

        startingStage = Stage.GoToPlanet
        setSuccessStage(Stage.Done)
        setAbandonStage(Stage.Abandoned)

        name = game.text["dg_de_intel_title"]
        setRewardMult((planet.distanceFromPlayerInHyperspace / 20f).coerceAtLeast(1f))
        setCreditReward(CreditReward.HIGH)
        updateTextReplacements(game.text)
        setGiverFaction(karengo.faction?.id) // Rep reward.
        personOverride = karengo // Shows on intel, needed for rep reward or else crash.

        setIconName(DepthsHubMission.icon.spriteName(game))

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

        state.startDateMillis = game.sector.clock.timestamp
        state.startingPlanet = startLocation
        game.logger.i { "${this.name} start location set to ${startLocation.fullName} in ${startLocation.starSystem.baseName}" }
        state.startDateMillis = game.sector.clock.timestamp
        PerseanChroniclesNPCs.isKarengoInFleet = true

        // Sets the system as the map objective.
        makeImportant(state.depthsPlanet, null, Stage.GoToPlanet)
        makeImportant(state.startingPlanet, null, Stage.ReturnToStart)
        makePrimaryObjective(state.depthsPlanet)

        trigger {
            beginStageTrigger(Stage.ReturnToStart)
            makePrimaryObjective(state.startingPlanet)
        }
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)
        state.completeDateInMillis = game.sector.clock.timestamp
        PerseanChroniclesNPCs.isKarengoInFleet = false
        // Credit reward is automatically given and shown.

        state.depthsPlanet?.let { planet ->
            if (planet.market.conditions.none { it.plugin is CrystalMarketMod }) {
                planet.market.addCondition("wispQuests_crystallineCatalyst")
            }
        }
    }

    override fun endAbandonImpl() {
        super.endAbandonImpl()
        game.logger.i { "Abandoning ${this.name} quest." }

        PerseanChroniclesNPCs.isKarengoInFleet = false
        state.map.clear()
        setCurrentStage(null, null, null)
    }

    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
        return when {
            // Land on ocean planet for Depths quest
            interactionTarget.hasSameMarketAs(state.depthsPlanet)
                    && currentStage == Stage.GoToPlanet -> {
                PluginPick(
                    Depths_Stage2_RiddleDialog().build(),
                    CampaignPlugin.PickPriority.MOD_SPECIFIC
                )
            }

            // Finish Depths by landing at quest-giving planet
            interactionTarget.hasSameMarketAs(state.startingPlanet)
                    && currentStage == Stage.ReturnToStart -> {
                PluginPick(
                    Depths_Stage2_EndDialog().build(),
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
                    game.text["dg_de_intel_subtitle_stg1"]
                }
            }

            Stage.ReturnToStart -> {
                info.addPara(pad, tc) {
                    game.text.getf(
                        "dg_de_intel_subtitle_stg2",
                        "ifCrewAlive" to
                                if (!didAllCrewDie)
                                    game.text["dg_de_intel_subtitle_stg2_ifCrewAlive"]
                                else String.empty
                    )
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
        val stg1TextColor = if (currentStage.equalsAny(
                Stage.ReturnToStart,
                Stage.Done
            )
        ) Misc.getGrayColor()
        else Misc.getTextColor()

        info.addPara(textColor = stg1TextColor) { game.text["dg_de_intel_desc_stg1"] }

        if (currentStage.equalsAny(Stage.ReturnToStart, Stage.Done)) {
            val stg2TextColor =
                if (isSucceeded)
                    Misc.getGrayColor()
                else
                    Misc.getTextColor()

            info.addPara(textColor = stg2TextColor) { game.text["dg_de_intel_desc_stg2"] }
        }

        if (currentStage == Stage.Done) {
            info.addPara { game.text["dg_de_intel_desc_stg3"] }
        }
    }

    fun startMusic() {
        runCatching {
            game.soundPlayer.playCustomMusic(
                0,
                5,
                "wisp_perseanchronicles_depthsMusic",
                true
            )
        }
            .onFailure {
                game.logger.e(it)
            }
    }

    fun stopMusic() {
        runCatching {
            game.soundPlayer.playCustomMusic(3, 0, null)
        }
            .onFailure {
                game.logger.e(it)
            }
    }

    fun restartQuest() {
        game.logger.i { "Restarting Depths quest." }

        state.map.clear()
        choices.map.clear()
        currentStage = Stage.NotStarted
        game.sector.starSystems.flatMap { it.solidPlanets }
            .filter { it.market?.hasCondition(CrystalMarketMod.CONDITION_ID) == true }
            .forEach { it.market?.removeCondition(CrystalMarketMod.CONDITION_ID) }
    }

    override fun getIntelTags(map: SectorMapAPI?) =
        (super.getIntelTags(map) + tags)

    fun generateRewardLoot(entity: SectorEntityToken): CargoAPI? {
        when (riddleSuccessesCount) {
            3 -> {
                entity.addDropValue(Drops.BASIC, 50000)
                entity.addDropRandom("blueprints", 5)
                entity.addDropRandom("rare_tech", 2)
            }

            2 -> {
                entity.addDropValue(Drops.BASIC, 30000)
                entity.addDropRandom("blueprints", 4)
                entity.addDropRandom("rare_tech", 1)
            }

            1 -> {
                entity.addDropValue(Drops.BASIC, 20000)
                entity.addDropRandom("blueprints", 3)
                entity.addDropRandom("rare_tech", 1)
            }

            else -> {
                entity.addDropValue(Drops.BASIC, 10000)
                entity.addDropRandom("blueprints", 2)
                entity.addDropRandom("rare_tech", 1)
            }
        }

        return SalvageEntity.generateSalvage(
            Misc.getRandom(game.sector.memoryWithoutUpdate.getLong(MemFlags.SALVAGE_SEED), 100),
            1f,
            1f,
            1f,
            1f,
            entity.dropValue,
            entity.dropRandom
        )
            .apply { sort() }
    }

    /**
     * Find a planet with oceans somewhere near the center, excluding player's current location.
     * Prefer decivilized world, then uninhabited, then all others
     */
    private fun findAndTagDepthsPlanet(playersCurrentStarSystem: StarSystemAPI?): PlanetAPI? {
        return try {
            // TODO test the spawn for this, make sure it's being properly random
            game.sector.starSystemsAllowedForQuests
                .filter { it.id != playersCurrentStarSystem?.id }
                .filter { system -> system.solidPlanets.any { planet -> planet.typeId in DEPTHS_PLANET_TYPES } }
                .prefer { it.distanceFromPlayerInHyperspace > minimumDistanceFromPlayerInLightYearsToPlaceDepthsPlanet }
                .sortedBy { it.distanceFromCenterOfSector }
                .flatMap { it.solidPlanets }
                .prefer { it.faction.id == Factions.NEUTRAL } // Uncolonized planets
                .filter { planet -> planet.typeId in DEPTHS_PLANET_TYPES }
                .toList()
                .run {
                    // Take all planets from the top half of the list,
                    // which is sorted by proximity to the center.
                    val possibles = this.take((this.size / 2).coerceAtLeast(1))

                    WeightedRandomPicker<PlanetAPI>().apply {
                        possibles.forEach { planet ->
                            when {
                                planet.market?.hasCondition(Conditions.DECIVILIZED) == true -> {
                                    game.logger.i { "Adding decivved planet ${planet.fullName} in ${planet.starSystem.baseName} to Depths candidate list" }
                                    add(planet, 3f)
                                }

                                planet.market.isPopulated() -> {
                                    game.logger.i { "Adding uninhabited planet ${planet.fullName} in ${planet.starSystem.baseName} to Depths candidate list" }
                                    add(planet, 2f)
                                }

                                else -> {
                                    game.logger.i { "Adding planet ${planet.fullName} in ${planet.starSystem.baseName} to Depths candidate list" }
                                    add(planet, 1f)
                                }
                            }
                        }
                    }
                        .pick()!!
                }
        } catch (e: Exception) {
            // If no planets matching the criteria are found
            game.logger.i(e) { "No planets matching the criteria found for Depths" }
            return null
        }
    }

    enum class Stage {
        NotStarted,
        GoToPlanet,
        ReturnToStart,
        Abandoned,
        Done,
    }
}