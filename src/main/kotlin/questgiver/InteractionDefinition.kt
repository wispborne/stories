package wisp.questgiver

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.util.Misc
import wisp.questgiver.Questgiver.game
import wisp.questgiver.wispLib.ParagraphText
import wisp.questgiver.wispLib.ServiceLocator
import wisp.questgiver.wispLib.addPara
import java.awt.Color

typealias OnPageShown<S> = S.() -> Unit
typealias OnOptionSelected<S> = S.(InteractionDefinition<*>.PageNavigator) -> Unit
typealias OnInteractionStarted<S> = S.() -> Unit

@Deprecated(
    "Use wisp.questgiver.v2.InteractionDialogLogic instead.",
    replaceWith = ReplaceWith("InteractionDialogLogic<S>", "wisp.questgiver.v2.InteractionDialogLogic")
)
abstract class InteractionDefinition<S : InteractionDefinition<S>>(
    @Transient internal var onInteractionStarted: OnInteractionStarted<S> = {},
    @Transient internal var people: List<PersonAPI>? = null,
    @Transient internal var pages: List<Page<S>>,
    @Transient private var shouldValidateOnDialogStart: Boolean = true
) {
    @Deprecated("Use wisp.questgiver.v2.IInteractionLogic.Page instead.",
        replaceWith = ReplaceWith("Page<S>", "wisp.questgiver.v2.Page"))
    class Page<S>(
        val id: Any,
        val image: Image? = null,
        val onPageShown: OnPageShown<S>,
        val options: List<Option<S>>
    )

    @Deprecated("Use wisp.questgiver.v2.IInteractionLogic.Option instead.",
        replaceWith = ReplaceWith("Option<S>", "wisp.questgiver.v2.Option"))
    open class Option<S>(
        val text: S.() -> String,
        val shortcut: Shortcut? = null,
        val showIf: S.() -> Boolean = { true },
        val onOptionSelected: OnOptionSelected<S>,
        val id: String = Misc.random.nextInt().toString()
    )

    init {
        if (pages.distinctBy { it.id }.count() != pages.count())
            error("All page ids must have a unique id. Page ids: ${pages.joinToString { it.id.toString() }} Dialog: $this")
    }

    /**
     * Create an instance of the implementing class. We then copy the transient fields in that class
     * to this one in [readResolve], since they do not get created by the deserializer.
     * We cannot use `this::class.java.newInstance()` because then the implementing class is required to have
     * a no-args constructor.
     */
    abstract fun createInstanceOfSelf(): InteractionDefinition<S>

    /**
     * When this class is created by deserializing from a save game,
     * it can't deserialize the anonymous methods, so we mark them as transient,
     * then manually assign them using this method, which gets called automagically
     * by the XStream serializer.
     */
    open fun readResolve(): Any {
        val newInstance = createInstanceOfSelf()
        onInteractionStarted = newInstance.onInteractionStarted
        people = newInstance.people
        pages = newInstance.pages
        shouldValidateOnDialogStart = newInstance.shouldValidateOnDialogStart
        return this
    }

    companion object {
        /**
         * Special button data that indicates the dialog page has a break in it to wait for the player to
         * press Continue.
         */
        internal const val CONTINUE_BUTTON_ID = "questgiver_continue_button_id"
    }

    /**
     * Coordinator for dialog page navigation.
     */
    open inner class PageNavigator() {
        /**
         * Function to execute after user presses "Continue" to resume a page.
         */
        private var continuationOfPausedPage: (() -> Unit)? = null
        private var currentPage: Page<S>? = null
        internal val isWaitingOnUserToPressContinue: Boolean
            get() = continuationOfPausedPage != null

        /**
         * Navigates to the specified dialogue page.
         */
        open fun goToPage(pageId: Any) {
            showPage(
                pages.firstOrNull { (it.id == pageId) || (it.id.toString() == pageId.toString()) }
                    ?: throw NoSuchElementException(
                        "No page with id '$pageId'." +
                                "\nPages: ${pages.joinToString { "'${it.id}'" }}."
                    )
            )
        }

        /**
         * Navigates to the specified dialogue page.
         */
        open fun goToPage(page: Page<S>) {
            showPage(page)
        }

        /**
         * Closes the dialog.
         * @param doNotOfferAgain If true, the prompt will not be displayed in the bar while the player
         *   is still there. If false, allows the player to immediately change their mind and trigger the interaction again.
         */
        open fun close(doNotOfferAgain: Boolean) {
            dialog.dismiss()
        }

        /**
         * Refreshes the page's options without fully re-displaying the page.
         * Useful for showing/hiding certain options after choosing one.
         */
        open fun refreshOptions() {
            game.logger.d { "Clearing options." }
            dialog.optionPanel.clearOptions()

            if (!isWaitingOnUserToPressContinue) {
                showOptions(currentPage!!.options)
            }
        }

        /**
         * Displays a new page of the dialogue.
         */
        open fun showPage(page: Page<S>) {
            dialog.optionPanel.clearOptions()

            if (page.image != null) {
                dialog.visualPanel.showImagePortion(
                    page.image.category,
                    page.image.id,
                    page.image.width,
                    page.image.height,
                    page.image.xOffset,
                    page.image.yOffset,
                    page.image.displayWidth,
                    page.image.displayHeight
                )
            }

            currentPage = page
            page.onPageShown(this@InteractionDefinition as S)

            if (!isWaitingOnUserToPressContinue) {
                showOptions(page.options)
            }
        }

        /**
         * Show the player a "Continue" button to break up dialog without creating a new Page object.
         */
        fun promptToContinue(continueText: String, continuation: () -> Unit) {
            continuationOfPausedPage = continuation
            game.logger.d { "Clearing options." }
            dialog.optionPanel.clearOptions()

            game.logger.d { "Adding option $CONTINUE_BUTTON_ID with text '$continueText'." }
            dialog.optionPanel.addOption(continueText, CONTINUE_BUTTON_ID)
        }

        internal fun onUserPressedContinue() {
            game.logger.d { "Clearing options." }
            dialog.optionPanel.clearOptions()

            // Save the continuation for execution
            val continuation = continuationOfPausedPage
            // Wipe the field variable because execution of the continuation
            // may set a new field variable (eg if there are nested pauses)
            continuationOfPausedPage = null

            // If we didn't just enter a nested pause, finally show the page options
            if (!isWaitingOnUserToPressContinue) {
                currentPage?.let { showOptions(it.options) }
            }

            continuation?.invoke()
        }

        internal fun <S : InteractionDefinition<S>> showOptions(options: List<Option<S>>) {
            options
                .filter { it.showIf(this@InteractionDefinition as S) }
                .forEach { option ->
                    val text = option.text(this@InteractionDefinition as S)
                    game.logger.d { "Adding option ${option.id} with text '$text' and shortcut ${option.shortcut}." }
                    dialog.optionPanel.addOption(text, option.id)

                    if (option.shortcut != null) {
                        dialog.optionPanel.setShortcut(
                            option.id,
                            option.shortcut.code,
                            option.shortcut.holdCtrl,
                            option.shortcut.holdAlt,
                            option.shortcut.holdShift,
                            false
                        )
                    }
                }
        }


        internal fun onOptionSelected(optionText: String?, optionData: Any?) {
            // If they pressed continue, resume the dialog interaction
            if (optionData == CONTINUE_BUTTON_ID) {
                onUserPressedContinue()
            } else {
                // Otherwise, look for the option they pressed
                val optionSelected = pages
                    .flatMap { page ->
                        page.options
                            .filter { option -> option.id == optionData }
                    }.firstOrNull()
                    ?: return

                optionSelected.onOptionSelected(this@InteractionDefinition as S, navigator)
            }
        }
    }

    /**
     * @param code constant from [org.lwjgl.input.Keyboard]
     */
    data class Shortcut(
        val code: Int,
        val holdCtrl: Boolean = false,
        val holdAlt: Boolean = false,
        val holdShift: Boolean = false
    )

    @Deprecated("Use wisp.questgiver.v2.IInteractionLogic.Image instead.",
        replaceWith = ReplaceWith("Image", "wisp.questgiver.v2.IInteractionLogic.Image"))
    open class Image(
        val category: String,
        val id: String,
        val width: Float,
        val height: Float,
        val xOffset: Float,
        val yOffset: Float,
        val displayWidth: Float,
        val displayHeight: Float
    )

    @Deprecated("Use wisp.questgiver.v2.IInteractionLogic.Portrait instead.",
        replaceWith = ReplaceWith("Portrait", "wisp.questgiver.v2.IInteractionLogic.Portrait"))
    class Portrait(
        category: String,
        id: String
    ) : Image(
        category = category,
        id = id,
        width = 128f,
        height = 128f,
        xOffset = 0f,
        yOffset = 0f,
        displayWidth = 128f,
        displayHeight = 128f
    )

    @Deprecated("Use wisp.questgiver.v2.IInteractionLogic.Illustration instead.",
        replaceWith = ReplaceWith("Illustration", "wisp.questgiver.v2.IInteractionLogic.Illustration"))
    class Illustration(
        category: String,
        id: String
    ) : Image(
        category = category,
        id = id,
        width = 640f,
        height = 400f,
        xOffset = 0f,
        yOffset = 0f,
        displayWidth = 480f,
        displayHeight = 300f
    )

    /**
     * Access to the dialog to assume direct control.
     */
    @Transient
    lateinit var dialog: InteractionDialogAPI
    var navigator = PageNavigator()
        internal set

    /**
     * Prints the text returned by [stringMaker] to the dialog's text panel.
     *
     * @param stringMaker A function that returns the text to display.
     */
    fun para(
        textColor: Color = Misc.getTextColor(),
        highlightColor: Color = Misc.getHighlightColor(),
        stringMaker: ParagraphText.() -> String
    ): LabelAPI? = dialog.textPanel.addPara(textColor, highlightColor, stringMaker)

    /**
     * Needed so we can figure out which BarEvents are part of this mod
     * when looking at [BarEventManager.getInstance().active.items].
     */
    abstract inner class InteractionDialog : InteractionDialogPlugin

    open fun build(): InteractionDialog = InteractionDialogImpl()


    internal open inner class InteractionDialogImpl : InteractionDialog() {
        /**
         * Called when this class is instantiated.
         */
        init {
            if (shouldValidateOnDialogStart) {

            }
        }

        /**
         * Called when the dialog is shown.
         */
        override fun init(dialog: InteractionDialogAPI) {
            this@InteractionDefinition.dialog = dialog
            val peopleInner = this@InteractionDefinition.people

            if (peopleInner?.getOrNull(0) != null) {
                if (peopleInner.size == 1) {
                    dialog.visualPanel.showPersonInfo(peopleInner[0], true)
                } else {
                    dialog.visualPanel.showPersonInfo(peopleInner[0], true, false)
                }
            }

            if (peopleInner?.getOrNull(1) != null) {
                dialog.visualPanel.showSecondPerson(peopleInner[1])
            }

            if (peopleInner?.getOrNull(2) != null) {
                dialog.visualPanel.showThirdPerson(peopleInner[2])
            }

            onInteractionStarted(this@InteractionDefinition as S)

            if (pages.any()) {
                navigator.showPage(pages.first())
            }
        }

        override fun optionSelected(optionText: String?, optionData: Any?) {
            if (optionText != null) {
                // Print out the text of the option the user just selected
                para(textColor = Global.getSettings().getColor("buttonText")) { optionText }
            }

            navigator.onOptionSelected(optionText, optionData)
        }

        // Other overrides that are necessary but do nothing
        override fun optionMousedOver(optionText: String?, optionData: Any?) {
        }

        override fun getMemoryMap(): MutableMap<String, MemoryAPI> = mutableMapOf()
        override fun backFromEngagement(battleResult: EngagementResultAPI?) {
        }

        override fun advance(amount: Float) {
        }

        override fun getContext(): Any? = null
    }
}

fun InteractionDefinition.Image.spriteName(game: ServiceLocator) = game.settings.getSpriteName(this.category, this.id)
fun InteractionDefinition.Image.spritePath(game: ServiceLocator) = this.spriteName(game)