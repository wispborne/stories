package wisp.questgiver

import com.fs.starfarer.api.campaign.PlanetAPI
import com.fs.starfarer.api.campaign.SectorAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import wisp.questgiver.Questgiver.game

data class Configuration(
    val blacklist: Blacklist,
    val whitelist: Whitelist
) {
    data class Blacklist(
        val systemIds: List<String>,
        val marketIds: List<String>,
        val systemTags: List<String>
    )

    data class Whitelist(
        val factionIds: List<String>
    )

    fun isValidQuestTarget(entity: SectorEntityToken?): Boolean =
        entity != null
                && entity.tags.none { tag -> tag in blacklist.systemTags }
                && entity.starSystem.isValidQuestTarget

    fun isValidQuestTarget(entity: PlanetAPI?): Boolean = entity != null
            && (isValidQuestTarget(entity as SectorEntityToken)
            && isValidQuestTarget(entity.market))

    fun isValidQuestTarget(system: StarSystemAPI?): Boolean =
        system != null
                && system.id !in blacklist.systemIds
                && system.tags.none { tag -> tag in blacklist.systemTags }

    fun isValidQuestTarget(market: MarketAPI?): Boolean =
        market != null
                && market.starSystem.isValidQuestTarget
                && market.id !in blacklist.marketIds
                && market.factionId in whitelist.factionIds
                && market.tags.none { tag -> tag in blacklist.systemTags }
                && market.connectedEntities.all { entity -> entity.tags.none { tag -> tag in blacklist.systemTags } }

    fun getStarSystemsThatAreValidQuestTargets(sector: SectorAPI) =
        sector.starSystems
            .filter { it.isValidQuestTarget }
}

val StarSystemAPI?.isValidQuestTarget: Boolean
    get() {
        return game.configuration.isValidQuestTarget(this ?: return false)
    }


val MarketAPI?.isValidQuestTarget: Boolean
    get() {
        return game.configuration.isValidQuestTarget(this ?: return false)
    }


val SectorEntityToken?.isValidQuestTarget: Boolean
    get() {
        return game.configuration.isValidQuestTarget(this ?: return false)
    }

val PlanetAPI?.isValidQuestTarget: Boolean
    get() {
        return game.configuration.isValidQuestTarget(this ?: return false)
    }

val SectorAPI.starSystemsAllowedForQuests: List<StarSystemAPI>
    get() = game.sector.starSystems.filter { it.isValidQuestTarget }

val StarSystemAPI?.planetsAllowedForQuests: List<PlanetAPI>
    get() = this?.planets?.filter { it.isValidQuestTarget } ?: emptyList()