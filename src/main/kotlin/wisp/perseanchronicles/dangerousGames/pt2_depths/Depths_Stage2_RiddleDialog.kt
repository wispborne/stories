package wisp.perseanchronicles.dangerousGames.pt2_depths

import wisp.perseanchronicles.game
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.InteractionDialogLogic
import wisp.questgiver.wispLib.empty
import wisp.questgiver.wispLib.findFirst

class Depths_Stage2_RiddleDialog(val mission: DepthsHubMission = game.intelManager.findFirst()!!) :
    InteractionDialogLogic<Depths_Stage2_RiddleDialog>(
        pages = listOf(
            IInteractionLogic.Page(
                id = 1,
                image = DepthsHubMission.intelIllustration,
                onPageShown = {
                    if (mission.isPlanetColonized()) {
                        para {
                            game.text.getf(
                                "dg_de_stg2_pg1_para1",
                                "ifPlayerOwnedWorld" to
                                        if (DepthsHubMission.state.depthsPlanet?.market?.isPlayerOwned == true)
                                            game.text["dg_de_stg2_pg1_para1_ifPlayerOwnedWorld"]
                                        else
                                            String.empty
                            )
                        }
                    }

                    para { game.text["dg_de_stg2_pg1_para2"] }
                    navigator.promptToContinue(game.text["continue"]) {
                        para { game.text["dg_de_stg2_pg1_para3"] }
                    }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_pg1_opt1"] },
                        onOptionSelected = { it.goToPage(2) }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_pg1_opt2"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_pg1_opt2_onSelected"] }
                            it.goToPage(2)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = 2,
                onPageShown = {
                    para { game.text["dg_de_stg2_pg2_para1"] }
                    navigator.promptToContinue(game.text["dg_de_stg2_pg2_para1_continue"]) {
                        dialog.visualPanel.showImagePortion(
                            DepthsHubMission.subIllustration.category,
                            DepthsHubMission.subIllustration.id,
                            DepthsHubMission.subIllustration.width,
                            DepthsHubMission.subIllustration.height,
                            DepthsHubMission.subIllustration.xOffset,
                            DepthsHubMission.subIllustration.yOffset,
                            DepthsHubMission.subIllustration.displayWidth,
                            DepthsHubMission.subIllustration.displayHeight
                        )
                        para { game.text["dg_de_stg2_pg2_para2"] }
                        mission.startMusic()
                    }
                },
                options = listOf(IInteractionLogic.Option(
                    text = { game.text["dg_de_stg2_pg2_opt1"] },
                    onOptionSelected = {
                        it.goToPage(3)
                    }
                ))
            ),
            IInteractionLogic.Page(
                id = 3,
                onPageShown = {
                    para { game.text["dg_de_stg2_pg3_para1"] }

                    navigator.promptToContinue(game.text["continue"]) {
                        para { game.text["dg_de_stg2_pg3_para1_2"] }

                        navigator.promptToContinue(game.text["continue"]) {
                            para { game.text["dg_de_stg2_pg3_para1_3"] }
                            para { game.text["dg_de_stg2_pg3_para2"] }
                        }
                    }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_pg3_opt1"] },
                        onOptionSelected = { it.goToPage(4) }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = 4,
                onPageShown = {
                    para { game.text["dg_de_stg2_pg4_para1"] }
                    para { game.text["dg_de_stg2_pg4_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_pg4_opt1"] },
                        onOptionSelected = { it.goToPage(PageId.ViewRiddle1) }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.ViewRiddle1,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_optEast"] },
                        onOptionSelected = {
                            mission.choices.riddle1Choice = RiddleChoice.Riddle1Choice.EastMorg
                            it.goToPage(PageId.Riddle1_East_Morg)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_optNorth"] },
                        onOptionSelected = {
                            mission.choices.riddle1Choice = RiddleChoice.Riddle1Choice.NorthSuccess
                            it.goToPage(PageId.Riddle1_North_Success)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_optSouth"] },
                        onOptionSelected = {
                            mission.choices.riddle1Choice = RiddleChoice.Riddle1Choice.SouthSmoke
                            it.goToPage(PageId.Riddle1_South_Smoke)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_optWest"] },
                        onOptionSelected = {
                            mission.choices.riddle1Choice = RiddleChoice.Riddle1Choice.WestWall
                            it.goToPage(PageId.Riddle1_West_Wall)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle1_East_Morg,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_east_para1"] }
                    para { game.text["dg_de_stg2_riddle1_east_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_east_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle1_east_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle1_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle1_South_Smoke,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_south_para1"] }
                    para { game.text["dg_de_stg2_riddle1_south_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_south_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle1_south_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle1_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle1_West_Wall,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_west_para1"] }
                    para { game.text["dg_de_stg2_riddle1_west_para2"] }
                    para { game.text["dg_de_stg2_riddle1_west_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_west_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle1_west_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle1_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle1_North_Success,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_north_para1"] }
                    para { game.text["dg_de_stg2_riddle1_north_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_north_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewRiddle2)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle1_Failed,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle1_failure_para1"] }
                    para { game.text["dg_de_stg2_riddle1_failure_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle1_failure_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewRiddle2)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.ViewRiddle2,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_optEast"] },
                        onOptionSelected = {
                            mission.choices.riddle2Choice = RiddleChoice.Riddle2Choice.EastSuccess
                            it.goToPage(PageId.Riddle2_East_Success_SecondSuccess)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_optNorth"] },
                        onOptionSelected = {
                            mission.choices.riddle2Choice = RiddleChoice.Riddle2Choice.NorthVines
                            it.goToPage(PageId.Riddle2_North_Vines)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_optWest"] },
                        onOptionSelected = {
                            mission.choices.riddle2Choice = RiddleChoice.Riddle2Choice.WestWall
                            if (mission.choices.riddle1Choice != RiddleChoice.Riddle1Choice.WestWall)
                                it.goToPage(PageId.Riddle2_West_FirstWallCrash)
                            else
                                it.goToPage(PageId.Riddle2_West_SecondWallCrash)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_North_Vines,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_north_para1"] }
                    para { game.text["dg_de_stg2_riddle2_north_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_north_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle2_north_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle2_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_West_FirstWallCrash,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_west_1stWallCrash_para1"] }
                    para { game.text["dg_de_stg2_riddle2_west_1stWallCrash_para2"] }
                    para { game.text["dg_de_stg2_riddle2_west_1stWallCrash_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_west_1stWallCrash_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle2_west_1stWallCrash_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle2_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_West_SecondWallCrash,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_west_2ndWallCrash_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_west_2ndWallCrash_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle2_west_2ndWallCrash_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle2_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_Failed,
                onPageShown = {
                    if (mission.choices.riddle1Choice == RiddleChoice.Riddle1Choice.NorthSuccess) {
                        para { game.text["dg_de_stg2_riddle2_failure_firstFail_para1"] }
                        para { game.text["dg_de_stg2_riddle2_failure_firstFail_para2"] }
                    } else {
                        para { game.text["dg_de_stg2_riddle2_failure_secondFail_para1"] }
                        para { game.text["dg_de_stg2_riddle2_failure_secondFail_para2"] }
                    }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_failure_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewRiddle3)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_East_Success_FirstSuccess,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_east_firstSuccess_para1"] }
                    para { game.text["dg_de_stg2_riddle2_east_firstSuccess_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_east_firstSuccess_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewRiddle3)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle2_East_Success_SecondSuccess,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle2_east_secondSuccess_para1"] }
                    para { game.text["dg_de_stg2_riddle2_east_secondSuccess_para2"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle2_east_secondSuccess_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewRiddle3)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.ViewRiddle3,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle3_para1"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_optNorth"] },
                        onOptionSelected = {
                            mission.choices.riddle3Choice = RiddleChoice.Riddle3Choice.NorthKoijuu
                            it.goToPage(PageId.Riddle3_North_Koijuu)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_optEast"] },
                        onOptionSelected = {
                            mission.choices.riddle3Choice = RiddleChoice.Riddle3Choice.EastWall
                            it.goToPage(PageId.Riddle3_East_Wall)
                        }
                    ),
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_optSouth"] },
                        onOptionSelected = {
                            mission.choices.riddle3Choice = RiddleChoice.Riddle3Choice.SouthSuccess
                            it.goToPage(PageId.Riddle3_South_Success)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle3_North_Koijuu,
                onPageShown = {
                    para {
                        when (mission.riddleSuccessesCount) {
                            2 -> game.text["dg_de_stg2_riddle3_north_quoijuu_para1_firstFail"]
                            1 -> game.text["dg_de_stg2_riddle3_north_quoijuu_para1_secondFail"]
                            0 -> game.text["dg_de_stg2_riddle3_north_quoijuu_para1_thirdFail"]
                            else -> error("Unexpected success count ${mission.riddleSuccessesCount}")
                        }
                    }
                    para { game.text["dg_de_stg2_riddle3_north_quoijuu_para2"] }

                    navigator.promptToContinue(game.text["continue"]) {
                        para { game.text["dg_de_stg2_riddle3_north_quoijuu_para3"] }
                        para { game.text["dg_de_stg2_riddle3_north_quoijuu_para4"] }

                        navigator.promptToContinue(game.text["continue"]) {
                            para { game.text["dg_de_stg2_riddle3_north_quoijuu_para5"] }
                            para { game.text["dg_de_stg2_riddle3_north_quoijuu_para6"] }
                        }
                    }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_north_quoijuu_opt1"] },
                        onOptionSelected = { it.goToPage(PageId.Riddle3_Failed) }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle3_East_Wall,
                onPageShown = {
                    para {
                        when (mission.wallCrashesCount) {
                            1 -> {
                                game.text["dg_de_stg2_riddle3_east_wall_firstWallFail_para1"]
                                game.text["dg_de_stg2_riddle3_east_wall_firstWallFail_para2"]
                                game.text["dg_de_stg2_riddle3_east_wall_firstWallFail_para3"]
                            }

                            2 -> game.text["dg_de_stg2_riddle3_east_wall_secondWallFail_para1"]
                            3 -> game.text["dg_de_stg2_riddle3_east_wall_thirdWallFail_para1"]
                            else -> error("Unexpected wall crash count ${mission.wallCrashesCount}")
                        }
                    }

                },
                options = listOf(
                    IInteractionLogic.Option(
                        showIf = { mission.wallCrashesCount == 1 }, // Only failed this wall
                        text = { game.text["dg_de_stg2_riddle3_east_wall_firstWallFail_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle3_east_wall_firstWallFail_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle3_Failed)
                        }
                    ),
                    IInteractionLogic.Option(
                        showIf = { mission.wallCrashesCount == 2 }, // Only failed this wall and one other
                        text = { game.text["dg_de_stg2_riddle3_east_wall_secondWallFail_opt1"] },
                        onOptionSelected = {
                            para { game.text["dg_de_stg2_riddle3_east_wall_secondWallFail_opt1_onSelected"] }
                            it.goToPage(PageId.Riddle3_Failed)
                        }
                    ),
                    IInteractionLogic.Option(
                        showIf = { mission.wallCrashesCount == 3 }, // Failed all three.
                        text = { game.text["dg_de_stg2_riddle3_east_wall_thirdWallFail_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.Riddle3_Failed)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle3_Failed,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle3_failure_para1"] }
                    para { game.text["dg_de_stg2_riddle3_failure_para2"] }
                    para { game.text["dg_de_stg2_riddle3_failure_para3"] }
                    para { game.text["dg_de_stg2_riddle3_failure_para4"] }

                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_failure_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewTreasure)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.Riddle3_South_Success,
                onPageShown = {
                    para { game.text["dg_de_stg2_riddle3_south_success_para1"] }
                    para { game.text["dg_de_stg2_riddle3_south_success_para2"] }
                    para { game.text["dg_de_stg2_riddle3_south_success_para3"] }
                    para { game.text["dg_de_stg2_riddle3_south_success_para4"] }

                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_riddle3_south_success_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.ViewTreasure)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.ViewTreasure,
                onPageShown = {
                    para { game.text["dg_de_stg2_viewTreasure_para1"] }
                    para { game.text["dg_de_stg2_viewTreasure_para2"] }

                    navigator.promptToContinue(continueText = game.text["continue"]) {
                        para {
                            if (mission.riddleSuccessesCount > 0)
                                game.text["dg_de_stg2_viewTreasure_para3_ifOthersSurvived"]
                            else
                                game.text["dg_de_stg2_viewTreasure_para3_ifOnlyPlayerSurvived"]
                        }
                        para { game.text["dg_de_stg2_viewTreasure_para4"] }
                        para {
                            if (mission.riddleSuccessesCount > 0)
                                game.text["dg_de_stg2_viewTreasure_para5_ifOthersSurvived"]
                            else
                                game.text["dg_de_stg2_viewTreasure_para5_ifOnlyPlayerSurvived"]
                        }
                        para {
                            when {
                                mission.riddleSuccessesCount == 3 -> game.text["dg_de_stg2_viewTreasure_para6_ifAllOthersSurvived"]
                                mission.riddleSuccessesCount > 0 -> game.text["dg_de_stg2_viewTreasure_para6_ifSomeOthersSurvived"]
                                mission.riddleSuccessesCount == 0 -> game.text["dg_de_stg2_viewTreasure_para6_ifOnlyPlayerSurvived"]
                                else -> error("Unexpected riddle success count ${mission.riddleSuccessesCount}")
                            }
                        }
                    }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_viewTreasure_opt1"] },
                        onOptionSelected = {
                            it.goToPage(PageId.BackAtTheSurface)
                        }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.BackAtTheSurface,
                image = DepthsHubMission.intelIllustration,
                onPageShown = {
                    para { game.text["dg_de_stg2_backAtSurface_para1"] }
                    mission.stopMusic()
                    para {
                        if (mission.riddleSuccessesCount > 0)
                            game.text["dg_de_stg2_backAtSurface_para2_ifOthersSurvived"]
                        else
                            game.text["dg_de_stg2_backAtSurface_para2_ifOnlyPlayerSurvived"]
                    }
                    para { game.text["dg_de_stg2_backAtSurface_para3"] }
                },
                options = listOf(
                    IInteractionLogic.Option(
                        text = { game.text["dg_de_stg2_backAtSurface_opt1"] },
                        onOptionSelected = { it.goToPage(PageId.BackInSpace) }
                    )
                )
            ),
            IInteractionLogic.Page(
                id = PageId.BackInSpace,
                onPageShown = {
                    para { game.text["dg_de_stg2_inSpace_para1"] }
                    mission.setCurrentStage(DepthsHubMission.Stage.ReturnToStart, dialog, null)
                },
                options = listOf(
                    IInteractionLogic.Option(
                        // Leave
                        text = { game.text["dg_de_stg2_inSpace_opt1"] },
                        onOptionSelected = {

                            dialog.visualPanel.showLoot(
                                game.text["dg_de_stg2_inSpace_salvaged_title"],
                                mission.generateRewardLoot(dialog.interactionTarget),
                                false,
                                true,
                                true
                            ) {
                                it.close(doNotOfferAgain = true)
                            }

                            dialog.optionPanel.clearOptions()
                            dialog.promptText = ""
                        }
                    )
                )
            )
        )
    ) {

    enum class PageId {
        ViewRiddle1,
        Riddle1_East_Morg,
        Riddle1_North_Success,
        Riddle1_South_Smoke,
        Riddle1_West_Wall,
        Riddle1_Failed,
        ViewRiddle2,
        Riddle2_East_Success_FirstSuccess,
        Riddle2_East_Success_SecondSuccess,
        Riddle2_North_Vines,
        Riddle2_West_FirstWallCrash,
        Riddle2_West_SecondWallCrash,
        Riddle2_Failed,
        ViewRiddle3,
        Riddle3_North_Koijuu,
        Riddle3_East_Wall,
        Riddle3_South_Success,
        Riddle3_Failed,
        ViewTreasure,
        BackAtTheSurface,
        BackInSpace
    }

    sealed class RiddleChoice {
        abstract fun wasSuccessful(): Boolean

        /**
         * If failure, Elias and Taddese die.
         */
        sealed class Riddle1Choice : RiddleChoice() {
            object EastMorg : Riddle1Choice() {
                override fun wasSuccessful() = false
            }

            object NorthSuccess : Riddle1Choice() {
                override fun wasSuccessful() = true
            }

            object SouthSmoke : Riddle1Choice() {
                override fun wasSuccessful() = false
            }

            object WestWall : Riddle1Choice() {
                override fun wasSuccessful() = false
            }
        }

        /**
         * If failure, Mussie dies.
         */
        sealed class Riddle2Choice : RiddleChoice() {
            object EastSuccess : Riddle2Choice() {
                override fun wasSuccessful() = true
            }

            object NorthVines : Riddle2Choice() {
                override fun wasSuccessful() = false
            }

            object WestWall : Riddle2Choice() {
                override fun wasSuccessful() = false
            }
        }

        /**
         * If failure, Jorma and Daciana die.
         */
        sealed class Riddle3Choice : RiddleChoice() {
            object NorthKoijuu : Riddle3Choice() {
                override fun wasSuccessful() = false
            }

            object EastWall : Riddle3Choice() {
                override fun wasSuccessful() = false
            }

            object SouthSuccess : Riddle3Choice() {
                override fun wasSuccessful() = true
            }
        }
    }
}