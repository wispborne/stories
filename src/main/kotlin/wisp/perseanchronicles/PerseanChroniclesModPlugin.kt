package wisp.perseanchronicles

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.impl.campaign.CoreRuleTokenReplacementGeneratorImpl
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.util.Misc
import com.thoughtworks.xstream.XStream
import org.apache.log4j.Level
import org.dark.shaders.util.ShaderLib
import org.dark.shaders.util.TextureData
import org.json.JSONObject
import org.magiclib.achievements.MagicAchievementManager
import wisp.perseanchronicles.achievements.Achievements
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.dangerousGames.pt1_dragons.DragonsBarEventWiring
import wisp.perseanchronicles.dangerousGames.pt1_dragons.DragonsHubMission
import wisp.perseanchronicles.dangerousGames.pt1_dragons.Dragons_Stage1_BarEvent
import wisp.perseanchronicles.dangerousGames.pt2_depths.*
import wisp.perseanchronicles.laborer.*
import wisp.perseanchronicles.nirvana.*
import wisp.perseanchronicles.riley.*
import wisp.perseanchronicles.telos.TelosCommon
import wisp.perseanchronicles.telos.pt1_deliveryToEarth.Telos1BarEventWiring
import wisp.perseanchronicles.telos.pt1_deliveryToEarth.Telos1HubMission
import wisp.perseanchronicles.telos.pt2_dart.Telos2HubMission
import wisp.perseanchronicles.telos.pt3_arrow.MenriSystemCreator
import wisp.perseanchronicles.telos.pt3_arrow.Telos3HubMission
import wisp.perseanchronicles.telos.pt3_arrow.nocturne.EthersightAbility
import wisp.questgiver.Configuration
import wisp.questgiver.Questgiver
import wisp.questgiver.wispLib.lastName
import wisp.questgiver.wispLib.toStringList
import wisp.questgiver.wispLib.tryGet
import java.util.*

class PerseanChroniclesModPlugin : BaseModPlugin() {
    init {
        Questgiver.init(modPrefix = MOD_ID)
    }

    override fun onApplicationLoad() {
        super.onApplicationLoad()
        addTextToServiceLocator()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)
        Questgiver.onGameLoad(newGame)

        // When the game (re)loads, we want to grab the new instances of everything, especially the new sector.
        game = SpaceTalesServiceLocator(Questgiver.game)
        game.logger.level = Level.ALL // try to remember to change this for release
        game.text.shouldThrowExceptionOnMissingValue = game.settings.isDevMode

        TelosCommon.onGameLoad()
        addTextToServiceLocator()
        game.jukebox.onGameLoad()

        val settings = game.settings
            .getMergedJSONForMod(
                "data/config/modSettings.json",
                "MagicLib"
            )
            .getJSONObject(MOD_ID)

        // Too lazy to add a compile-time dependency on Nexerelin.
        // FUN FACT Corvus Mode is non-random mode and I'm an idiot
        val isNexCorvusModeEnabled = if (game.sector.memory.keys.contains("\$nex_corvusMode")) {
            game.sector.memory.getBoolean("\$nex_corvusMode")
        } else {
            true // Without Nex, map is non-random.
        }

        Questgiver.loadQuests(
            creators = listOfNotNull(
                if (isNexCorvusModeEnabled && settings.tryGet("isTelosQuestEnabled") { true })
                    Telos1BarEventWiring()
                else null,
                if (settings.tryGet("isLaborerQuestEnabled") { true }) LaborerBarEventWiring() else null,
                if (settings.tryGet("isDragonsQuestEnabled") { true }) DragonsBarEventWiring() else null,
                if (settings.tryGet("isDepthsQuestEnabled") { true }) DepthsBarEventWiring() else null,
                if (settings.tryGet("isRileyQuestEnabled") { true }) RileyBarEventWiring() else null,
                if (settings.tryGet("isNirvanaQuestEnabled") { true }) NirvanaBarEventWiring() else null,
            ),
            configuration = readConfiguration(settings),
        )

        applyTextVariableSubstitutions()

        initGraphicsLib()

        game.sector.importantPeople.addPerson(PerseanChroniclesNPCs.karengo)
        game.sector.importantPeople.addPerson(PerseanChroniclesNPCs.captainEugel)
        game.sector.importantPeople.addPerson(PerseanChroniclesNPCs.riley)

        if (game.sector.playerPerson.nameString == "test") {
            kotlin.runCatching {
                if (MenriSystemCreator.createMenriSystem() != null) {
                    val menri = game.sector.getStarSystem("menri")
//                    game.sector.playerFleet.loca
                }
            }
                .onFailure { game.logger.e(it) }
        }

        MagicAchievementManager.getInstance().addAchievementSpecs(Achievements.PignutsAchievementSpec())
        MagicAchievementManager.getInstance().addAchievementSpecs(Achievements.CheatedFlashbackBattleAchievementSpec())
        MagicAchievementManager.getInstance().addAchievementSpecs(Achievements.DefeatedEugelEarlyAchievementSpec())
        MagicAchievementManager.getInstance().addAchievementSpecs(Achievements.DefeatedEugelAchievementSpec())
        Questgiver.onGameLoadEnd(newGame)

        // DON'T FORGET THIS
        // YOU DUMBASS YOU FORGOT
        if (false) {
            game.sector.characterData.addAbility(TelosCommon.ETHER_SIGHT_ID)
            game.sector.playerFleet.addAbility(TelosCommon.ETHER_SIGHT_ID)
        }
    }

    /**
     * Adds text variable substitutions, including all vanilla ones.
     * See [com.fs.starfarer.api.impl.campaign.CoreRuleTokenReplacementGeneratorImpl.getTokenReplacements].
     */
    private fun applyTextVariableSubstitutions() {
        // Add all vanilla replacements to our global replacements.
        CoreRuleTokenReplacementGeneratorImpl().getTokenReplacements(null, null, null)
            .entries
            .forEach { (variable, value) -> game.text.globalReplacementGetters[variable.trim().removePrefix("$")] = { value } }

        game.text.globalReplacementGetters["playerPronounHimHer"] = {
            when (game.sector.playerPerson.gender) {
                FullName.Gender.MALE -> game.text["playerPronounHim"]
                FullName.Gender.FEMALE -> game.text["playerPronounHer"]
                else -> game.text["playerPronounThey"]
            }
        }
        game.text.globalReplacementGetters["playerPronounHeShe"] = {
            when (game.sector.playerPerson.gender) {
                FullName.Gender.MALE -> game.text["playerPronounHe"]
                FullName.Gender.FEMALE -> game.text["playerPronounShe"]
                else -> game.text["playerPronounThey"]
            }
        }
        game.text.globalReplacementGetters["playerSirOrMaamUcFirst"] = {
            when (game.sector.playerPerson.gender) {
                FullName.Gender.FEMALE -> game.text["playerMadam"]
                else -> game.text["playerSir"] // Use Sir for genderless because it can technically be used for women as well.
            }
                .let { Misc.ucFirst(it) }
        }
        game.text.globalReplacementGetters["playerSirOrMaam"] = {
            when (game.sector.playerPerson.gender) {
                FullName.Gender.FEMALE -> game.text["playerMadam"]
                else -> game.text["playerSir"] // Use Sir for genderless because it can technically be used for women as well.
            }
        }
        game.text.globalReplacementGetters["playerManOrWoman"] = {
            when (game.sector.playerPerson.gender) {
                FullName.Gender.FEMALE -> game.text["playerWoman"]
                FullName.Gender.MALE -> game.text["playerMan"]
                else -> game.text["playerPerson"]
            }
        }
        game.text.globalReplacementGetters["playerFlagshipName"] = { game.sector.playerFleet.flagship?.shipName }
        game.text.globalReplacementGetters["playerLastName"] = { game.sector.playerPerson.lastName }

        game.text.globalReplacementGetters["dragonPlanet"] = { DragonsHubMission.state.dragonPlanet?.name }
        game.text.globalReplacementGetters["dragonSystem"] = { DragonsHubMission.state.dragonPlanet?.starSystem?.name }
    }

    /**
     * Tell the XML serializer to use custom naming, so that moving or renaming classes doesn't break saves.
     */
    override fun configureXStream(x: XStream) {
        super.configureXStream(x)
        Questgiver.configureXStream(x)

        // DO NOT CHANGE THESE STRINGS, DOING SO WILL BREAK SAVE GAMES
        // No periods allowed in the serialized name, causes crash.
        val aliases = listOf(
            Dragons_Stage1_BarEvent::class to "DragonsPart1_BarEvent",
            DragonsHubMission::class to "DragonsHubMission",
            DragonsHubMission.Stage::class to "DragonsHubMission_Stage",
            DragonsBarEventWiring::class to "DragonsBarEventWiring",
            DepthsHubMission::class to "DepthsHubMission",
            DepthsHubMission.Stage::class to "DepthsHubMission_Stage",
            DepthsBarEventLogic::class to "DepthsBarEventLogic",
            DepthsBarEventWiring::class to "DepthsBarEventWiring",
            DepthsBarEventCreator::class to "DepthsBarEventCreator",
            Depths_Stage2_RiddleDialog::class to "Depths_Stage2_RiddleDialog",
            Depths_Stage2_EndDialog::class to "Depths_Stage2_EndDialog",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice.EastMorg::class to "1East",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice.NorthSuccess::class to "1North",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice.SouthSmoke::class to "1South",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle1Choice.WestWall::class to "1West",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle2Choice.EastSuccess::class to "2East",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle2Choice.NorthVines::class to "2North",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle2Choice.WestWall::class to "2West",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle3Choice.NorthKoijuu::class to "3North",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle3Choice.EastWall::class to "3East",
            Depths_Stage2_RiddleDialog.RiddleChoice.Riddle3Choice.SouthSuccess::class to "3South",
            RileyHubMission::class to "RileyHubMission",
            Riley_Stage1_BarEvent::class to "Riley_Stage1_BarEvent",
            RileyBarEventWiring::class to "RileyBarEventWiring",
            Riley_Stage2_TriggerDialogScript::class to "Riley_Stage2_TriggerDialogScript",
            Riley_Stage2_TriggerDialogScript::class to "Riley_Stage2_Dialog",
            Riley_EnteredDestinationSystemListener::class to "Riley_EnteredDestinationSystemListener",
            Riley_Stage3_Dialog::class to "Riley_Stage3_Dialog",
            Riley_Stage4_Dialog::class to "Riley_Stage4_Dialog",
            NirvanaHubMission::class to "NirvanaHubMission",
            Nirvana_Stage1_BarEvent::class to "Nirvana_Stage1_BarEvent",
            NirvanaBarEventCreator::class to "NirvanaBarEventCreator",
            Nirvana_Stage2_Dialog::class to "Nirvana_Stage2_Dialog",
            Nirvana_Stage3_Dialog::class to "Nirvana_Stage3_Dialog",
            LaborerHubMission::class to "LaborerHubMission",
            Laborer_Stage1_BarEvent::class to "Laborer_Stage1_BarEvent",
            LaborerBarEventCreator::class to "LaborerBarEventCreator",
            Laborer_Stage2_Dialog::class to "Laborer_Stage2_Dialog",
            Telos1BarEventWiring::class to "Telos1BarEventWiring",
            Telos1HubMission::class to "Telos1HubMission",
            Telos2HubMission::class to "Telos2HubMission",
            Telos3HubMission::class to "Telos3HubMission",
            EthersightAbility::class to "EthersightAbility",
        )

        // Prepend with mod prefix so the classes don't conflict with anything else getting serialized
        aliases.forEach { x.alias("${MOD_ID}_${it.second}", it.first.java) }
    }

    private fun addTextToServiceLocator() {
        game.text.resourceBundles.addAll(
            listOf(
                ResourceBundle.getBundle("PerseanChronicles_Shared"),
                ResourceBundle.getBundle("PerseanChronicles_Karengo_1_Dragons"),
                ResourceBundle.getBundle("PerseanChronicles_Karengo_2_Depths"),
                ResourceBundle.getBundle("PerseanChronicles_Nirvana"),
                ResourceBundle.getBundle("PerseanChronicles_Laborer"),
                ResourceBundle.getBundle("PerseanChronicles_Riley_1_Father")
            )
        )
    }

    private fun readConfiguration(modSettings: JSONObject): Configuration {
        val startTime = game.sector.clock.timestamp
        val blacklistedSystemTags = kotlin.runCatching {
            modSettings.getJSONArray("system_tags_to_blacklist")
                .toStringList()
                .distinct()
        }
            .onFailure { game.logger.i(it) { it.message } }
            .getOrElse { emptyList() }
            // Don't let quests go to TTBlacksite or hidden mod systems.
            .plus(Tags.THEME_HIDDEN)

        val blacklistedMarketIds = kotlin.runCatching {
            modSettings.getJSONArray("market_ids_to_blacklist")
                .toStringList()
                .distinct()
        }
            .onFailure { game.logger.i(it) { it.message } }
            .getOrElse { emptyList() }

        val blacklistedSystemIds = kotlin.runCatching {
            modSettings.getJSONArray("system_ids_to_blacklist")
                .toStringList()
                .distinct()
        }
            .onFailure { game.logger.i(it) { it.message } }
            .getOrElse { emptyList() }

        val whitelistedFactions = kotlin.runCatching {
            modSettings.getJSONArray("faction_ids_to_whitelist")
                .toStringList()
                .distinct()
        }
            .onFailure { game.logger.i(it) { it.message } }
            .getOrElse { emptyList() }


        val conf = Configuration(
            blacklist = Configuration.Blacklist(
                systemIds = blacklistedSystemIds,
                marketIds = blacklistedMarketIds,
                systemTags = blacklistedSystemTags
            ),
            whitelist = Configuration.Whitelist(
                factionIds = whitelistedFactions
            )
        )

        game.logger.i { "Persean Chronicles system blacklist loaded in ${game.sector.clock.timestamp - startTime} seconds.\n$conf" }

        return conf
    }

    fun initGraphicsLib() {
        if (game.settings.modManager.isModEnabled("shaderLib")) {
            ShaderLib.init()
//            LightData.readLightDataCSV("data/lights/perseanchronicles_light_data.csv")
            TextureData.readTextureDataCSV("data/lights/perseanchronicles_texture_data.csv")
        }
    }

    /**
     * Fix for bug in 3.0.0 - 3.0.2, Riley never pays player.
     *
     * Bug: if you clear the state from Abandon, you get paid again
     */
    private fun fixV302RileyBug() {
        if (RileyHubMission.state.isPostV302save != true) {
            RileyHubMission.state.isPostV302save = true

            // If quest was completed and player didn't refuse payment, give them the money.
            if (RileyHubMission.state.completeDateInMillis != null && RileyHubMission.choices.refusedPayment != true) {
                game.sector.addTransientScript(object : EveryFrameScript {
                    var done = false
                    var timePassed = 0f
                    override fun isDone(): Boolean = done
                    override fun runWhilePaused(): Boolean = false
                    override fun advance(amount: Float) {
                        timePassed += amount

                        if (timePassed > 2f) {
                            game.sector.playerFleet.cargo.credits.add(70000f)
                            game.sector.campaignUI.addMessage(
                                "80,000 credits appear in your account. \n\"Sorry, forgot to pay you!\" - Riley",
                                Misc.getHighlightColor()
                            )
                            done = true
                            game.sector.removeTransientScript(this)
                        }
                    }
                })
            }
        }
    }


    override fun beforeGameSave() = Questgiver.beforeGameSave()
    override fun afterGameSave() = Questgiver.afterGameSave()
    override fun onGameSaveFailed() = Questgiver.onGameSaveFailed()
    override fun onNewGameAfterProcGen() = Questgiver.onNewGameAfterProcGen()
    override fun onNewGameAfterEconomyLoad() = Questgiver.onNewGameAfterEconomyLoad()
    override fun onNewGameAfterTimePass() = Questgiver.onNewGameAfterTimePass()
}