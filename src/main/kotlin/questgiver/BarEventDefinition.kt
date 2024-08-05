package wisp.questgiver

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventWithPerson

typealias CreateInteractionPrompt<S> = S.() -> Unit
typealias TextToStartInteraction<S> = S.() -> String

/**
 * Defines a [BaseBarEventWithPerson]. Create the [BaseBarEventWithPerson] by calling [buildBarEvent].
 * @param questFacilitator The [QuestFacilitator] for the quest.
 * @param createInteractionPrompt Add text/images to the bar to show that this event is present,
 *   e.g. "A man is searching for something in the corner."
 * @param textToStartInteraction The option available to the player to start the event, e.g. "Help the man."
 * @param onInteractionStarted Called when the player chooses to start the bar event.
 * @param pages A list of [wisp.questgiver.InteractionDefinition.Page]s that define the structure of the conversation.
 */
@Deprecated("Use v2")
abstract class BarEventDefinition<S : InteractionDefinition<S>>(
    @Transient private var shouldShowAtMarket: (market: MarketAPI) -> Boolean,
    @Transient internal var createInteractionPrompt: CreateInteractionPrompt<S>,
    @Transient internal var textToStartInteraction: TextToStartInteraction<S>,
    onInteractionStarted: OnInteractionStarted<S>,
    pages: List<Page<S>>,
    people: List<PersonAPI>? = null,
) : InteractionDefinition<S>(
    onInteractionStarted = onInteractionStarted,
    people = people,
    pages = pages
) {

    lateinit var manOrWoman: String
    lateinit var hisOrHer: String
    lateinit var heOrShe: String
    lateinit var event: BaseBarEventWithPerson

    /**
     * When this class is created by deserializing from a save game,
     * it can't deserialize the anonymous methods, so we mark them as transient,
     * then manually assign them using this method, which gets called automagically
     * by the XStream serializer.
     */
    override fun readResolve(): Any {
        val newInstance = this::class.java.newInstance()
        shouldShowAtMarket = newInstance.shouldShowAtMarket
        createInteractionPrompt = newInstance.createInteractionPrompt
        textToStartInteraction = newInstance.textToStartInteraction
        return super.readResolve()
    }

    open fun buildBarEvent(): BarEvent = BarEvent()

    /**
     * Needed so we can figure out which BarEvents are part of this mod
     * when looking at [BarEventManager.getInstance().active.items].
     */
    open inner class BarEvent : BaseBarEventWithPerson() {
        init {
            navigator = object : InteractionDefinition<S>.PageNavigator() {

                override fun close(doNotOfferAgain: Boolean) {
                    if (doNotOfferAgain) {
                        BarEventManager.getInstance().notifyWasInteractedWith(event)
                    }

                    done = true
                    noContinue = true
                }
            }
        }

        override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
            regen(market)

            return super.shouldShowAtMarket(market)
                    && (market?.isValidQuestTarget ?: true)
                    && (market?.let { this@BarEventDefinition.shouldShowAtMarket(market) } ?: true)
        }

        /**
         * Set up the text that appears when the player goes to the bar
         * and the option for them to init the conversation.
         */
        override fun addPromptAndOption(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
            super.addPromptAndOption(dialog, memoryMap)
            this@BarEventDefinition.manOrWoman = manOrWoman
            this@BarEventDefinition.hisOrHer = hisOrHer
            this@BarEventDefinition.heOrShe = heOrShe
            this@BarEventDefinition.dialog = dialog
            this@BarEventDefinition.event = this
            createInteractionPrompt.invoke(this@BarEventDefinition as S)

            dialog.optionPanel.addOption(
                textToStartInteraction.invoke(this@BarEventDefinition),
                this as BaseBarEventWithPerson
            )
        }

        /**
         * Called when the player chooses to start the conversation.
         */
        override fun init(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
            super.init(dialog, memoryMap)
            val firstPerson = this@BarEventDefinition.people?.firstOrNull()

            if (firstPerson?.name != null) {
                this.person.apply { name = firstPerson.name }
            }

            this.done = false
            this.noContinue = false

            onInteractionStarted.invoke(this@BarEventDefinition as S)

            if (pages.any()) {
                showPage(pages.first())
            }
        }

        override fun optionSelected(optionText: String?, optionData: Any?) {
            navigator.onOptionSelected(optionText, optionData)
        }

        fun showPage(page: Page<S>) {
            if (noContinue || done) return

            navigator.showPage(page)
        }

        override fun getPersonFaction(): String? = this@BarEventDefinition.people?.firstOrNull()?.faction?.id
            ?: super.getPersonFaction()

        override fun getPersonGender(): FullName.Gender? = this@BarEventDefinition.people?.firstOrNull()?.gender
            ?: super.getPersonGender()

        override fun getPersonPortrait(): String? = this@BarEventDefinition.people?.firstOrNull()?.portraitSprite
            ?: super.getPersonPortrait()

        override fun getPersonPost(): String? = this@BarEventDefinition.people?.firstOrNull()?.postId
            ?: super.getPersonPost()

        override fun getPersonRank(): String? = this@BarEventDefinition.people?.firstOrNull()?.rankId
            ?: personPost
            ?: super.getPersonRank()
    }
}