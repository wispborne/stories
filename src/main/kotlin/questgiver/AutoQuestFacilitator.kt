package wisp.questgiver

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator
import wisp.questgiver.AutoQuestFacilitator.AutoBarEventInfo
import wisp.questgiver.AutoQuestFacilitator.AutoIntelInfo
import wisp.questgiver.wispLib.*
import kotlin.properties.Delegates

/**
 * @param autoIntelInfo If the quest has intel, use this field to have it managed. See [AutoIntelInfo].
 * @param autoBarEventInfo If the quest has a bar event, use this field to have it managed. See [AutoBarEventInfo].
 */
@Deprecated("Use v2")
abstract class AutoQuestFacilitator(
    private var stageBackingField: PersistentData<Stage>,
    internal val autoIntelInfo: AutoIntelInfo<out BaseIntelPlugin>?,
    internal val autoBarEventInfo: AutoBarEventInfo?
) : QuestFacilitator {

    var stage: Stage by Delegates.observable(stageBackingField.get()) { _, oldStage, newStage ->
        // Update backing field to update save game
        if (stageBackingField != newStage) {
            stageBackingField.set(newStage)
        }

        if (oldStage == newStage)
            return@observable

        if (autoIntelInfo != null) {
            val shownIntel = Questgiver.game.intelManager.findFirst(autoIntelInfo.intelClass)

            if (newStage.progress == Stage.Progress.NotStarted && shownIntel != null) {
                shownIntel.endImmediately()
                Questgiver.game.intelManager.removeIntel(shownIntel)
            } else if (newStage.progress == Stage.Progress.InProgress && shownIntel == null) {
                Questgiver.game.intelManager.addIntel(autoIntelInfo.intelCreator())
            } else if (
                (newStage.progress == Stage.Progress.Completed && shownIntel != null)
                && !shownIntel.isEnding
                && !shownIntel.isEnded
            ) {
                shownIntel.endAndNotifyPlayer()
            }
        }

        if (autoBarEventInfo != null) {
            val barEventManager = BarEventManager.getInstance()

            // If we just moved to NotStarted from a different stage, reset the timer so it's immediately available
            if (newStage.progress == Stage.Progress.NotStarted && barEventManager
                    .hasEventCreator(autoBarEventInfo.barEventCreator::class.java)
            ) {
                barEventManager.setTimeout(autoBarEventInfo.barEventCreator::class.java, 0f)
                barEventManager.removeBarEventCreator(autoBarEventInfo.barEventCreator::class.java)
            }

            barEventManager
                .configureBarEventCreator(
                    shouldGenerateBarEvent = autoBarEventInfo.shouldGenerateBarEvent(),
                    barEventCreator = autoBarEventInfo.barEventCreator,
                    isStarted = newStage.progress != Stage.Progress.NotStarted
                )
        }
    }

    fun getShownIntel(): BaseIntelPlugin? {
        return Questgiver.game.intelManager.findFirst((autoIntelInfo ?: return null).intelClass)
    }

    /**
     * Set up the quest as if the player was about to start it from the given [MarketAPI].
     * Especially, set new start and end points based on the current location.
     * This is called just before `shouldOfferFromMarket`.
     *
     * @param interactionTarget The target of the interaction. For bar events, this is the planet/station.
     * @param market The [MarketAPI] of the target, if there is one.
     */
    abstract fun regenerateQuest(interactionTarget: SectorEntityToken, market: MarketAPI?)

    internal fun onGameLoad() {
        stage = stageBackingField.get()
        autoBarEventInfo?.stage = { stageBackingField.get() }
    }

    /**
     * Avoid any potential memory leaks or things held across saves.
     */
    internal fun onDestroy() {
        autoBarEventInfo?.stage = null
    }

    /**
     * The current stage of the quest.
     */
    abstract class Stage(val progress: Progress) {
        enum class Progress {
            NotStarted,
            InProgress,
            Completed
        }

        val isCompleted = progress == Progress.Completed
        val isNotStarted = progress == Progress.NotStarted

        override fun equals(other: Any?): Boolean {
            return this::class.java == (other ?: return false)::class.java
        }

        override fun hashCode(): Int {
            return progress.hashCode()
        }
    }

    /**
     * Automatically displays the given [BaseIntelPlugin] when the quest's [Stage.Progress] is in progress.
     * Marks the intel as complete once the quest's progress is complete.
     */
    open class AutoIntelInfo<T : BaseIntelPlugin>(
        val intelClass: Class<T>,
        val intelCreator: () -> T
    )

    /**
     * If this quest is found at a bar, return a [AutoBarEventInfo] object.
     * The [BaseBarEventCreator] will automatically be added to [BarEventManager].
     * Return `null` if the quest is not found at a bar, or to manage this by yourself.
     *
     * @param shouldGenerateBarEvent Whether the bar event should be generated at all. If false, the [BaseBarEventCreator]
     *   will not be added and [shouldOfferFromMarket] will never be called, nor will `regenerateQuest`.
     *   Use this to limit when a quest should be offered, such as a player level or fleet power.
     * @param shouldOfferFromMarket Whether the quest should be offered or not. `regenerateQuest` is called before this.
     */
    open class AutoBarEventInfo(
        val barEventCreator: BaseBarEventCreator,
        internal val shouldGenerateBarEvent: () -> Boolean,
        private val shouldOfferFromMarket: (MarketAPI) -> Boolean
    ) {
        internal var stage: (() -> Stage)? = null

        /**
         * Whether the quest should be offered at the [MarketAPI] of the specified bar.
         * Only override for very custom logic, such as if the quest should still be offered even if it is complete.
         */
        open fun shouldOfferFromMarketInternal(market: MarketAPI): Boolean =
            stage?.invoke()?.progress != Stage.Progress.Completed
                    && market.isValidQuestTarget
                    && shouldGenerateBarEvent()
                    && shouldOfferFromMarket(market)
    }
}