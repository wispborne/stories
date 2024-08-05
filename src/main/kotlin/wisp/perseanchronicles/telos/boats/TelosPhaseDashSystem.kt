package wisp.perseanchronicles.telos.boats

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.PhaseCloakSystemAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.impl.combat.PhaseCloakStats
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import wisp.perseanchronicles.game

class TelosPhaseDashSystem : BaseShipSystemScript() {
    companion object {
        /**
         * Elapsed time when the system was activated. Key is the ship id.
         */
        val dashStartTracker = mutableMapOf<String, Float>()
    }

    override fun apply(
        stats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float
    ) {
        if (Global.getCombatEngine().isPaused) return
        val combatEngine = game.combatEngine ?: return

        val ship = stats.entity as ShipAPI
        // Get the palette from the ship's deco weapons so this color coordinates.
        val palette =
            ship.allWeapons
                .orEmpty()
                .mapNotNull { it.effectPlugin }
                .filterIsInstance<TelosEngineEffects>()
                .firstOrNull()
                ?.currentPalette ?: defaultShipPalette

        // Make Avalok modules lunge too.
        ship.childModulesCopy.orEmpty()
            .filter { it.hullSpec.hullId == "wisp_perseanchronicles_avalok_module" }
            .forEach { module ->
                module.useSystem()
            }

        if (dashStartTracker[ship.id] == null) {
            dashStartTracker[ship.id] = combatEngine.getTotalElapsedTime(false)
        }

        val timeElapsed = combatEngine.getTotalElapsedTime(false) - dashStartTracker[ship.id]!!

        if (state == ShipSystemStatsScript.State.IN) {
            // Set deco weapon color (the fog) to the palette's initial phase color.
            ship.allWeapons
                .orEmpty()
                .mapNotNull { it.effectPlugin }
                .filterIsInstance<TelosEngineEffects>()
                .forEach { it.baseNebulaColorOverride = palette.phaseInitial }

            TelosPhaseDashModifier.apply(stats, id, state, effectLevel, timeElapsed)
        }

        if (state == ShipSystemStatsScript.State.ACTIVE) {
            stats.maxSpeed.unmodifyFlat(id)

            // Set phase engine color to phase color
            ship.allWeapons
                .orEmpty()
                .mapNotNull { it.effectPlugin }
                .filterIsInstance<TelosEngineEffects>()
                .forEach { it.baseNebulaColorOverride = palette.phaseMain }

            TelosPhaseDashModifier.apply(stats, id, state, effectLevel, timeElapsed)
        }

        if (state == ShipSystemStatsScript.State.OUT) {
            TelosPhaseDashModifier.apply(stats, id, state, effectLevel, timeElapsed)

            // Unset phase engine color
            ship.allWeapons
                .orEmpty()
                .mapNotNull { it.effectPlugin }
                .filterIsInstance<TelosEngineEffects>()
                .forEach { it.baseNebulaColorOverride = null }
        }
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship = stats.entity as ShipAPI
        dashStartTracker.remove(ship.id)
        TelosPhaseDashModifier.unapply(stats, id)
    }
}

internal object TelosPhaseDashModifier {
    val initialSpeedBoost = 200f

    fun apply(
        stats: MutableShipStatsAPI,
        id: String,
        state: ShipSystemStatsScript.State,
        effectLevel: Float,
        timeElapsed: Float
    ) {
        val ship = stats.entity as? ShipAPI ?: return
        val isPlayer = ship.id == Global.getCombatEngine().playerShip.id

        val currentSpeedBoost = initialSpeedBoost - Easing.Quadratic.easeOut(
            time = timeElapsed,
            valueAtStart = 0f,
            valueAtEnd = initialSpeedBoost,
            duration = (ship.system.specAPI.`in` + ship.system.specAPI.active)
        )
        stats.maxSpeed.modifyFlat(id, currentSpeedBoost)
        stats.acceleration.modifyFlat(id, currentSpeedBoost)
        stats.deceleration.modifyFlat(id, currentSpeedBoost)

        // Modified vanilla phase code (PhaseCloakStats)
//        val speedPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f)
//        val accelPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).computeEffective(0f)
//        stats.maxSpeed.modifyPercent(id, speedPercentMod * effectLevel)
//        stats.acceleration.modifyPercent(id, accelPercentMod * effectLevel)
//        stats.deceleration.modifyPercent(id, accelPercentMod * effectLevel)
//
//        val speedMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).getMult()
//        val accelMultMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_ACCEL_MOD).getMult()
//        stats.maxSpeed.modifyMult(id, speedMultMod * effectLevel)
//        stats.acceleration.modifyMult(id, accelMultMod * effectLevel)
//        stats.deceleration.modifyMult(id, accelMultMod * effectLevel)

        val levelForAlpha = effectLevel

        ship.isPhased = true
        ship.extraAlphaMult = 1f - (1f - PhaseCloakStats.SHIP_ALPHA_MULT) * levelForAlpha
        ship.setApplyExtraAlphaToEngines(true)

        val extra = 0f
        val shipTimeMult = 1f + (PhaseCloakStats.getMaxTimeMult(stats) - 1f) * levelForAlpha * (1f - extra)
        stats.timeMult.modifyMult(id, shipTimeMult)

        if (isPlayer) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / shipTimeMult)
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }
    }

    fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship = stats.entity as? ShipAPI ?: return
        Global.getCombatEngine() ?: return

        stats.maxSpeed.unmodifyFlat(id)
        stats.acceleration.unmodifyFlat(id)

        // Modified vanilla phase code (PhaseCloakStats)
        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)
        stats.maxSpeed.unmodify(id)
        stats.maxSpeed.unmodifyMult(id + "_2")
        stats.acceleration.unmodify(id)
        stats.deceleration.unmodify(id)
        ship.isPhased = false
        ship.extraAlphaMult = 1f
        ((ship.phaseCloak ?: ship.system) as? PhaseCloakSystemAPI)?.minCoilJitterLevel = 0f
    }
}