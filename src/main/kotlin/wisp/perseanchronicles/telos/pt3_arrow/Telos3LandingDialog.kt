package wisp.perseanchronicles.telos.pt3_arrow

import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Drops
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.CargoPods
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.util.Misc
import org.json.JSONObject
import org.magiclib.kotlin.*
import wisp.perseanchronicles.Jukebox
import wisp.perseanchronicles.common.PerseanChroniclesNPCs
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.TelosCommon
import wisp.perseanchronicles.telos.pt2_dart.Telos2HubMission
import wisp.questgiver.v2.IInteractionLogic
import wisp.questgiver.v2.InteractionDialogLogic
import wisp.questgiver.v2.json.PagesFromJson
import wisp.questgiver.v2.json.getPageById
import wisp.questgiver.v2.json.query
import wisp.questgiver.wispLib.findFirst
import kotlin.math.roundToInt

class Telos3LandingDialog(
    stageJson: JSONObject = Telos3HubMission.part3Json.query("/stages/goToPlanet"),
    mission: Telos3HubMission = game.sector.intelManager.findFirst()!!
) : InteractionDialogLogic<Telos3LandingDialog>(
    onInteractionStarted = {

    },
    people = { listOfNotNull(PerseanChroniclesNPCs.karengo) },
    firstPageSelector = {
        val pages = this

        // Resume from where player left off.
        if (Telos3HubMission.state.visitedPrimaryPlanet == true) {
            if (Telos2HubMission.choices.injectedSelf == true)
                pages.single { it.id == "4-ether-go-inside" }
            else
                pages.single { it.id == "14-noEther" }
        } else if (Telos2HubMission.choices.injectedSelf == true)
            pages.single { it.id == "1-ether-start" }
        else {
            pages.single { it.id == "1-noEther-start" }
        }
    },
    pages = PagesFromJson(
        pagesJson = stageJson.query("/pages"),
        onPageShownHandlersByPageId = mapOf(
            "1-ether-start" to {
                game.jukebox.playTelosThemeMusic()
                Telos3HubMission.state.visitedPrimaryPlanet = true
            },
            "1-noEther-start" to {
                game.jukebox.playTelosThemeMusic()
                Telos3HubMission.state.visitedPrimaryPlanet = true
                // Injected with Ether
                game.sector.playerPerson.addTag(TelosCommon.ETHER_OFFICER_TAG)
                PerseanChroniclesNPCs.karengo.addTag(TelosCommon.ETHER_OFFICER_TAG)
            },
            "4-choices" to {
                dialog.visualPanel.showImagePortion(IInteractionLogic.Illustration("wisp_perseanchronicles_telos", "choices"))
            },
            "4-labs" to {
                Telos3HubMission.state.visitedLabs = true
            },
            "4-survivors" to {
                Telos3HubMission.state.searchedForSurvivors = true
            },
            "4-common-areas" to {
                Telos3HubMission.state.sawKryptaDaydream = true
            },
            "4-labs-2" to {
                if (Telos3HubMission.state.etherVialChoice == null)
                    para { getPageById(stageJson.query("/pages"), "4-labs-2")?.optString("vials") ?: "" }
            },
            "4-labs-destroy-ether" to {
                PerseanChroniclesNPCs.karengo.adjustReputationWithPlayer(repChange = -.05f, textPanel = dialog.textPanel)
            },
            "4-survivors-investigate-2" to {
                dialog.visualPanel.showImagePortion(IInteractionLogic.Illustration("wisp_perseanchronicles_telos", "sleepers"))
            },
            "4-storage" to {
                if (Telos3HubMission.state.retrievedSupplies != true) {
                    val random = Misc.getRandom(game.sector.memoryWithoutUpdate.getLong(MemFlags.SALVAGE_SEED), 100)
                    dialog.interactionTarget.addDropValue(Drops.SUPPLY, (game.sector.playerFleet.totalSupplyCostPerDay * 3000).roundToInt())
                    val supplies = SalvageEntity.generateSalvage(
                        random,
                        1f,
                        1f,
                        1f,
                        1f,
                        dialog.interactionTarget.dropValue,
                        dialog.interactionTarget.dropRandom
                    ).supplies.roundToInt()
                    dialog.interactionTarget.dropValue.clear()

                    if (game.sector.playerFleet.cargo.spaceLeft >= supplies) {
                        game.sector.playerFleet.cargo.addSupplies(supplies.toFloat())
                    } else {
                        game.sector.playerFleet.cargo.addSupplies(game.sector.playerFleet.cargo.spaceLeft)
                        Misc.addCargoPods(game.sector.playerFleet.containingLocation, game.sector.playerFleet.location)
                            .also { pods ->
                                pods.cargo.addSupplies((supplies - game.sector.playerFleet.cargo.spaceLeft))
                                CargoPods.stabilizeOrbit(pods, true)
                            }
                    }
                    dialog.textPanel.addCommodityGainText(commodityId = Commodities.SUPPLIES, quantity = supplies)

                    Telos3HubMission.state.retrievedSupplies = true
                }
            },
            "7-shipyard-itesh" to {
                dialog.visualPanel.showFleetMemberInfo(game.settings.getVariant("wisp_perseanchronicles_itesh_Standard")
                    .let { game.factory.createFleetMember(FleetMemberType.SHIP, it) })
            },
            "10-disconnected" to {
                // Give Itesh
                val itesh = game.settings.getVariant("wisp_perseanchronicles_itesh_Standard")
                    .let { game.factory.createFleetMember(FleetMemberType.SHIP, it) }
                game.sector.playerFleet.fleetData.addFleetMember(itesh)
                itesh.apply {
                    prepareShipForRecovery(
                        retainAllHullmods = true,
                        retainKnownHullmods = true,
                        clearSMods = false,
                        weaponRetainProb = 1f,
                        wingRetainProb = 1f
                    )
                    shipName = Telos3HubMission.part3Json.query("/strings/iteshName")
                    repairTracker.cr = .7f
                }
                dialog.textPanel.addFleetMemberGainText(itesh)
            },
            "13-getting-to-bridge" to {
                // Vanilla illustration, not in Persean Chronicles.
                dialog.visualPanel.showImagePortion(IInteractionLogic.Illustration("illustrations", "free_orbit"))
            },
            "14-question-bridge" to {
                if (Telos3HubMission.state.viewedWho == true
                    && Telos3HubMission.state.viewedWhat == true
                    && Telos3HubMission.state.viewedWhen == true
                    && Telos3HubMission.state.viewedWhere == true
                ) {
                    navigator.goToPage("15-powerup-choice")
                }
            },
            "16-powerup-bar" to {
                val page = navigator.currentPage()!!

                if (game.settings.modManager.isModEnabled("alcoholism")) {
                    if (game.settings.modManager.isModEnabled("PAGSM")) {
                        para { page.extraData["order-alcoholism-pagsm"] as String }
                    } else {
                        para { page.extraData["order-alcoholism"] as String }
                    }
                } else {
                    para { page.extraData["order-vanilla"] as String }
                }
            },
            "16-powerup-main-1" to {
                val page = navigator.currentPage()!!

                if (game.sector.playerFleet.fleetData.membersListCopy.any { it.hullId == "wisp_perseanchronicles_vara" })
                    para { page.extraData["withVara"] as String }
                else
                    para { page.extraData["withoutVara"] as String }
            },
            "16-powerup-main-4" to {
                if (TelosCommon.ETHER_SIGHT_ID !in game.sector.characterData.abilities) {
                    game.sector.characterData.addAbility(TelosCommon.ETHER_SIGHT_ID)
                    game.sector.playerFleet.addAbility(TelosCommon.ETHER_SIGHT_ID)
                    dialog.textPanel.addAbilityGainText(TelosCommon.ETHER_SIGHT_ID)
                }
            },
            "16-powerup-main-7" to {
                mission.setCurrentStage(Telos3HubMission.Stage.EscapeSystemForDisplay, dialog, null)
            },
        ),
        optionConfigurator = { options ->
            options.map { option ->
                when (option.id) {
                    // Let player collect supplies only once.
                    "search-storage" -> option.copy(
                        showIf = { Telos3HubMission.state.retrievedSupplies != true })

                    "take-ether" -> option.copy(
                        showIf = { Telos3HubMission.state.etherVialChoice == null },
                        onOptionSelected = {
                            Telos3HubMission.state.etherVialChoice = Telos3HubMission.EtherVialsChoice.Took
                        })

                    "destroy-ether" -> option.copy(
                        showIf = { Telos3HubMission.state.etherVialChoice == null },
                        onOptionSelected = { Telos3HubMission.state.etherVialChoice = Telos3HubMission.EtherVialsChoice.Destroyed })

                    "return-to-orbit" -> option.copy(
                        onOptionSelected = { this.navigator.close(doNotOfferAgain = false) }
                    )

                    "debrief-who" -> option.copy(
                        showIf = { Telos3HubMission.state.viewedWho != true },
                        onOptionSelected = { Telos3HubMission.state.viewedWho = true },
                    )

                    "debrief-what" -> option.copy(
                        showIf = { Telos3HubMission.state.viewedWhat != true },
                        onOptionSelected = { Telos3HubMission.state.viewedWhat = true },
                    )

                    "debrief-when" -> option.copy(
                        showIf = { Telos3HubMission.state.viewedWhen != true },
                        onOptionSelected = { Telos3HubMission.state.viewedWhen = true },
                    )

                    "debrief-where" -> option.copy(
                        showIf = { Telos3HubMission.state.viewedWhere != true },
                        onOptionSelected = { Telos3HubMission.state.viewedWhere = true },
                    )

                    "karengo-cabin" -> option.copy { PerseanChroniclesNPCs.karengo.adjustReputationWithPlayer(.1f, dialog.textPanel) }

                    "cheers-karengo" -> option.copy { PerseanChroniclesNPCs.karengo.adjustReputationWithPlayer(.05f, dialog.textPanel) }

                    "worry-agree" -> option.copy { PerseanChroniclesNPCs.karengo.adjustReputationWithPlayer(.05f, dialog.textPanel) }

                    "flee" -> {
                        option.copy(onOptionSelected = {
                            this.navigator.close(doNotOfferAgain = true)
                            mission.setCurrentStage(Telos3HubMission.Stage.EscapeSystem, dialog, null)
                        })
                    }

                    else -> option
                }
            }
        }
    )
)