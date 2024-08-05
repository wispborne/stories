package wisp.questgiver.wispLib

import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.CommodityOnMarketAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.cb.BaseCustomBounty
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithSearch
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode
import org.lwjgl.util.vector.Vector2f

class SystemFinder
@JvmOverloads constructor(
    val mission: HubMissionWithSearch = BaseCustomBounty(),
    includeHiddenSystems: Boolean = false
) {

    init {
        if (!includeHiddenSystems) {
            mission.requireSystemTags(ReqMode.NOT_ANY, Tags.THEME_HIDDEN)
        }
    }

    val search: HubMissionWithSearch.SearchData
        get() = mission.search

    fun resetSearch() = mission.resetSearch()
        .run { this@SystemFinder }

    fun requireSystemInterestingAndNotCore() = mission.requireSystemInterestingAndNotCore()
        .run { this@SystemFinder }

    fun requireSystemInterestingAndNotUnsafeOrCore() = mission.requireSystemInterestingAndNotUnsafeOrCore()
        .run { this@SystemFinder }

    fun preferSystemInteresting() = mission.preferSystemInteresting()
        .run { this@SystemFinder }

    @Deprecated(
        "Use preferPlanetInDirectionOfOtherMissions instead.",
        ReplaceWith("preferPlanetInDirectionOfOtherMissions()")
    )
    fun preferSystemInDirectionOfOtherMissions() = mission.preferSystemInDirectionOfOtherMissions()
        .run { this@SystemFinder }

    fun requireSystemInDirection(dir: Float, arc: Float) = mission.requireSystemInDirection(dir, arc)
        .run { this@SystemFinder }

    fun preferSystemInDirection(dir: Float, arc: Float) = mission.preferSystemInDirection(dir, arc)
        .run { this@SystemFinder }

    fun requireSystemInDirectionFrom(from: Vector2f?, dir: Float, arc: Float) =
        mission.requireSystemInDirectionFrom(from, dir, arc)
            .run { this@SystemFinder }

    fun preferSystemInDirectionFrom(from: Vector2f?, dir: Float, arc: Float) =
        mission.preferSystemInDirectionFrom(from, dir, arc)
            .run { this@SystemFinder }

    /**
     * Shouldn't use "preferSystem" for these because the systems are picked BEFORE planets are checked so
     * e.g. we may pick 20 systems that "match", find that none of them have planets that match, and fall
     * back to the full set of systems. Using the preferPlanets method, we'll look at all-direction systems
     * and filter them out at the planet level.
     */
    fun preferPlanetInDirectionOfOtherMissions() = mission.preferPlanetInDirectionOfOtherMissions()
        .run { this@SystemFinder }

    fun preferEntityInDirectionOfOtherMissions() = mission.preferEntityInDirectionOfOtherMissions()
        .run { this@SystemFinder }

    fun preferTerrainInDirectionOfOtherMissions() = mission.preferTerrainInDirectionOfOtherMissions()
        .run { this@SystemFinder }

    fun preferMarketInDirectionOfOtherMissions() = mission.preferMarketInDirectionOfOtherMissions()
        .run { this@SystemFinder }

    fun requireSystemTags(mode: ReqMode?, vararg tags: String?) = mission.requireSystemTags(mode, *tags)
        .run { this@SystemFinder }

    fun preferSystemTags(mode: ReqMode?, vararg tags: String?) = mission.preferSystemTags(mode, *tags)
        .run { this@SystemFinder }

    fun requireSystemHasBase(factionId: String?) = mission.requireSystemHasBase(factionId)
        .run { this@SystemFinder }

    fun preferSystemHasBase(factionId: String?) = mission.preferSystemHasBase(factionId)
        .run { this@SystemFinder }

    fun requireSystemHasColony(factionId: String?, minSize: Int) =
        mission.requireSystemHasColony(factionId, minSize)
            .run { this@SystemFinder }

    fun preferSystemHasColony(factionId: String?, minSize: Int) =
        mission.preferSystemHasColony(factionId, minSize)
            .run { this@SystemFinder }

    fun requireSystemHasAtLeastNumJumpPoints(min: Int) = mission.requireSystemHasAtLeastNumJumpPoints(min)
        .run { this@SystemFinder }

    fun preferSystemHasAtLeastNumJumpPoints(min: Int) = mission.preferSystemHasAtLeastNumJumpPoints(min)
        .run { this@SystemFinder }

    fun requireSystemUnexplored() = mission.requireSystemUnexplored()
        .run { this@SystemFinder }

    fun preferSystemUnexplored() = mission.preferSystemUnexplored()
        .run { this@SystemFinder }

    fun requireSystemNotEnteredByPlayerFor(days: Float) = mission.requireSystemNotEnteredByPlayerFor(days)
        .run { this@SystemFinder }

    fun preferSystemNotEnteredByPlayerFor(days: Float) = mission.preferSystemNotEnteredByPlayerFor(days)
        .run { this@SystemFinder }

    fun requireSystemExplored() = mission.requireSystemExplored()
        .run { this@SystemFinder }

    fun preferSystemExplored() = mission.preferSystemExplored()
        .run { this@SystemFinder }

    fun requireSystemHasNumPlanets(num: Int) = mission.requireSystemHasNumPlanets(num)
        .run { this@SystemFinder }

    fun preferSystemHasNumPlanets(num: Int) = mission.preferSystemHasNumPlanets(num)
        .run { this@SystemFinder }

    fun requireSystemHasNumTerrain(num: Int) = mission.requireSystemHasNumTerrain(num)
        .run { this@SystemFinder }

    fun preferSystemHasNumTerrain(num: Int) = mission.preferSystemHasNumTerrain(num)
        .run { this@SystemFinder }

    fun requireSystemHasNumPlanetsAndTerrain(num: Int) = mission.requireSystemHasNumPlanetsAndTerrain(num)
        .run { this@SystemFinder }

    fun preferSystemHasNumPlanetsAndTerrain(num: Int) = mission.preferSystemHasNumPlanetsAndTerrain(num)
        .run { this@SystemFinder }

    fun requireSystemIsDense() = mission.requireSystemIsDense()
        .run { this@SystemFinder }

    fun preferSystemIsDense() = mission.preferSystemIsDense()
        .run { this@SystemFinder }

    fun requireSystemBlackHole() = mission.requireSystemBlackHole()
        .run { this@SystemFinder }

    fun requireSystemNebula() = mission.requireSystemNebula()
        .run { this@SystemFinder }

    fun requireSystemHasPulsar() = mission.requireSystemHasPulsar()
        .run { this@SystemFinder }

    fun preferSystemBlackHole() = mission.preferSystemBlackHole()
        .run { this@SystemFinder }

    fun preferSystemNebula() = mission.preferSystemNebula()
        .run { this@SystemFinder }

    fun preferSystemHasPulsar() = mission.preferSystemHasPulsar()
        .run { this@SystemFinder }

    fun requireSystemBlackHoleOrPulsarOrNebula() = mission.requireSystemBlackHoleOrPulsarOrNebula()
        .run { this@SystemFinder }

    fun preferSystemBlackHoleOrPulsarOrNebula() = mission.preferSystemBlackHoleOrPulsarOrNebula()
        .run { this@SystemFinder }

    fun requireSystemBlackHoleOrNebula() = mission.requireSystemBlackHoleOrNebula()
        .run { this@SystemFinder }

    fun preferSystemBlackHoleOrNebula() = mission.preferSystemBlackHoleOrNebula()
        .run { this@SystemFinder }

    fun requireSystemNotBlackHole() = mission.requireSystemNotBlackHole()
        .run { this@SystemFinder }

    fun requireSystemNotNebula() = mission.requireSystemNotNebula()
        .run { this@SystemFinder }

    fun requireSystemNotHasPulsar() = mission.requireSystemNotHasPulsar()
        .run { this@SystemFinder }

    fun requireSystemNotAlreadyUsedForStory() = mission.requireSystemNotAlreadyUsedForStory()
        .run { this@SystemFinder }

    /**
     * To avoid re-using the same system for different story things.
     */
    fun setSystemWasUsedForStory(stage: Any?, system: StarSystemAPI?) =
        mission.setSystemWasUsedForStory(stage, system)
            .run { this@SystemFinder }

    fun preferSystemNotBlackHole() = mission.preferSystemNotBlackHole()
        .run { this@SystemFinder }

    fun preferSystemNotNebula() = mission.preferSystemNotNebula()
        .run { this@SystemFinder }

    fun preferSystemNotPulsar() = mission.preferSystemNotPulsar()
        .run { this@SystemFinder }

    fun requireSystemHasSafeStars() = mission.requireSystemHasSafeStars()
        .run { this@SystemFinder }

    fun requireSystemInInnerSector() = mission.requireSystemInInnerSector()
        .run { this@SystemFinder }

    fun preferSystemInInnerSector() = mission.preferSystemInInnerSector()
        .run { this@SystemFinder }

    fun requireSystemOnFringeOfSector() = mission.requireSystemOnFringeOfSector()
        .run { this@SystemFinder }

    fun preferSystemOnFringeOfSector() = mission.preferSystemOnFringeOfSector()
        .run { this@SystemFinder }

    fun requireSystemWithinRangeOf(location: Vector2f?, rangeLY: Float) =
        mission.requireSystemWithinRangeOf(location, rangeLY)
            .run { this@SystemFinder }

    fun requireSystemWithinRangeOf(location: Vector2f?, minRangeLY: Float, maxRangeLY: Float) =
        mission.requireSystemWithinRangeOf(location, minRangeLY, maxRangeLY)
            .run { this@SystemFinder }

    fun preferSystemWithinRangeOf(location: Vector2f?, rangeLY: Float) =
        mission.preferSystemWithinRangeOf(location, rangeLY)
            .run { this@SystemFinder }

    fun preferSystemWithinRangeOf(location: Vector2f?, minRangeLY: Float, maxRangeLY: Float) =
        mission.preferSystemWithinRangeOf(location, minRangeLY, maxRangeLY)
            .run { this@SystemFinder }

    fun requireSystemOutsideRangeOf(location: Vector2f?, rangeLY: Float) =
        mission.requireSystemOutsideRangeOf(location, rangeLY)
            .run { this@SystemFinder }

    fun preferSystemOutsideRangeOf(location: Vector2f?, rangeLY: Float) =
        mission.preferSystemOutsideRangeOf(location, rangeLY)
            .run { this@SystemFinder }

    fun requirePlanetNotStar() = mission.requirePlanetNotStar()
        .run { this@SystemFinder }

    fun requirePlanetIsStar() = mission.requirePlanetIsStar()
        .run { this@SystemFinder }

    fun requirePlanetNotGasGiant() = mission.requirePlanetNotGasGiant()
        .run { this@SystemFinder }

    fun preferPlanetNonGasGiant() = mission.preferPlanetNonGasGiant()
        .run { this@SystemFinder }

    fun requirePlanetNotNearJumpPoint(minDist: Float) = mission.requirePlanetNotNearJumpPoint(minDist)
        .run { this@SystemFinder }

    fun preferPlanetNotNearJumpPoint(minDist: Float) = mission.preferPlanetNotNearJumpPoint(minDist)
        .run { this@SystemFinder }

    fun requirePlanetIsGasGiant() = mission.requirePlanetIsGasGiant()
        .run { this@SystemFinder }

    fun preferPlanetIsGasGiant() = mission.preferPlanetIsGasGiant()
        .run { this@SystemFinder }

    fun requirePlanetPopulated() = mission.requirePlanetPopulated()
        .run { this@SystemFinder }

    fun preferPlanetPopulated() = mission.preferPlanetPopulated()
        .run { this@SystemFinder }

    fun requirePlanetUnpopulated() = mission.requirePlanetUnpopulated()
        .run { this@SystemFinder }

    fun preferPlanetUnpopulated() = mission.preferPlanetUnpopulated()
        .run { this@SystemFinder }

    fun requirePlanetTags(mode: ReqMode?, vararg tags: String?) = mission.requirePlanetTags(mode, *tags)
        .run { this@SystemFinder }

    fun preferPlanetTags(mode: ReqMode?, vararg tags: String?) = mission.preferPlanetTags(mode, *tags)
        .run { this@SystemFinder }

    fun requirePlanetConditions(mode: ReqMode?, vararg tags: String?) =
        mission.requirePlanetConditions(mode, *tags)
            .run { this@SystemFinder }

    fun preferPlanetConditions(mode: ReqMode?, vararg conditions: String?) =
        mission.preferPlanetConditions(mode, *conditions)
            .run { this@SystemFinder }

    fun requirePlanetNotFullySurveyed() = mission.requirePlanetNotFullySurveyed()
        .run { this@SystemFinder }

    fun preferPlanetNotFullySurveyed() = mission.preferPlanetNotFullySurveyed()
        .run { this@SystemFinder }

    fun requirePlanetFullySurveyed() = mission.requirePlanetFullySurveyed()
        .run { this@SystemFinder }

    fun preferPlanetFullySurveyed() = mission.preferPlanetFullySurveyed()
        .run { this@SystemFinder }

    fun preferPlanetUnsurveyed() = mission.preferPlanetUnsurveyed()
        .run { this@SystemFinder }

    fun requirePlanetUnsurveyed() = mission.requirePlanetUnsurveyed()
        .run { this@SystemFinder }

    fun requirePlanetWithRuins() = mission.requirePlanetWithRuins()
        .run { this@SystemFinder }

    fun preferPlanetWithRuins() = mission.preferPlanetWithRuins()
        .run { this@SystemFinder }

    fun requirePlanetWithoutRuins() = mission.requirePlanetWithoutRuins()
        .run { this@SystemFinder }

    fun preferPlanetWithoutRuins() = mission.preferPlanetWithoutRuins()
        .run { this@SystemFinder }

    fun requirePlanetUnexploredRuins() = mission.requirePlanetUnexploredRuins()
        .run { this@SystemFinder }

    fun preferPlanetUnexploredRuins() = mission.preferPlanetUnexploredRuins()
        .run { this@SystemFinder }

    fun requireEntityTags(mode: ReqMode?, vararg tags: String?) = mission.requireEntityTags(mode, *tags)
        .run { this@SystemFinder }

    fun preferEntityTags(mode: ReqMode?, vararg tags: String?) = mission.preferEntityTags(mode, *tags)
        .run { this@SystemFinder }

    fun requireEntityType(vararg types: String?) = mission.requireEntityType(*types)
        .run { this@SystemFinder }

    fun preferEntityType(vararg types: String?) = mission.preferEntityType(*types)
        .run { this@SystemFinder }

    fun requireEntityMemoryFlags(vararg flags: String?) = mission.requireEntityMemoryFlags(*flags)
        .run { this@SystemFinder }

    fun preferEntityMemoryFlags(vararg flags: String?) = mission.preferEntityMemoryFlags(*flags)
        .run { this@SystemFinder }

    fun requireEntityUndiscovered() = mission.requireEntityUndiscovered()
        .run { this@SystemFinder }

    fun preferEntityUndiscovered() = mission.preferEntityUndiscovered()
        .run { this@SystemFinder }

    fun requireEntityNot(entity: SectorEntityToken?) = mission.requireEntityNot(entity)
        .run { this@SystemFinder }

    fun requirePlanetNot(planet: PlanetAPI?) = mission.requirePlanetNot(planet)
        .run { this@SystemFinder }

    fun requireSystemNot(system: StarSystemAPI?) = mission.requireSystemNot(system)
        .run { this@SystemFinder }

    fun requireSystemIs(system: StarSystemAPI?) = mission.requireSystemIs(system)
        .run { this@SystemFinder }

    fun requireSystem(req: HubMissionWithSearch.StarSystemRequirement?) = mission.requireSystem(req)
        .run { this@SystemFinder }

    fun preferSystem(req: HubMissionWithSearch.StarSystemRequirement?) = mission.preferSystem(req)
        .run { this@SystemFinder }

    fun pickFromMatching(matches: List<*>?, preferred: List<*>?): Any? =
        mission.pickFromMatching(matches, preferred)

    fun pickSystem(): StarSystemAPI? = mission.pickSystem()
    fun pickSystem(resetSearch: Boolean): StarSystemAPI? = mission.pickSystem(resetSearch)
    fun searchMakeSystemPreferencesMoreImportant(value: Boolean) =
        mission.searchMakeSystemPreferencesMoreImportant(value)
            .run { this@SystemFinder }

    fun pickPlanet(): PlanetAPI? = mission.pickPlanet()
    fun pickPlanet(resetSearch: Boolean): PlanetAPI? = mission.pickPlanet(resetSearch)
    fun pickEntity(): SectorEntityToken? = mission.pickEntity()
    fun pickEntity(resetSearch: Boolean): SectorEntityToken? = mission.pickEntity(resetSearch)
    fun pickMarket(): MarketAPI? = mission.pickMarket()
    fun pickMarket(resetSearch: Boolean): MarketAPI? = mission.pickMarket(resetSearch)
    fun pickCommodity(): CommodityOnMarketAPI? = mission.pickCommodity()
    fun pickCommodity(resetSearch: Boolean): CommodityOnMarketAPI? = mission.pickCommodity(resetSearch)
    fun requireMarketTacticallyBombardable() = mission.requireMarketTacticallyBombardable()
        .run { this@SystemFinder }

    fun requireMarketNotTacticallyBombardable() = mission.requireMarketNotTacticallyBombardable()
        .run { this@SystemFinder }

    fun preferMarketTacticallyBombardable() = mission.preferMarketTacticallyBombardable()
        .run { this@SystemFinder }

    fun preferMarketNotTacticallyBombardable() = mission.preferMarketNotTacticallyBombardable()
        .run { this@SystemFinder }

    fun requireMarketMilitary() = mission.requireMarketMilitary()
        .run { this@SystemFinder }

    fun preferMarketMilitary() = mission.preferMarketMilitary()
        .run { this@SystemFinder }

    fun requireMarketNotMilitary() = mission.requireMarketNotMilitary()
        .run { this@SystemFinder }

    fun preferMarketNotMilitary() = mission.preferMarketNotMilitary()
        .run { this@SystemFinder }

    fun requireMarketMemoryFlag(key: String?, value: Any?) = mission.requireMarketMemoryFlag(key, value)
        .run { this@SystemFinder }

    fun preferMarketMemoryFlag(key: String?, value: Any?) = mission.preferMarketMemoryFlag(key, value)
        .run { this@SystemFinder }

    fun requireMarketHidden() = mission.requireMarketHidden()
        .run { this@SystemFinder }

    fun preferMarketHidden() = mission.preferMarketHidden()
        .run { this@SystemFinder }

    fun requireMarketNotHidden() = mission.requireMarketNotHidden()
        .run { this@SystemFinder }

    fun preferMarketNotHidden() = mission.preferMarketNotHidden()
        .run { this@SystemFinder }

    fun requireMarketNotInHyperspace() = mission.requireMarketNotInHyperspace()
        .run { this@SystemFinder }

    fun preferMarketNotInHyperspace() = mission.preferMarketNotInHyperspace()
        .run { this@SystemFinder }

    fun requireMarketIs(id: String?) = mission.requireMarketIs(id)
        .run { this@SystemFinder }

    fun requireMarketIs(param: MarketAPI?) = mission.requireMarketIs(param)
        .run { this@SystemFinder }

    fun preferMarketIs(param: MarketAPI?) = mission.preferMarketIs(param)
        .run { this@SystemFinder }

    fun requireMarketIsNot(param: MarketAPI?) = mission.requireMarketIsNot(param)
        .run { this@SystemFinder }

    fun preferMarketIsNot(param: MarketAPI?) = mission.preferMarketIsNot(param)
        .run { this@SystemFinder }

    fun requireMarketFaction(vararg factions: String?) = mission.requireMarketFaction(*factions)
        .run { this@SystemFinder }

    fun preferMarketFaction(vararg factions: String?) = mission.preferMarketFaction(*factions)
        .run { this@SystemFinder }

    fun requireMarketFactionNot(vararg factions: String?) = mission.requireMarketFactionNot(*factions)
        .run { this@SystemFinder }

    fun preferMarketFactionNot(vararg factions: String?) = mission.preferMarketFactionNot(*factions)
        .run { this@SystemFinder }

    fun requireMarketFactionNotPlayer() = mission.requireMarketFactionNotPlayer()
        .run { this@SystemFinder }

    fun requireMarketFactionHostileTo(faction: String?) = mission.requireMarketFactionHostileTo(faction)
        .run { this@SystemFinder }

    fun preferMarketFactionHostileTo(faction: String?) = mission.preferMarketFactionHostileTo(faction)
        .run { this@SystemFinder }

    fun requireMarketFactionNotHostileTo(faction: String?) = mission.requireMarketFactionNotHostileTo(faction)
        .run { this@SystemFinder }

    fun preferMarketFactionNotHostileTo(faction: String?) = mission.preferMarketFactionNotHostileTo(faction)
        .run { this@SystemFinder }

    fun requireMarketLocation(vararg locations: String?) = mission.requireMarketLocation(*locations)
        .run { this@SystemFinder }

    fun requireMarketLocation(vararg locations: LocationAPI?) = mission.requireMarketLocation(*locations)
        .run { this@SystemFinder }

    fun preferMarketLocation(vararg locations: String?) = mission.preferMarketLocation(*locations)
        .run { this@SystemFinder }

    fun preferMarketLocation(vararg locations: LocationAPI?) = mission.preferMarketLocation(*locations)
        .run { this@SystemFinder }

    fun requireMarketLocationNot(vararg locations: String?) = mission.requireMarketLocationNot(*locations)
        .run { this@SystemFinder }

    fun requireMarketLocationNot(vararg locations: LocationAPI?) = mission.requireMarketLocationNot(*locations)
        .run { this@SystemFinder }

    fun preferMarketLocationNot(vararg locations: String?) = mission.preferMarketLocationNot(*locations)
        .run { this@SystemFinder }

    fun preferMarketLocationNot(vararg locations: LocationAPI?) = mission.preferMarketLocationNot(*locations)
        .run { this@SystemFinder }

    fun requireMarketFactionCustom(mode: ReqMode?, vararg custom: String?) =
        mission.requireMarketFactionCustom(mode, *custom)
            .run { this@SystemFinder }

    fun preferMarketFactionCustom(mode: ReqMode?, vararg custom: String?) =
        mission.preferMarketFactionCustom(mode, *custom)
            .run { this@SystemFinder }

    fun requireMarketSizeAtLeast(size: Int) = mission.requireMarketSizeAtLeast(size)
        .run { this@SystemFinder }

    fun preferMarketSizeAtLeast(size: Int) = mission.preferMarketSizeAtLeast(size)
        .run { this@SystemFinder }

    fun requireMarketSizeAtMost(size: Int) = mission.requireMarketSizeAtMost(size)
        .run { this@SystemFinder }

    fun preferMarketSizeAtMost(size: Int) = mission.preferMarketSizeAtMost(size)
        .run { this@SystemFinder }

    fun requireMarketStabilityAtLeast(stability: Int) = mission.requireMarketStabilityAtLeast(stability)
        .run { this@SystemFinder }

    fun preferMarketStabilityAtLeast(stability: Int) = mission.preferMarketStabilityAtLeast(stability)
        .run { this@SystemFinder }

    fun requireMarketStabilityAtMost(stability: Int) = mission.requireMarketStabilityAtMost(stability)
        .run { this@SystemFinder }

    fun preferMarketStabilityAtMost(stability: Int) = mission.preferMarketStabilityAtMost(stability)
        .run { this@SystemFinder }

    fun requireMarketConditions(mode: ReqMode?, vararg conditions: String?) =
        mission.requireMarketConditions(mode, *conditions)
            .run { this@SystemFinder }

    fun preferMarketConditions(mode: ReqMode?, vararg conditions: String?) =
        mission.preferMarketConditions(mode, *conditions)
            .run { this@SystemFinder }

    fun requireMarketIndustries(mode: ReqMode?, vararg industries: String?) =
        mission.requireMarketIndustries(mode, *industries)
            .run { this@SystemFinder }

    fun preferMarketIndustries(mode: ReqMode?, vararg industries: String?) =
        mission.preferMarketIndustries(mode, *industries)
            .run { this@SystemFinder }

    fun requireMarketIsMilitary() = mission.requireMarketIsMilitary()
        .run { this@SystemFinder }

    fun preferMarketIsMilitary() = mission.preferMarketIsMilitary()
        .run { this@SystemFinder }

    fun requireMarketHasSpaceport() = mission.requireMarketHasSpaceport()
        .run { this@SystemFinder }

    fun preferMarketHasSpaceport() = mission.preferMarketHasSpaceport()
        .run { this@SystemFinder }

    fun requireMarketNotHasSpaceport() = mission.requireMarketNotHasSpaceport()
        .run { this@SystemFinder }

    fun preferMarketNotHasSpaceport() = mission.preferMarketNotHasSpaceport()
        .run { this@SystemFinder }

    fun requireCommodityIsNotPersonnel() = mission.requireCommodityIsNotPersonnel()
        .run { this@SystemFinder }

    fun preferCommodityIsNotPersonnel() = mission.preferCommodityIsNotPersonnel()
        .run { this@SystemFinder }

    fun requireCommodityLegal() = mission.requireCommodityLegal()
        .run { this@SystemFinder }

    fun preferCommodityLegal() = mission.preferCommodityLegal()
        .run { this@SystemFinder }

    fun requireCommodityIllegal() = mission.requireCommodityIllegal()
        .run { this@SystemFinder }

    fun preferCommodityIllegal() = mission.preferCommodityIllegal()
        .run { this@SystemFinder }

    fun requireCommodityIs(id: String?) = mission.requireCommodityIs(id)
        .run { this@SystemFinder }

    fun preferCommodityIs(id: String?) = mission.preferCommodityIs(id)
        .run { this@SystemFinder }

    fun requireCommodityTags(mode: ReqMode?, vararg tags: String?) = mission.requireCommodityTags(mode, *tags)
        .run { this@SystemFinder }

    fun preferCommodityTags(mode: ReqMode?, vararg tags: String?) = mission.preferCommodityTags(mode, *tags)
        .run { this@SystemFinder }

    fun requireCommodityAvailableAtLeast(qty: Int) = mission.requireCommodityAvailableAtLeast(qty)
        .run { this@SystemFinder }

    fun preferCommodityAvailableAtLeast(qty: Int) = mission.preferCommodityAvailableAtLeast(qty)
        .run { this@SystemFinder }

    fun requireCommodityAvailableAtMost(qty: Int) = mission.requireCommodityAvailableAtMost(qty)
        .run { this@SystemFinder }

    fun preferCommodityAvailableAtMost(qty: Int) = mission.preferCommodityAvailableAtMost(qty)
        .run { this@SystemFinder }

    fun requireCommodityDemandAtLeast(qty: Int) = mission.requireCommodityDemandAtLeast(qty)
        .run { this@SystemFinder }

    fun preferCommodityDemandAtLeast(qty: Int) = mission.preferCommodityDemandAtLeast(qty)
        .run { this@SystemFinder }

    fun requireCommodityDemandAtMost(qty: Int) = mission.requireCommodityDemandAtMost(qty)
        .run { this@SystemFinder }

    fun preferCommodityDemandAtMost(qty: Int) = mission.preferCommodityDemandAtMost(qty)
        .run { this@SystemFinder }

    fun requireCommodityProductionAtLeast(qty: Int) = mission.requireCommodityProductionAtLeast(qty)
        .run { this@SystemFinder }

    fun preferCommodityProductionAtLeast(qty: Int) = mission.preferCommodityProductionAtLeast(qty)
        .run { this@SystemFinder }

    fun requireCommodityProductionAtMost(qty: Int) = mission.requireCommodityProductionAtMost(qty)
        .run { this@SystemFinder }

    fun preferCommodityProductionAtMost(qty: Int) = mission.preferCommodityProductionAtMost(qty)
        .run { this@SystemFinder }

    fun requireCommoditySurplusAtLeast(qty: Int) = mission.requireCommoditySurplusAtLeast(qty)
        .run { this@SystemFinder }

    fun preferCommoditySurplusAtLeast(qty: Int) = mission.preferCommoditySurplusAtLeast(qty)
        .run { this@SystemFinder }

    fun requireCommoditySurplusAtMost(qty: Int) = mission.requireCommoditySurplusAtMost(qty)
        .run { this@SystemFinder }

    fun preferCommoditySurplusAtMost(qty: Int) = mission.preferCommoditySurplusAtMost(qty)
        .run { this@SystemFinder }

    fun requireCommodityDeficitAtLeast(qty: Int) = mission.requireCommodityDeficitAtLeast(qty)
        .run { this@SystemFinder }

    fun preferCommodityDeficitAtLeast(qty: Int) = mission.preferCommodityDeficitAtLeast(qty)
        .run { this@SystemFinder }

    fun requireCommodityDeficitAtMost(qty: Int) = mission.requireCommodityDeficitAtMost(qty)
        .run { this@SystemFinder }

    fun preferCommodityDeficitAtMost(qty: Int) = mission.preferCommodityDeficitAtMost(qty)
        .run { this@SystemFinder }

    fun requireCommodityBasePriceAtLeast(price: Float) = mission.requireCommodityBasePriceAtLeast(price)
        .run { this@SystemFinder }

    fun preferCommodityBasePriceAtLeast(price: Float) = mission.preferCommodityBasePriceAtLeast(price)
        .run { this@SystemFinder }

    fun requireCommodityBasePriceAtMost(price: Float) = mission.requireCommodityBasePriceAtMost(price)
        .run { this@SystemFinder }

    fun preferCommodityBasePriceAtMost(price: Float) = mission.preferCommodityBasePriceAtMost(price)
        .run { this@SystemFinder }

    fun requireTerrainType(mode: ReqMode?, vararg types: String?) = mission.requireTerrainType(mode, *types)
        .run { this@SystemFinder }

    fun preferTerrainType(mode: ReqMode?, vararg types: String?) = mission.preferTerrainType(mode, *types)
        .run { this@SystemFinder }

    fun requireTerrainTags(mode: ReqMode?, vararg tags: String?) = mission.requireTerrainTags(mode, *tags)
        .run { this@SystemFinder }

    fun preferTerrainTags(mode: ReqMode?, vararg tags: String?) = mission.preferTerrainTags(mode, *tags)
        .run { this@SystemFinder }

    fun requireTerrainHasSpecialName() = mission.requireTerrainHasSpecialName()
        .run { this@SystemFinder }

    fun preferTerrainHasSpecialName() = mission.preferTerrainHasSpecialName()
        .run { this@SystemFinder }

    fun pickTerrain(): CampaignTerrainAPI? = mission.pickTerrain()
    fun pickTerrain(resetSearch: Boolean): CampaignTerrainAPI? = mission.pickTerrain(resetSearch)

    // Wisp custom added

    fun requirePlanet(req: HubMissionWithSearch.PlanetRequirement?) = mission.search.planetReqs.add(req)
        .run { this@SystemFinder }

    fun preferPlanet(req: HubMissionWithSearch.PlanetRequirement?) = mission.search.planetPrefs.add(req)
        .run { this@SystemFinder }
}