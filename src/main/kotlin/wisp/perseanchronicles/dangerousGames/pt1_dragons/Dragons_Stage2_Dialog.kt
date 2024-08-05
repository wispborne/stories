package wisp.perseanchronicles.dangerousGames.pt1_dragons

import com.fs.starfarer.api.Global
import org.magiclib.achievements.MagicAchievementManager
import wisp.perseanchronicles.achievements.Achievements
import wisp.perseanchronicles.game
import wisp.questgiver.isPopulated
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.InteractionDialogLogic
import wisp.questgiver.wispLib.empty
import wisp.questgiver.wispLib.findFirst

class Dragons_Stage2_Dialog(val dragons: DragonsHubMission = Global.getSector().intelManager.findFirst()!!) :
    InteractionDialogLogic<Dragons_Stage2_Dialog>(
        pages = listOf(
            IInteractionLogic.Page(
                id = 1,
                image = DragonsHubMission.dragonPlanetImage,
                onPageShown = {
                    // The men load into a shuttle and Karengo directs you down through the atmosphere
                    para {
                        game.text.getf(
                            "dg_dr_stg2_pg1_para1",
                            "ifColonized" to
                                    if (isPlanetColonized())
                                        game.text["dg_dr_stg2_pg1_para1_ifColonized"]
                                    else
                                        String.empty
                        )
                    }
                    para { game.text["dg_dr_stg2_pg1_para2"] }
                    para { game.text["dg_dr_stg2_pg1_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(text = { game.text["dg_dr_stg2_pg1_opt1"] }, onOptionSelected = { it.goToPage(2) })
                )
            ),
            IInteractionLogic.Page(
                id = 2,
                onPageShown = {
                    // The dragons of ${dragonPlanet} you learned about on the "Xenofauna and Flora"
                    para { game.text["dg_dr_stg2_pg2_para1"] }
                    para { game.text["dg_dr_stg2_pg2_para2"] }
                    para { game.text["dg_dr_stg2_pg2_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg2_opt1"] },
                        onOptionSelected = { it.goToPage(Pages.TellEveryoneToGetOnBoard) }),
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg2_opt2"] },
                        onOptionSelected = { it.goToPage(Pages.StayAfterThingHitsShip) }),
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg2_opt3"] },
                        onOptionSelected = { it.goToPage(Pages.AbandonEveryone) })
                )
            ),
            IInteractionLogic.Page(
                id = Pages.StayAfterThingHitsShip,
                onPageShown = {
                    para { game.text["dg_dr_stg2_pg3_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg3_opt1"] },
                        onOptionSelected = { it.goToPage(Pages.TellEveryoneToGetOnBoard) }),
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg3_opt2"] },
                        onOptionSelected = { it.goToPage(Pages.AbandonEveryone) })
                )
            ),
            IInteractionLogic.Page(
                id = Pages.AbandonEveryone,
                onPageShown = {
                    para { game.text["dg_dr_stg2_pg-abandon_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg-abandon_opt1"] },
                        onOptionSelected = {
                            dragons.setCurrentStage(DragonsHubMission.Stage.FailedByAbandoningDragonriders, dialog, null)
                            it.close(doNotOfferAgain = true)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = Pages.TellEveryoneToGetOnBoard,
                onPageShown = {
                    para { game.text["dg_dr_stg2_pg4_para1"] }
                    para { game.text["dg_dr_stg2_pg4_para2"] }
                    para { game.text["dg_dr_stg2_pg4_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg4_opt1"] },
                        onOptionSelected = {
                            it.goToPage(Pages.TakeOff)
                        }),
                    IInteractionLogic.Option(
                        text = { game.text["dg_dr_stg2_pg4_opt2"] },
                        onOptionSelected = {
                            para { game.text["dg_dr_stg2_pg4_opt2_para1"] }
                            MagicAchievementManager.getInstance().completeAchievement(Achievements.PignutsAchievement::class.java)
                            it.goToPage(Pages.TakeOff)
                        })
                )
            ),
            IInteractionLogic.Page(
                id = Pages.TakeOff,
                onPageShown = {
                    para {
                        game.text.getf(
                            "dg_dr_stg2_pg5_para1",
                            "ifColonized" to
                                    if (isPlanetColonized())
                                        game.text["dg_dr_stg2_pg5_para1_ifColonized"]
                                    else
                                        String.empty
                        )
                    }
                    dragons.setCurrentStage(DragonsHubMission.Stage.ReturnToStart, dialog, null)
                },
                options = listOf(
                    IInteractionLogic.Option(
                        // Take the men back home
                        text = { game.text["dg_dr_stg2_pg5_opt1"] },
                        onOptionSelected = {
                            it.close(doNotOfferAgain = true)
                        }
                    )
                )
            )
        )
    ) {
    private fun planetName() = DragonsHubMission.state.dragonPlanet?.name

    enum class Pages {
        TellEveryoneToGetOnBoard,
        StayAfterThingHitsShip,
        AbandonEveryone,
        TakeOff
    }

    private fun isPlanetColonized() = DragonsHubMission.state.dragonPlanet?.market.isPopulated()
}