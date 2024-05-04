package wisp.perseanchronicles.telos.pt3_arrow

import org.json.JSONObject
import wisp.perseanchronicles.game
import wisp.questgiver.v2.InteractionDialogLogic
import wisp.questgiver.v2.json.PagesFromJson
import wisp.questgiver.v2.json.query
import wisp.questgiver.wispLib.findFirst

class Telos3EscapedDialog(
    stageJson: JSONObject = Telos3HubMission.part3Json.query("/stages/escaped"),
    mission: Telos3HubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<Telos3EscapedDialog>(
//    people = { listOfNotNull(PerseanChroniclesNPCs.karengo) },
//    firstPageSelector = {
//        val pages = this
//
//        // Resume from where player left off.
//        if (Telos3HubMission.state.visitedPrimaryPlanet == true) {
//            if (Telos2HubMission.choices.injectedSelf == true)
//                pages.single { it.id == "4-ether-go-inside" }
//            else
//                pages.single { it.id == "14-noEther" }
//        } else if (Telos2HubMission.choices.injectedSelf == true)
//            pages.single { it.id == "1-ether-start" }
//        else {
//            pages.single { it.id == "1-noEther-start" }
//        }
//    },
    pages = PagesFromJson(
        pagesJson = stageJson.query("/pages"),
        onPageShownHandlersByPageId = mapOf(
            "3-explanation" to {
                val page = navigator.currentPage()!!
                if (game.memory["\$gaPZ_scannedZiggurat"] == true) {
                    para { page.extraData["response-zigg"] as String }
                } else {
                    para { page.extraData["response-noZigg"] as String }
                }
            },
            "4-cliffhanger" to {
                mission.setCurrentStage(Telos3HubMission.Stage.Completed, dialog, null)

            }
        ),
        optionConfigurator = { options ->
            options.map { option ->
                when (option.id) {
                    "endOfPhase2" -> option.copy(onOptionSelected = {
                        it.close(doNotOfferAgain = true)
                    })

                    else -> option
                }
            }
        }
    )
)