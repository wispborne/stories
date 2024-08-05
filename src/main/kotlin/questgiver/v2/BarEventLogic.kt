package wisp.questgiver.v2

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson
import wisp.questgiver.OnInteractionStarted
import java.awt.Color

typealias CreateInteractionPrompt<S> = S.() -> Unit
typealias TextToStartInteraction<S> = S.() -> BarEventLogic.Option

/**
 * Defines a [BaseBarEventWithPerson]. Create the [BaseBarEventWithPerson] by calling [buildBarEvent].
 * @param specId The mission/bar event id.
 * @param createInteractionPrompt Add text/images to the bar to show that this event is present,
 *   e.g. "A man is searching for something in the corner."
 * @param textToStartInteraction The option available to the player to start the event, e.g. "Help the man."
 * @param onInteractionStarted Called when the player chooses to start the bar event.
 * @param pages A list of [wisp.questgiver.InteractionDefinition.Page]s that define the structure of the conversation.
 */
open class BarEventLogic<H : QGHubMissionWithBarEvent>(
    @Transient internal var createInteractionPrompt: CreateInteractionPrompt<BarEventLogic<H>>,
    @Transient internal var textToStartInteraction: TextToStartInteraction<BarEventLogic<H>>,
    override var people: PeopleSelector<BarEventLogic<H>>? = null,
    override var firstPageSelector: FirstPageSelector<BarEventLogic<H>>? = null,
    override var onInteractionStarted: OnInteractionStarted<BarEventLogic<H>>?,
    override var pages: List<IInteractionLogic.Page<BarEventLogic<H>>>,
) : IInteractionLogic<BarEventLogic<H>>//(
{
    override lateinit var dialog: InteractionDialogAPI

    internal lateinit var missionGetter: () -> H

    /**
     * The HubMission for this bar event.
     * Available as a field variable for the implementing [BarEventLogic].
     */
    val mission: H
        get() = missionGetter.invoke()

    lateinit var event: BaseBarEvent

    internal lateinit var closeBarEvent: (doNotOfferAgain: Boolean) -> Unit

    final override var navigator = object : InteractionDialogLogic.PageNavigator<BarEventLogic<H>>(this) {
        override fun close(doNotOfferAgain: Boolean) {
            closeBarEvent.invoke(doNotOfferAgain)
        }
    }
        internal set


    data class Option(
        val text: String,
        val textColor: Color?,
        val tooltip: String? = null
    )
}