package wisp.perseanchronicles.telos.pt3_arrow

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.campaign.CampaignEngine
import wisp.perseanchronicles.game
import wisp.questgiver.wispLib.Easing

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
    @Deprecated("doesn't work due to bug fixed in 0.97a")
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
            // TODO after 0.97, change to `game.sector.campaignUI.maxZoomFactor`
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
//        CampaignEngine.getInstance().uiData.campaignZoom =
//            newZoom // TODO does this work? Asked <https://fractalsoftworks.com/forum/index.php?topic=5061.msg423025#msg423025>
//        game.sector.viewport.isExternalControl = true
//        game.sector.viewport.viewMult = newZoom
        game.sector.campaignUI.maxZoomFactor = newZoom
        game.sector.campaignUI.minZoomFactor = newZoom
        isFirstRun = false
    }

    companion object {
        // Note: `maxZoomFactor` doesn't return the correct value (returns min instead of max).
        // Will be fixed in 0.97a.
        fun getVanillaMaxZoom() = game.settings.getFloat("maxCampaignZoom") ?: 3f
    }
}