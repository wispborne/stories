package wisp.perseanchronicles.telos.boats

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.listeners.HullDamageAboutToBeTakenListener
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.lwjgl.util.vector.Vector2f
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.TelosCommon
import wisp.questgiver.wispLib.IntervalUtil
import wisp.questgiver.wispLib.addPara
import kotlin.math.absoluteValue

class TelosEtherNetworkedHullmod : BaseHullMod() {
    companion object {
        const val MANUV_PERCENT = -50
        const val SPEED_PERCENT = -50
        const val PPT_PERCENT = -20
        const val HULLSHIELD_FLUX_PERCENT = 80f

        fun hasEtherOfficer(captain: PersonAPI?): Boolean {
            return captain?.hasTag(TelosCommon.ETHER_OFFICER_TAG) ?: false
        }
    }

    val timer = IntervalUtil(1f)

    override fun advanceInCampaign(member: FleetMemberAPI?, amount: Float) {
        super.advanceInCampaign(member, amount)
        timer.advance(amount)
        if (!timer.intervalElapsed()) return

        setEnabledOrDisabled(member?.captain)
    }

    private fun setEnabledOrDisabled(captain: PersonAPI?) {
        if (hasEtherOfficer(captain)) {
            spec.spriteName = game.settings.getSpriteName("wisp_perseanchronicles_telos", "telos_ethernet")
        } else {
            spec.spriteName = game.settings.getSpriteName("wisp_perseanchronicles_telos", "telos_ethernet_disabled")
        }
    }

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize?, stats: MutableShipStatsAPI?, id: String?) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id)
        stats ?: return

        val isDebuffed = !hasEtherOfficer(stats.fleetMember?.captain)

        if (isDebuffed) {
            // Manuverability
            stats.acceleration.modifyPercent(id, MANUV_PERCENT.toFloat())
            stats.deceleration.modifyPercent(id, MANUV_PERCENT.toFloat())
            stats.turnAcceleration.modifyPercent(id, MANUV_PERCENT.toFloat())
            stats.maxTurnRate.modifyPercent(id, MANUV_PERCENT.toFloat())
            // Max Speed
            stats.maxSpeed.modifyPercent(id, SPEED_PERCENT.toFloat())
            // PPT
            stats.peakCRDuration.modifyPercent(id, PPT_PERCENT.toFloat())
        }
    }

    override fun applyEffectsAfterShipCreation(ship: ShipAPI, id: String?) {
        ship.addListener(TelosEtherNetworkedHullmodScript(ship))
        setEnabledOrDisabled(ship.captain)
    }

    override fun advanceInCombat(ship: ShipAPI?, amount: Float) {
        super.advanceInCombat(ship, amount)

        val engine = Global.getCombatEngine() ?: return
        setEnabledOrDisabled(ship?.captain)
    }

    override fun addPostDescriptionSection(tooltip: TooltipMakerAPI?, hullSize: HullSize?, ship: ShipAPI?, width: Float, isForModSpec: Boolean) {
        super.addPostDescriptionSection(tooltip, hullSize, ship, width, isForModSpec)
        tooltip ?: return

        val hasEtherOfficer = hasEtherOfficer(ship?.captain)

        tooltip.addPara(textColor = if (hasEtherOfficer) Misc.getTextColor() else Misc.getNegativeHighlightColor()) {
            "This ship requires an Ether-networked officer for full performance."
        }

        val shieldTextColor = if (hasEtherOfficer) Misc.getTextColor() else Misc.getGrayColor()
        tooltip.addSectionHeading(
            /* str = */ "Hullshield",
            /* textColor = */
            if (hasEtherOfficer) game.sector.getFaction(TelosCommon.FACTION_TELOS_ID).color else Misc.getGrayColor(),
            /* bgColor = */
            if (hasEtherOfficer) Misc.setAlpha(Misc.scaleColorOnly(game.sector.getFaction(TelosCommon.FACTION_TELOS_ID).brightUIColor, 0.4f), 175)
            else Misc.setAlpha(
                Misc.scaleColorOnly(
                    Misc.getGrayColor(),
                    0.4f
                ), 175
            ),
            /* align = */
            Alignment.MID,
            /* pad = */
            10f
        )
        tooltip.addPara(textColor = shieldTextColor, highlightColor = if (hasEtherOfficer) Misc.getHighlightColor() else shieldTextColor) {
            "If flux is below ==${HULLSHIELD_FLUX_PERCENT.toInt()}%==, hull damage will be absorbed at a high flux cost.\n"
        }

        val penaltyTextColor = if (!hasEtherOfficer) Misc.getTextColor() else Misc.getGrayColor()
        tooltip.addSectionHeading(
            /* str = */ "Penalty",
            /* textColor = */
            if (!hasEtherOfficer) Misc.getNegativeHighlightColor() else Misc.getGrayColor(),
            /* bgColor = */
            if (!hasEtherOfficer) Misc.setAlpha(Misc.scaleColorOnly(Misc.getNegativeHighlightColor(), 0.4f), 175) else Misc.setAlpha(
                Misc.scaleColorOnly(
                    Misc.getGrayColor(),
                    0.4f
                ), 175
            ),
            /* align = */
            Alignment.MID,
            /* pad = */
            3f
        )
        tooltip.addPara(textColor = penaltyTextColor, highlightColor = if (!hasEtherOfficer) Misc.getNegativeHighlightColor() else penaltyTextColor) {
            "Reduces maneuverability by ==${MANUV_PERCENT.absoluteValue}%==, top speed by ==${SPEED_PERCENT.absoluteValue}%==, and PPT by ==${PPT_PERCENT.absoluteValue}%==.\n"
        }
    }

    override fun getUnapplicableReason(ship: ShipAPI?) = "Requires a Telos ship."

    class TelosEtherNetworkedHullmodScript(val networkedShip: ShipAPI) : HullDamageAboutToBeTakenListener {

        override fun notifyAboutToTakeHullDamage(param: Any?, ship: ShipAPI?, point: Vector2f?, damageAmount: Float): Boolean {
            ship ?: return false
            if (!hasEtherOfficer(ship.captain)) return false
            if (!ship.isAlive) return false
            if (!ship.variant.hasHullMod(TelosCommon.ETHERNETWORKED_HULLMOD_ID)) return false // need Ether-networked hullmod
            if (ship.fluxLevel > (HULLSHIELD_FLUX_PERCENT / 100f)) return false // don't activate if flux is high

            ship.fluxTracker.increaseFlux(damageAmount * 3f, true)


            return true
        }
    }
}