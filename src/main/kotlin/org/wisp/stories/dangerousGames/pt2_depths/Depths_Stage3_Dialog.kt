package org.wisp.stories.dangerousGames.pt2_depths

import org.wisp.stories.game
import wisp.questgiver.InteractionDefinition

class Depths_Stage3_Dialog : InteractionDefinition<Depths_Stage3_Dialog>(
    pages = listOf(
        Page(
            id = 1,
            onPageShown = {
                para {
                    game.text.getf(
                        "dg_de_stg3_backToStart_pg1_para1",
                        "depthsPlanet" to (DepthsQuest.startingPlanet?.fullName ?: "null")
                    )
                }
                para {
                    val gatesAwakenedRemainingCodesMemKey = "\$GatesAwakened_activation_codes_remaining"
                    val gatesAwakenedSecondQuestDoneMemKey = "\$GatesAwakened_mid_quest_done"

                    if (game.settings.modManager.isModEnabled("gates_awakened") &&
                        game.memory[gatesAwakenedSecondQuestDoneMemKey] == true &&
                        game.memory[gatesAwakenedRemainingCodesMemKey] as? Int != null
                    ) {
                        when (DepthsQuest.Stage2.riddleSuccessesCount) {
                            0, 1, 2 -> {
                                // Give one Gate Code
                                game.memory[gatesAwakenedRemainingCodesMemKey] =
                                    game.memory[gatesAwakenedRemainingCodesMemKey] as Int + 1
                                game.text["dg_de_stg3_backToStart_pg1_para2_ifFailedSomeRiddles"]
                            }
                            3 -> {
                                // Give two Gate Codes
                                game.memory[gatesAwakenedRemainingCodesMemKey] =
                                    game.memory[gatesAwakenedRemainingCodesMemKey] as Int + 2
                                game.text["dg_de_stg3_backToStart_pg1_para2_ifFailedNoRiddles"]
                            }
                            else -> error("Unexpected success count ${DepthsQuest.Stage2.riddleSuccessesCount}")
                        }
                    } else
                        game.text["dg_de_stg3_backToStart_pg1_para2_ifGatesAwakenedMidQuestNotDone"]
                }
                para { game.text["dg_de_stg3_backToStart_pg1_para3"] }
                para { game.text["dg_de_stg3_backToStart_pg1_para4"] }
                para { game.text["dg_de_stg3_backToStart_pg1_para5"] }
            },
            options = listOf(
                Option(text = { game.text["dg_de_stg3_backToStart_pg1_opt1"] },
                    onOptionSelected = {
                        DepthsQuest.finishStage2()
                        it.close(doNotOfferAgain = true)
                    }
                )
            )
        ))
) {
    override fun createInstanceOfSelf() = Depths_Stage3_Dialog()
}