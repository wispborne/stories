package wisp.questgiver

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.thoughtworks.xstream.XStream
import wisp.questgiver.v2.BarEventWiring
import wisp.questgiver.wispLib.QuestgiverServiceLocator
import wisp.questgiver.wispLib.ServiceLocator

object Questgiver {
    @Deprecated("Use hubMissionCreators.")
    internal lateinit var questFacilitators: List<QuestFacilitator>
    internal lateinit var hubMissionCreators: List<BarEventWiring<*>>

    /**
     * An idempotent method to initialize Questgiver with enough information to start up.
     *
     * @param modPrefix The mod prefix to use, without a trailing underscore.
     */
    fun init(modPrefix: String) {
        MOD_PREFIX = modPrefix
    }

    /**
     * Call this when a save game is loaded.
     * This refreshes the sector data so that the loaded game doesn't have any data from the previous save.
     */
    fun onGameLoad(isNewGame: Boolean) {
        this.game = QuestgiverServiceLocator()
    }

    fun onGameLoadEnd(isNewGame: Boolean) {
        for (x in Global.getSector().listenerManager.getListeners(
            ModPluginEventListener::class.java
        )) {
            x.onGameLoad(isNewGame)
        }
    }

    fun loadQuests(
        creators: List<BarEventWiring<*>>,
        configuration: Configuration
    ) = loadQuests(
        questFacilitators = emptyList(),
        creators = creators,
        configuration = configuration
    )

    /**
     * @param questFacilitators All [QuestFacilitator]s used by the mod.
     * @param configuration The white/blacklist configuration
     */
    @Deprecated("Use overload without questFacilitators instead.")
    fun loadQuests(
        questFacilitators: List<QuestFacilitator>,
        creators: List<BarEventWiring<*>>,
        configuration: Configuration
    ) {
        this.questFacilitators = questFacilitators
        this.hubMissionCreators = creators

        game.configuration = configuration

        Questgiver.questFacilitators.forEach { questFacilitator ->
            if (questFacilitator is AutoQuestFacilitator) {
                questFacilitator.onDestroy()
                questFacilitator.onGameLoad()
                questFacilitator.autoBarEventInfo
                    ?.also {
                        BarEventManager.getInstance()
                            .configureBarEventCreator(
                                shouldGenerateBarEvent = it.shouldGenerateBarEvent(),
                                barEventCreator = it.barEventCreator,
                                isStarted = questFacilitator.stage.progress != AutoQuestFacilitator.Stage.Progress.NotStarted
                            )
                    }
            }

            questFacilitator.updateTextReplacements(game.text)
        }

        creators.forEach { creator ->
            BarEventManager.getInstance()
                .configureBarEventCreator(
                    shouldGenerateBarEvent = creator.shouldBeAddedToBarEventPool(),
                    barEventCreator = creator.createBarEventCreator(),
                    isStarted = !creator.shouldBeAddedToBarEventPool()
                )
        }

        QuestgiverEveryFrameScript.start()
    }

    /**
     * The mod prefix, without a trailing underscore.
     */
    internal lateinit var MOD_PREFIX: String


    /**
     * Singleton instance of the service locator. Set a new one of these for unit tests.
     */
    var game: ServiceLocator = QuestgiverServiceLocator()
        internal set

    fun configureXStream(x: XStream) {
        // DO NOT CHANGE THESE STRINGS, DOING SO WILL BREAK SAVE GAMES
        // No periods allowed in the serialized name, causes crash.
        val aliases = listOf(
            BarEventDefinition::class to "BarEventDefinition",
            AutoBarEventDefinition::class to "AutoBarEventDefinition",
            AutoBarEventDefinition.AutoBarEvent::class to "AutoBarEvent",
            BarEventDefinition.BarEvent::class to "BarEventDefinition.BarEvent",
            InteractionDefinition::class to "InteractionDefinition",
            InteractionDefinition.Page::class to "InteractionDefinition.Page",
            InteractionDefinition.InteractionDialog::class to "InteractionDefinition.InteractionDialog",
        )

        // Prepend with mod prefix so the classes don't conflict with anything else getting serialized
        aliases.forEach { x.alias("${MOD_PREFIX}_${it.second}", it.first.java) }
    }


    fun beforeGameSave() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.beforeGameSave()
        }
    }

    fun afterGameSave() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.afterGameSave()
        }
    }

    fun onGameSaveFailed() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.onGameSaveFailed()
        }
    }

    fun onNewGameAfterProcGen() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.onNewGameAfterProcGen()
        }
    }

    fun onNewGameAfterEconomyLoad() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.onNewGameAfterEconomyLoad()
        }
    }

    fun onNewGameAfterTimePass() {
        Global.getSector().listenerManager.getListeners(ModPluginEventListener::class.java).forEach {
            it.onNewGameAfterTimePass()
        }
    }
}