package wisp.questgiver.v2

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemKeys
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.util.Misc
import java.util.*

/**
 * Logic here is copied from [com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionBarEventWrapper]
 * and the FireBest/rules.csv logic is removed.
 */
abstract class HubMissionBarEventWrapperWithoutRules<H : QGHubMissionWithBarEvent>(
    specId: String?,
) : BaseBarEvent() {
    protected var seed: Long = 0

//    @Transient
//    protected var spec: BarEventSpec? = null

    var specId: String? = specId
        protected set

    @Transient
    protected lateinit var genRandom: Random

    @Transient
    var mission: H? = null
        protected set

    init {
        seed = Misc.genRandomSeed()
//        spec = Global.getSettings().getBarEventSpec(specId)
    }

    protected open fun readResolve(): Any {
//        spec = Global.getSettings().getBarEventSpec(specId)
        return this
    }

    override fun getBarEventId(): String? {
        return specId
    }

    abstract fun createMission(): H

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        if (shownAt != null && shownAt !== market) return false
        abortMission()

        genRandom = Random(seed + market?.id.hashCode() * 181783497276652981L)
//        if (genRandom.nextFloat() > spec!!.prob) return false
//        mission = spec!!.createMission()
        mission = createMission()
        mission!!.missionId = specId
        mission!!.genRandom = genRandom

        return mission!!.shouldShowAtMarket(market)
    }

    open fun abortMission() {
        if (mission != null) {
            mission!!.abort()
            mission = null
        }
    }

    override fun addPromptAndOption(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI?>) {
        val market = dialog.interactionTarget.market
        mission!!.createAndAbortIfFailed(market, true)

        if (mission!!.isMissionCreationAborted) {
            mission = null
            return
        }

        val prev = memoryMap[MemKeys.LOCAL]
        if (mission!!.person != null) {
            memoryMap[MemKeys.ENTITY] = prev
            memoryMap[MemKeys.LOCAL] = mission!!.person.memoryWithoutUpdate
            memoryMap[MemKeys.PERSON_FACTION] = mission!!.person.faction.memory
        }
        mission!!.updateInteractionData(dialog, memoryMap)
//        FireBest.fire(null, dialog, memoryMap, mission!!.triggerPrefix + "_blurbBar true")
//        FireBest.fire(null, dialog, memoryMap, mission!!.triggerPrefix + "_optionBar true")
        memoryMap[MemKeys.LOCAL] = prev
        memoryMap.remove(MemKeys.ENTITY)
        memoryMap.remove(MemKeys.PERSON_FACTION)
    }
}