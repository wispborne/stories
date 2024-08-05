package wisp.questgiver

import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import wisp.questgiver.wispLib.distanceFrom
import wisp.questgiver.wispLib.removeBarEventCreator
import kotlin.math.roundToInt


/**
 * Gives a base 3500 credits per LY. Roughly 1/3 of the sector is ~120,000 credits.
 */
fun Questgiver.calculateCreditReward(
    distanceInLY: Float,
    scaling: Float = 1f,
    max: Float? = null,
    min: Float? = null
): Float =
    (scaling * 3500 * distanceInLY.roundToInt())
        .run { if (min != null) coerceAtLeast(min) else this }
        .run { if (max != null) coerceAtMost(max) else this }

/**
 * Gives a base 3500 credits per LY. Roughly 1/3 of the sector is ~120,000 credits.
 */
fun Questgiver.calculateCreditReward(
    startLocation: SectorEntityToken?,
    endLocation: SectorEntityToken?,
    scaling: Float = 1f,
    max: Float? = null,
    min: Float? = null
): Float =
    calculateCreditReward(
        distanceInLY = endLocation?.let { startLocation?.distanceFrom(it) } ?: 0F,
        scaling = scaling
    )

/**
 * If quest has not been started, ensures that the [BarEventManager] has the [barEventCreator].
 *
 * If quest has been started, ensures that the [BarEventManager] does not have an instance of [barEventCreator].
 */
fun BarEventManager.configureBarEventCreator(
    shouldGenerateBarEvent: Boolean,
    barEventCreator: BarEventManager.GenericBarEventCreator,
    isStarted: Boolean
) {
    val hasEventCreator =
        this.hasEventCreator(barEventCreator::class.java)

    if (!shouldGenerateBarEvent || isStarted) {
        if (hasEventCreator) {
            this.removeBarEventCreator(barEventCreator::class.java)
        }
    } else {
        if (this.creators.count { it::class.java == barEventCreator::class.java } > 1) {
            this.removeBarEventCreator(barEventCreator::class.java)
        }

        if (!hasEventCreator) {
            this.addEventCreator(barEventCreator)
        }
    }
}

fun MarketAPI?.isPopulated(): Boolean = this == null || !this.isPlanetConditionMarketOnly