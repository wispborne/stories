package wisp.perseanchronicles.telos.pt1_deliveryToEarth

import org.json.JSONObject
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.pt2_dart.Telos2HubMission
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.InteractionDialogLogic
import wisp.questgiver.v2.json.PagesFromJson
import wisp.questgiver.v2.json.query
import wisp.questgiver.wispLib.findFirst

class Telo1CompleteDialog(
    stageJson: JSONObject = Telos1HubMission.part1Json.query("/stages/deliveryDropoff"),
    mission: Telos1HubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<Telo1CompleteDialog>(
    onInteractionStarted = {
        this.dialog.visualPanel.showImagePortion(IInteractionLogic.Illustration("wisp_perseanchronicles_telos", "shipInSpace"))
    },
    pages = PagesFromJson(
        pagesJson = stageJson.query("/pages"),
        onPageShownHandlersByPageId = mapOf(
            "2" to {
                this.dialog.visualPanel.showPersonInfo(PerseanChroniclesNPCs.karengo)
            },
            "3" to {
                mission.setCurrentStage(Telos1HubMission.Stage.Completed, dialog, null)

                // Start Part 2 on finishing dialog.
                Telos2HubMission().apply {
                    if (create(null, false))
                        accept(dialog, null)
                }
            }
        ),
        optionConfigurator = { options ->
            options.map { option ->
                when (option.id) {
                    "close" -> option.copy(
                        onOptionSelected = {
                            it.close(doNotOfferAgain = true)
                        })

                    else -> option
                }
            }
        }
    ),
)