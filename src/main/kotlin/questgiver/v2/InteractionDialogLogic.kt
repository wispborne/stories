package wisp.questgiver.v2

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import wisp.questgiver.Questgiver.game
import wisp.questgiver.v2.IInteractionLogic.Companion.CONTINUE_BUTTON_ID
import wisp.questgiver.wispLib.ServiceLocator
import wisp.questgiver.wispLib.showPeople

/**
 * Implement this to create an interaction dialog anywhere - not a bar event.
 * Unlike a bar event which has one HubMisison, this can be tied to zero or more HubMissions.
 * To show the dialog, use `Dialog().build().show(game.sector.campaignUI, game.sector.playerFleet)`.
 *
 * To use a HubMission in the dialog logic, you can create a default constructor param
 * such as `mission: Telos1HubMission = game.sector.intelManager.findFirst()!!`.
 */
abstract class InteractionDialogLogic<S : InteractionDialogLogic<S>>(
    @Transient override var onInteractionStarted: OnInteractionStarted<S>? = null,
    @Transient override var people: PeopleSelector<S>? = null,
    @Transient override var firstPageSelector: FirstPageSelector<S>? = null,
    @Transient final override var pages: List<IInteractionLogic.Page<S>>
) : IInteractionLogic<S> {

    init {
        if (pages.distinctBy { it.id }.count() != pages.count())
            error("All page ids must have a unique id. Page ids: ${pages.joinToString { it.id.toString() }} Dialog: $this")
    }


    /**
     * Coordinator for dialog page navigation.
     * This is what is exposed to users of Questgiver.
     *
     * Not serialized.
     */
    open class PageNavigator<S : IInteractionLogic<S>>(
        internal var interactionDefinition: IInteractionLogic<S>?
    ) : IInteractionLogic.IPageNavigator<S> {
        private val pages
            get() = interactionDefinition!!.pages
        private val dialog
            get() = interactionDefinition!!.dialog
        val doOnCloseActions = mutableListOf<() -> Unit>()

        /**
         * Function to execute after user presses "Continue" to resume a page.
         */
        private var continuationOfPausedPage: (() -> Unit)? = null
        private var currentPage: IInteractionLogic.Page<S>? = null
        internal val isWaitingOnUserToPressContinue: Boolean
            get() = continuationOfPausedPage != null

        override fun currentPage(): IInteractionLogic.Page<S>? = currentPage

        /**
         * Navigates to the specified dialogue page.
         */
        override fun goToPage(pageId: Any) {
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
        override fun goToPage(page: IInteractionLogic.Page<S>) {
            showPage(page)
        }

        /**
         * Closes the dialog.
         * @param doNotOfferAgain If true, the prompt will not be displayed in the bar while the player
         *   is still there. If false, allows the player to immediately change their mind and trigger the interaction again.
         */
        override fun close(doNotOfferAgain: Boolean) {
            doOnCloseActions.forEach { it.invoke() }
            dialog.dismiss()
            destroy()
        }

        /**
         * Refreshes the page's options without fully re-displaying the page.
         * Useful for showing/hiding certain options after choosing one.
         */
        override fun refreshOptions() {
            runCatching {
                if (!isWaitingOnUserToPressContinue) {
                    game.logger.d { "Clearing options." }
                    dialog.optionPanel.clearOptions()
                    showOptions(currentPage!!.options)
                } else {
                    game.logger.d { "Not clearing options because we are at a 'Continue' pause (an option without a page, so we can't refresh from a page)." }
                }
            }.onFailure { game.logger.w(it) }
        }

        /**
         * Displays a new page of the dialogue.
         */
        override fun showPage(page: IInteractionLogic.Page<S>) {
            game.logger.d { "Clearing options." }
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

            // Call onPageTurned on the previous page.
            page.onPageTurned?.invoke(interactionDefinition as S)

            currentPage = page
            page.onPageShown(interactionDefinition as S)

            page.people?.invoke(interactionDefinition as S)
                ?.also { people -> dialog.visualPanel.showPeople(people) }

            if (!isWaitingOnUserToPressContinue) {
                showOptions(page.options)
            }
        }

        /**
         * Show the player a "Continue" button to break up dialog without creating a new Page object.
         */
        override fun promptToContinue(continueText: String, continuation: () -> Unit) {
            continuationOfPausedPage = continuation
            game.logger.d { "Clearing options." }
            dialog.optionPanel.clearOptions()

            game.logger.d { "Adding option $CONTINUE_BUTTON_ID with text '$continueText'." }
            dialog.optionPanel.addOption(continueText, CONTINUE_BUTTON_ID)
        }

        override fun onUserPressedContinue() {
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

        override fun showOptions(options: List<IInteractionLogic.Option<S>>) {
            options
                .filter { option ->
                    (option.hideOptionIfFlagTrue == null) || (game.memory[option.hideOptionIfFlagTrue] != true)
                }
                .filter { it.showIf(interactionDefinition as S) }
                .forEach { option ->
                    val text = option.text(interactionDefinition as S)
                    game.logger.d { "Adding option ${option.id} with text '$text' and shortcut ${option.shortcut}." }

                    if (option.textColor != null) {
                        dialog.optionPanel.addOption(
                            /* text = */ text,
                            /* data = */ option.id,
                            /* color = */ option.textColor,
                            /* tooltip = */ option.tooltip?.invoke(interactionDefinition as S)
                        )
                    } else {
                        dialog.optionPanel.addOption(
                            /* text = */ text,
                            /* data = */ option.id,
                            /* tooltip = */ option.tooltip?.invoke(interactionDefinition as S)
                        )
                    }

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


        override fun onOptionSelected(optionText: String?, optionData: Any?) {
            game.logger.i { "Selected option $optionText with data $optionData" }

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

                optionSelected.onOptionSelected(interactionDefinition as S, this)
            }
        }

        fun destroy() {
            game.logger.i { "Destroying navigator $this." }
            interactionDefinition = null
        }
    }

    fun destroy() {
        navigator.destroy()
    }

    /**
     * Access to the dialog to assume direct control.
     */
    @Transient
    override lateinit var dialog: InteractionDialogAPI
    final override var navigator = PageNavigator(this)
        internal set

    fun build(): InteractionDialog<S> =
        object : InteractionDialog<S>() {
            override fun createInteractionDialogLogic(): S = this@InteractionDialogLogic as S
        }
}

fun IInteractionLogic.Image.spriteName(game: ServiceLocator) = game.settings.getSpriteName(this.category, this.id)
fun IInteractionLogic.Image.spritePath(game: ServiceLocator) = this.spriteName(game)