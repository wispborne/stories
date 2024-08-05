package wisp.questgiver

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.characters.PersonAPI

@Deprecated("Use v2")
abstract class AutoInteractionDefinition<S : InteractionDefinition<S>>(
    private val questFacilitator: AutoQuestFacilitator,
    onInteractionStarted: S.() -> Unit = {},
    people: List<PersonAPI>? = null,
    pages: List<InteractionDefinition.Page<S>>,
    shouldValidateOnDialogStart: Boolean = true
) : InteractionDefinition<S>(
    onInteractionStarted = onInteractionStarted,
    people = people,
    pages = pages,
    shouldValidateOnDialogStart = shouldValidateOnDialogStart
) {
    internal inner class AutoInteractionDialogImpl : InteractionDialogImpl() {
        override fun init(dialog: InteractionDialogAPI) {
            if (questFacilitator.stage.progress == AutoQuestFacilitator.Stage.Progress.NotStarted) {
                questFacilitator.regenerateQuest(dialog.interactionTarget, dialog.interactionTarget.market)
            }

            super.init(dialog)
        }
    }
}