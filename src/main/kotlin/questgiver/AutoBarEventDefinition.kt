package wisp.questgiver

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import wisp.questgiver.Questgiver.game

@Deprecated("Use v2")
abstract class AutoBarEventDefinition<S : InteractionDefinition<S>>(
    @Transient private var questFacilitator: AutoQuestFacilitator,
    createInteractionPrompt: CreateInteractionPrompt<S>,
    textToStartInteraction: TextToStartInteraction<S>,
    onInteractionStarted: OnInteractionStarted<S>,
    pages: List<Page<S>>,
    people: List<PersonAPI>? = null,
) : BarEventDefinition<S>(
    shouldShowAtMarket = { questFacilitator.autoBarEventInfo?.shouldOfferFromMarketInternal(it) ?: true },
    createInteractionPrompt = createInteractionPrompt,
    textToStartInteraction = textToStartInteraction,
    onInteractionStarted = onInteractionStarted,
    pages = pages,
    people = people
) {


    /**
     * When this class is created by deserializing from a save game,
     * it can't deserialize the anonymous methods, so we mark them as transient,
     * then manually assign them using this method, which gets called automagically
     * by the XStream serializer.
     */
    override fun readResolve(): Any {
        val newInstance = this::class.java.newInstance()
        questFacilitator = newInstance.questFacilitator
        return super.readResolve()
    }

    override fun buildBarEvent(): BarEvent = AutoBarEvent()

    open inner class AutoBarEvent : BarEvent() {
        override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
            if (questFacilitator.stage.progress == AutoQuestFacilitator.Stage.Progress.NotStarted) {
                questFacilitator.regenerateQuest(
                    game.sector.campaignUI.currentInteractionDialog.interactionTarget,
                    market
                )
            }

            return super.shouldShowAtMarket(market)
        }
    }
}