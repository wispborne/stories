package wisp.perseanchronicles.telos.pt3_arrow

import Easing
import com.fs.starfarer.api.EveryFrameScript
import wisp.perseanchronicles.game

class SmoothScrollPlayerCampaignZoomScript(
    val endingZoom: Float,
    val endingMaxZoom: Float? = null,
    val duration: Float = 0.5f,
) : EveryFrameScript {
    @Transient
    var runningTime = 0f

    @Transient
    val startingZoom = game.sector.campaignUI.zoomFactor

    @Transient
    val startingMinZoom = game.sector.campaignUI.minZoomFactor

    @Transient
    val startingMaxZoom = game.sector.campaignUI.maxZoomFactor

    @Transient
    val vanillaMaxZoom = game.sector.campaignUI.maxZoomFactor

    @Transient
    var isFirstRun = true

    override fun isDone(): Boolean {
        val isDone = runningTime >= duration

        if (isDone && endingMaxZoom != null) {
            game.sector.campaignUI.maxZoomFactor = endingMaxZoom
        }

        if (isDone) {
            game.sector.viewport.isExternalControl = false
        }

        // Reset min zoom that was used as a workaround to modify player zoom due to lack of direct access to zoom.
        // Min zoom was used to zoom out from small.
        if (isDone) {
            game.sector.campaignUI.minZoomFactor = startingMinZoom
        }

        return isDone
    }

    override fun runWhilePaused() = true

    override fun advance(amount: Float) {
        if (endingMaxZoom != null) {
            // If we're growing the zoom to a larger max, we need to grow the max first.
            // Otherwise, the smooth zoom will hit the current max and stop.
            if (startingZoom < endingMaxZoom && isFirstRun) {
                game.sector.campaignUI.maxZoomFactor = endingMaxZoom
            }
        }

        runningTime += amount
        val newZoom = Easing.Quadratic.easeOut(
            time = runningTime,
            valueAtStart = startingZoom,
            valueAtEnd = endingZoom,
            duration = duration
        )

        game.sector.campaignUI.maxZoomFactor = newZoom
        game.sector.campaignUI.minZoomFactor = newZoom
        isFirstRun = false
    }

    companion object {
        fun getVanillaMaxZoom() = game.settings.getFloat("maxCampaignZoom") ?: 3f
    }
}