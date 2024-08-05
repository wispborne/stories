package wisp.questgiver.v2

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent
import wisp.questgiver.Questgiver.game
import wisp.questgiver.isValidQuestTarget
import wisp.questgiver.wispLib.addOption
import wisp.questgiver.wispLib.showPeople

/**
 * Custom Questgiver bar event, subclass of [BaseBarEvent]. Implement this.
 */
abstract class BarEvent<H : QGHubMissionWithBarEvent>(barEventSpecId: String) :
    HubMissionBarEventWrapperWithoutRules<H>(barEventSpecId) {
    abstract fun createBarEventLogic(): BarEventLogic<H>

    @Transient
    private var barEventLogic: BarEventLogic<H> = setupBarEventLogic()

    override fun readResolve(): Any {
        barEventLogic = setupBarEventLogic()

        return super.readResolve()
    }

    private fun setupBarEventLogic(): BarEventLogic<H> {
        return createBarEventLogic().also { logic ->
            logic.missionGetter = { this.mission!! }
        }
    }

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean =
        super.shouldShowAtMarket(market)
                && (market?.isValidQuestTarget ?: true)
                && mission?.result == null

    /**
     * Set up the text that appears when the player goes to the bar
     * and the option for them to init the conversation.
     */
    override fun addPromptAndOption(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI?>) {
        super.addPromptAndOption(dialog, memoryMap)
        if (mission == null) return // super.addPromptAndOption does this too.

        barEventLogic.dialog = dialog
        barEventLogic.event = this
        barEventLogic.createInteractionPrompt.invoke(barEventLogic)

        val option = barEventLogic.textToStartInteraction.invoke(barEventLogic)
        game.logger.i { "Adding prompt and option '${option.text}'." }

        dialog.optionPanel.addOption(
            text = option.text,
            data = this as BaseBarEvent,
            color = option.textColor,
            tooltip = option.tooltip
        )
    }

    /**
     * Called when the player chooses to start the conversation.
     */
    override fun init(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        super.init(dialog, memoryMap)
        barEventLogic.dialog = dialog
        barEventLogic.event = this
        game.logger.i { "Init dialog '${this.barEventId}'." }

        barEventLogic.people?.invoke(barEventLogic)
            ?.also { people -> dialog.visualPanel.showPeople(people) }

        // Set bar event close logic.
        barEventLogic.closeBarEvent = { doNotOfferAgain ->
            game.logger.i { "Closing dialog ${barEventLogic.event.barEventId}. Offer again? ${!doNotOfferAgain}" }
            if (doNotOfferAgain) {
                BarEventManager.getInstance().notifyWasInteractedWith(this)
            }

            done = true
            noContinue = true
        }

        this.done = false
        this.noContinue = false

        barEventLogic.onInteractionStarted?.invoke(barEventLogic)

        if (barEventLogic.pages.any()) {
            // If `firstPageSelector` is defined, show that page, otherwise show the first page.
            barEventLogic.navigator.showPage(
                barEventLogic.firstPageSelector?.invoke(barEventLogic.pages)
                    ?: barEventLogic.pages.first()
            )
        }
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        barEventLogic.navigator.onOptionSelected(optionText, optionData)
    }

    fun showPage(page: IInteractionLogic.Page<BarEventLogic<H>>) {
        if (noContinue || done) return

        barEventLogic.navigator.showPage(page)
    }
}