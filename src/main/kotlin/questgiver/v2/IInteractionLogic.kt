package wisp.questgiver.v2

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.VisualPanelAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.ui.LabelAPI
import com.fs.starfarer.api.util.Misc
import wisp.questgiver.wispLib.ParagraphText
import wisp.questgiver.wispLib.addPara
import java.awt.Color

typealias OnPageShown<S> = S.() -> Unit
typealias OnOptionSelected<S> = S.(IInteractionLogic.IPageNavigator<S>) -> Unit
typealias OnInteractionStarted<S> = S.() -> Unit
typealias PeopleSelector<S> = S.() -> List<PersonAPI>
typealias FirstPageSelector<S> = List<IInteractionLogic.Page<S>>.() -> IInteractionLogic.Page<S>

interface IInteractionLogic<S : IInteractionLogic<S>> {
    val onInteractionStarted: OnInteractionStarted<S>?
    val people: PeopleSelector<S>?
    val firstPageSelector: FirstPageSelector<S>?
    val pages: List<Page<S>>

    /**
     * Access to the dialog to assume direct control.
     */
    val dialog: InteractionDialogAPI
    val navigator: IPageNavigator<S>

    data class Page<S : IInteractionLogic<S>>(
        val id: Any,
        val image: Image? = null,
        /**
         * Called when this page is shown.
         */
        val onPageShown: OnPageShown<S>,
        /**
         * Called when the user leaves this page.
         */
        val onPageTurned: OnPageShown<S>? = null,
        val options: List<Option<S>>,
        /**
         * Show people on the side. Persists across pages.
         * Set to `emptyList` to remove people.
         */
        val people: PeopleSelector<S>? = null,
        /**
         * Can put anything in here.
         * [PagesFromJson] adds the json representation of the page.
         */
        val extraData: Map<String, Any> = emptyMap(),
    )

    /**
     * @param disableAutomaticHandling If true, page navigation and options clear/display will not happen.
     *   You will need to do this manually after this option is selected.
     */
    data class Option<S : IInteractionLogic<S>>(
        val id: String = Misc.random.nextInt().toString(),
        val text: S.() -> String,
        val textColor: Color? = null,
        val tooltip: (S.() -> String)? = null,
        val shortcut: Shortcut? = null,
        val showIf: S.() -> Boolean = { true },
        val disableAutomaticHandling: Boolean = false,
        val flagToSet: String? = null,
        val hideOptionIfFlagTrue: String? = null,
        val onOptionSelected: OnOptionSelected<S>,
    )

    /**
     * @param code constant from [org.lwjgl.input.Keyboard]
     */
    data class Shortcut(
        val code: Int,
        val holdCtrl: Boolean = false,
        val holdAlt: Boolean = false,
        val holdShift: Boolean = false
    )

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

    fun VisualPanelAPI?.showImagePortion(image: Image) =
        this?.showImagePortion(
            image.category,
            image.id,
            image.width,
            image.height,
            image.xOffset,
            image.yOffset,
            image.displayWidth,
            image.displayHeight
        )

    interface IPageNavigator<S : IInteractionLogic<S>> {
        /**
         * Returns the current page.
         */
        fun currentPage(): Page<S>?

        /**
         * Navigates to the specified dialogue page.
         */
        fun goToPage(pageId: Any)

        /**
         * Navigates to the specified dialogue page.
         */
        fun goToPage(page: Page<S>)

        /**
         * Closes the dialog.
         * @param doNotOfferAgain If true, the prompt will not be displayed in the bar while the player
         *   is still there. If false, allows the player to immediately change their mind and trigger the interaction again.
         */
        fun close(doNotOfferAgain: Boolean)

        /**
         * Refreshes the page's options without fully re-displaying the page.
         * Useful for showing/hiding certain options after choosing one.
         */
        fun refreshOptions()

        /**
         * Displays a new page of the dialogue.
         */
        fun showPage(page: Page<S>)

        /**
         * Show the player a "Continue" button to break up dialog without creating a new Page object.
         */
        fun promptToContinue(continueText: String, continuation: () -> Unit)
        fun onUserPressedContinue()
        fun showOptions(options: List<Option<S>>)
        fun onOptionSelected(optionText: String?, optionData: Any?)
    }


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

    companion object {
        /**
         * Special button data that indicates the dialog page has a break in it to wait for the player to
         * press Continue.
         */
        const val CONTINUE_BUTTON_ID = "questgiver_continue_button_id"
    }
}