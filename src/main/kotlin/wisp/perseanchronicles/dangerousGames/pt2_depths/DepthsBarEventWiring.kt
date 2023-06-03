package wisp.perseanchronicles.dangerousGames.pt2_depths

import wisp.perseanchronicles.dangerousGames.pt1_dragons.DragonsHubMission
import wisp.perseanchronicles.game
import wisp.questgiver.BarEventWiring
import wisp.questgiver.QGBarEventCreator

class DepthsBarEventWiring :
    BarEventWiring<DepthsHubMission>(missionId = DepthsHubMission.MISSION_ID, isPriority = false) {
    override fun createBarEventLogic() = DepthsBarEventLogic()
    override fun createMission() = DepthsHubMission()
    override fun shouldBeAddedToBarEventPool(): Boolean {
        return DragonsHubMission.state.completeDateInMillis != null
                && game.sector.clock.getElapsedDaysSince(DragonsHubMission.state.completeDateInMillis!!) >= 30
                && DepthsHubMission.state.completeDateInMillis == null
    }

    override fun createBarEventCreator() = DepthsBarEventCreator(this)
}

class DepthsBarEventCreator(wiring: DepthsBarEventWiring) : QGBarEventCreator<DepthsHubMission>(wiring)