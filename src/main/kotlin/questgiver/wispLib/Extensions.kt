@file:Suppress("NOTHING_TO_INLINE")

package wisp.questgiver.wispLib

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.SettingsAPI
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.procgen.Constellation
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import org.apache.log4j.Priority
import org.json.JSONArray
import org.json.JSONObject
import org.lazywizard.lazylib.ext.json.getFloat
import org.lwjgl.util.vector.Vector2f
import wisp.questgiver.Questgiver.game
import wisp.questgiver.isValidQuestTarget
import wisp.questgiver.starSystemsAllowedForQuests
import java.awt.Color
import kotlin.math.pow
import kotlin.random.Random
import kotlin.system.measureTimeMillis


/**
 * How far the vector is from the other vector.
 */
inline fun Vector2f.distanceFrom(other: Vector2f): Float =
    Misc.getDistanceLY(this, other)

/**
 * How far the token's system is from the center of the sector.
 */
val SectorEntityToken.distanceFromCenterOfSector: Float
    get() = this.starSystem.distanceFromCenterOfSector


/**
 * How far the system is from another system.
 */
inline fun StarSystemAPI.distanceFrom(other: StarSystemAPI): Float =
    Misc.getDistanceLY(this.location, other.location)

/**
 * How far the token is from another token, in hyperspace.
 */
inline fun SectorEntityToken.distanceFrom(other: SectorEntityToken): Float =
    Misc.getDistanceLY(
        this.locationInHyperspace,
        other.locationInHyperspace
    )

/**
 * How far the system is from the center of the sector.
 */
inline val StarSystemAPI.distanceFromCenterOfSector: Float
    get() = Misc.getDistanceLY(
        this.location,
        game.sector.hyperspace.location
    )

/**
 * How far the token's system is from the player's fleet, in LY.
 */
val SectorEntityToken.distanceFromPlayerInHyperspace: Float
    get() = this.starSystem.distanceFromPlayerInHyperspace

/**
 * How far the system is from the player's fleet, in LY.
 */
inline val StarSystemAPI.distanceFromPlayerInHyperspace: Float
    get() = Misc.getDistanceLY(
        this.location,
        game.sector.playerFleet.locationInHyperspace
    )

/**
 * How far the point is from the player's fleet, in LY.
 */
inline val Vector2f.distanceFromPlayerInHyperspace: Float
    get() = Misc.getDistanceLY(
        this,
        game.sector.playerFleet.locationInHyperspace
    )

/**
 * Empty string, `""`.
 */
inline val String.Companion.empty
    get() = ""

/**
 * Creates a token for the fleet at its current location.
 */
fun CampaignFleetAPI.createToken(): SectorEntityToken = this.containingLocation.createToken(this.location)

/**
 * Whether the point is inside the circle.
 */
fun isPointInsideCircle(
    point: Vector2f,
    circleCenter: Vector2f,
    circleRadius: Float
): Boolean = (point.x - circleCenter.x).pow(2) +
        (point.y - circleCenter.y).pow(2) < circleRadius.pow(2)

/**
 * @see [isPointInsideCircle]
 */
fun Vector2f.isInsideCircle(
    center: Vector2f,
    radius: Float
) = isPointInsideCircle(this, center, radius)

/**
 * Displays the dialog as an interaction with [targetEntity].
 */
fun InteractionDialogPlugin.show(campaignUIAPI: CampaignUIAPI, targetEntity: SectorEntityToken) =
    campaignUIAPI.showInteractionDialog(this, targetEntity)

/**
 * Gets the first intel of the given type.
 */
fun <T : IntelInfoPlugin> IntelManagerAPI.findFirst(intelClass: Class<T>): T? =
    this.getFirstIntel(intelClass) as? T

/**
 * Gets the first intel of the given type.
 */
inline fun <reified T : IntelInfoPlugin> IntelManagerAPI.findFirst(): T? =
    this.getFirstIntel(T::class.java) as? T

/**
 * The player's first name. Falls back to their full name, and then to "No-Name" if they have no name.
 */
val PersonAPI.firstName: String
    get() = this.name?.first?.ifBlank { null }
        ?: this.nameString
        ?: "No-Name"

/**
 * The player's last name. Falls back to their full name, and then to "No-Name" if they have no name.
 */
val PersonAPI.lastName: String
    get() = this.name?.last?.ifBlank { null }
        ?: this.nameString
        ?: "No-Name"

/**
 * Removes a [BaseBarEventCreator] immediately.
 */
fun <T : BarEventManager.GenericBarEventCreator> BarEventManager.removeBarEventCreator(barEventCreatorClass: Class<T>) {
    this.setTimeout(barEventCreatorClass, 0f)
    this.creators.removeAll { it::class.java == barEventCreatorClass }
}

/**
 * Adds the [BaseBarEventCreator] to the [BarEventManager] if it isn't already present and if the [predicate] returns true.
 */
inline fun <reified T : BaseBarEventCreator> BarEventManager.addBarEventCreatorIf(
    barEventCreator: T = T::class.java.newInstance(),
    predicate: () -> Boolean
) {
    if (!this.hasEventCreator(barEventCreator::class.java) && predicate()) {
        this.addEventCreator(barEventCreator)
    }
}

/**
 * True if any of the arguments are equal; false otherwise.
 */
fun Any.equalsAny(vararg other: Any): Boolean = arrayOf(*other).any { this == it }

/**
 * Returns `primaryEntity` if non-null, or the first item in `connectedEntities` otherwise. Returns `null` if `connectedEntities` is empty.
 */
val MarketAPI.preferredConnectedEntity: SectorEntityToken?
    get() = this.primaryEntity ?: this.connectedEntities.firstOrNull()

fun List<PlanetAPI>.getNonHostileOnlyIfPossible(): List<PlanetAPI> {
    val nonHostile = this.filter { it.market?.faction?.isHostileTo(game.sector.playerFaction.id) == true }
    return if (nonHostile.isNotEmpty()) nonHostile else this
}

/**
 * Returns items matching the predicate or, if none are matching, returns the original [List].
 */
fun <T> List<T>.prefer(predicate: (item: T) -> Boolean): List<T> =
    this.filter { predicate(it) }
        .ifEmpty { this }

fun BaseIntelPlugin.endAndNotifyPlayer(delayBeforeEndingInDays: Float = 3f) {
    this.endAfterDelay(delayBeforeEndingInDays)
    this.sendUpdateIfPlayerHasIntel(null, false)
}

val LocationAPI.actualPlanets: List<PlanetAPI>
    get() = this.planets.filter { it.isValidQuestTarget && !it.isStar }

val LocationAPI.solidPlanets: List<PlanetAPI>
    get() = this.planets.filter { it.isValidQuestTarget && it.isSolidPlanet }

fun SectorEntityToken.hasSameMarketAs(other: SectorEntityToken?) =
    this.market != null && this.market.id == other?.market?.id

val PlanetAPI.isSolidPlanet: Boolean
    get() = !this.isStar && !this.isGasGiant

fun SectorAPI.getConstellations(): List<Constellation> =
    this.starSystemsAllowedForQuests
        .mapNotNull { it.constellation }
        .distinctBy { it.name }

fun ClosedFloatingPointRange<Float>.random(): Float =
    (this.start + (this.endInclusive - this.start) * Random.nextFloat())

/**
 * Returns true if the two circles have any overlap, false otherwise.
 * (x1 - x0)^2 + (y1 - y0)^2 >= (r1 + r0)^2 (thanks, Greg)
 */
fun doCirclesIntersect(centerA: Vector2f, radiusA: Float, centerB: Vector2f, radiusB: Float) =
    (centerB.x - centerA.x).pow(2) + (centerB.y - centerA.y).pow(2) >= (radiusA + radiusB).pow(2)

operator fun Priority.compareTo(other: Priority) = this.toInt().compareTo(other.toInt())

fun JSONArray.toStringList(): List<String> {
    return MutableList(this.length()) {
        this.getString(it)
    }
        .filterNotNull()
}

fun JSONArray.toLongList(): List<Long> {
    return MutableList(this.length()) {
        this.getLong(it)
    }
}

inline fun <reified T> JSONObject.getObj(key: String): T =
    getJsonObj(this, key)

inline fun <reified T> JSONObject.tryGet(key: String, default: () -> T): T =
    kotlin.runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

inline fun <reified T> JSONObject.optional(key: String, default: () -> T? = { null }): T? =
    kotlin.runCatching { getJsonObj<T>(this, key) }
        .getOrDefault(default())

inline fun <reified T> JSONArray.forEach(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    action: (T) -> Unit
) {
    for (i in (0 until this.length()))
        action.invoke(transform.invoke(this, i))
}

inline fun <reified T, K> JSONArray.map(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    action: (T) -> K
): List<K> {
    val results = mutableListOf<K>()

    for (i in (0 until this.length()))
        results += action.invoke(transform.invoke(this, i))

    return results
}

inline fun <reified T> JSONArray.filter(
    transform: (JSONArray, Int) -> T = { json, i -> getJsonObjFromArray(json, i) },
    predicate: (T) -> Boolean
): List<T> {
    val results = mutableListOf<T>()

    for (i in (0 until this.length())) {
        val obj = transform.invoke(this, i)

        if (predicate.invoke(obj)) {
            results += obj
        }
    }

    return results
}

inline fun <reified T> getJsonObjFromArray(json: JSONArray, i: Int) =
    when (T::class) {
        String::class -> json.getString(i) as T
        Float::class -> json.getFloat(i) as T
        Int::class -> json.getInt(i) as T
        Boolean::class -> json.getBoolean(i) as T
        Double::class -> json.getDouble(i) as T
        JSONArray::class -> json.getJSONArray(i) as T
        Long::class -> json.getLong(i) as T
        else -> json.getJSONObject(i) as T
    }

inline fun <reified T> getJsonObj(json: JSONObject, key: String) =
    when (T::class) {
        String::class -> json.getString(key) as T
        Float::class -> json.getFloat(key) as T
        Int::class -> json.getInt(key) as T
        Boolean::class -> json.getBoolean(key) as T
        Double::class -> json.getDouble(key) as T
        JSONArray::class -> json.getJSONArray(key) as T
        Long::class -> json.getLong(key) as T
        else -> json.getJSONObject(key) as T
    }

fun <T> T?.asList(): List<T> = if (this == null) emptyList() else listOf(this)

fun getJavaVersion(): Int {
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version.filter { it.isDigit() }.toInt()
}

inline fun HubMissionWithTriggers.trigger(actions: () -> Unit) {
    actions.invoke()
    this.endTrigger()
}

inline val BaseHubMission.isStarted: Boolean
    get() = this.currentStage != null

/**
 * Given a string `"(test1 (inner) t)"`, returns `"test1 (inner) t"`.
 * @param openChar The opening bracket/parenthesis char, eg `(`.
 * @param closeChar The closing bracket/parenthesis char, eg `)`.
 */
fun String.textInsideSurroundingChars(openChar: Char, closeChar: Char) =
    getTextInsideChars(
        stringStartingWithOpeningChar = this,
        openChar = openChar,
        closeChar = closeChar
    )

internal fun getTextInsideChars(stringStartingWithOpeningChar: String, openChar: Char, closeChar: Char): String {
    var pos = 0
    var numOpen = 0
    val str = stringStartingWithOpeningChar

    if (str[0] != openChar)
        return "First char should be '$openChar' but was '${str[0]}' in string '$str'."

    while (pos < str.length) {
        when (str[pos]) {
            openChar -> numOpen++
            closeChar -> numOpen--
        }

        if (numOpen == 0) {
            return str.substring(1, pos)
        }

        pos++
    }

    return str.substring(1)
}

fun CampaignFleetAPI.addShipVariant(
    variantOrHullId: String,
    count: Int = 1,
    combatReadinessPercent: Float? = null
): List<FleetMemberAPI> {
    val ret = mutableListOf<FleetMemberAPI>()
    repeat(count) {
        val variant =
            kotlin.runCatching { Global.getSettings().getVariant(variantOrHullId) ?: throw RuntimeException() }
                // If no variant, use the vanilla generated one.
                .recover { Global.getSettings().getVariant(variantOrHullId + "_Hull") }
                .getOrNull()

        fleetData.addFleetMember(
            Global.getFactory().createFleetMember(
                FleetMemberType.SHIP,
                variant
            )
                .also {
                    it.status.repairFully()
                    it.repairTracker.cr = combatReadinessPercent ?: 0.7f
                    it.setStatUpdateNeeded(true)
                    ret.add(it)
                }
        )
    }

    return ret
}

/**
 * Refit a fleet member with a random loadout.
 */
fun FleetMemberAPI.refit(
    shouldUpgrade: Boolean,
    shouldStrip: Boolean,
    factionOverride: FactionAPI? = null,
    averageSMods: Int = 0,
    qualityOverride: Float? = null,
    random: java.util.Random = Misc.random,
    useAllWeapons: Boolean = true,
    removeAfterInflating: Boolean = false,
    pickMode: FactionAPI.ShipPickMode = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL
) {
    val currVariant: ShipVariantAPI =
        game.settings.createEmptyVariant(random.nextLong().toString(), this.hullSpec)
    val cmdr = this.captain
    val faction = factionOverride ?: cmdr.faction

    CoreAutofitPlugin(cmdr).apply {
        this.setChecked(CoreAutofitPlugin.UPGRADE, shouldUpgrade)
        this.setChecked(CoreAutofitPlugin.STRIP, shouldStrip)
        this.random = random
    }
        .doFit(currVariant, this.variant, averageSMods,
            DefaultFleetInflater(DefaultFleetInflaterParams().apply {
                this.allWeapons = useAllWeapons
                this.averageSMods = averageSMods
                this.factionId = faction?.id
                this.persistent = !removeAfterInflating
                this.quality = qualityOverride ?: faction?.doctrine?.shipQuality?.toFloat() ?: 1f
                this.mode = pickMode
                this.seed = random.nextLong()
            })
                .apply {
                    inflate(this@refit.fleetData.fleet)
                    this@refit.setVariant(currVariant, removeAfterInflating, removeAfterInflating)
                })
}

fun SettingsAPI.getMergedJSONForMod(paths: List<String>, masterMod: String): JSONObject =
    paths
        .mapNotNull { path ->
            kotlin.runCatching { game.settings.getMergedJSONForMod(path, masterMod) }
                .onFailure { game.logger.e(it) }
                .getOrNull()
        }
        .reduce { obj1, obj2 -> obj2.deepMerge(obj1) }

/**
 * Merge "source" into "target". If fields have equal name, merge them recursively.
 * source: https://stackoverflow.com/a/15070484/1622788
 *
 * @return the merged object (target).
 */

fun JSONObject.deepMerge(target: JSONObject): JSONObject {
    val source = this

    for (key in JSONObject.getNames(source)) {
        val value = source[key]
        if (!target.has(key)) {
            // new value for "key":
            target.put(key, value)
        } else {
            // existing value for "key" - recursively deep merge:
            if (value is JSONObject) {
                value.deepMerge(target.getJSONObject(key))
            } else if (value is JSONArray && target.optJSONArray(key) != null) {
                value.forEach<JSONObject> { target.getJSONArray(key).put(it) }
            } else {
                target.put(key, value)
            }
        }
    }

    return target
}

/**
 * Swaps two fleets' ships and captains. Does not swap other things eg. cargo.
 */
fun CampaignFleetAPI.swapFleets(otherFleet: CampaignFleetAPI) {
    val leftFleet = this
    val originalLeftFleetShips = leftFleet.fleetData.membersListCopy
    val originalRightFleetShips = otherFleet.fleetData.membersListCopy

    // Move left to right.
    originalLeftFleetShips
        .forEach { ship ->
            leftFleet.fleetData.removeFleetMember(ship)
            otherFleet.fleetData.addFleetMember(ship)
        }

    // Move right to left.
    originalRightFleetShips.forEach { ship ->
        otherFleet.fleetData.removeFleetMember(ship)
        leftFleet.fleetData.addFleetMember(ship)
    }

    // Set fleet flagships based upon the original flagships.
    originalLeftFleetShips.firstOrNull { it.isFlagship }?.run { otherFleet.fleetData.setFlagship(this) }
    originalRightFleetShips.firstOrNull { it.isFlagship }?.run { leftFleet.fleetData.setFlagship(this) }

    // If one fleet is player, set them as captain of the flagship.
    if (leftFleet.isPlayerFleet) {
        leftFleet.flagship?.captain = game.sector.playerPerson
    } else if (otherFleet.isPlayerFleet) {
        otherFleet.flagship?.captain = game.sector.playerPerson
    }
}

fun ShipAPI.say(
    text: String,
    textColor: Color = Misc.getTextColor(),
    prependShipNameInCorner: Boolean,
    shipTextColor: Color = this.fleetMember?.captain?.faction?.baseUIColor
        ?: textColor
) {
    val ship = this
    game.combatEngine?.combatUI?.addMessage(
        1,
        ship,
        *(if (prependShipNameInCorner) {
            arrayOf(
                shipTextColor,
                "${ship.name} (${ship.hullSpec.hullNameWithDashClass})",
                Misc.getTextColor(),
                ":"
            )
        } else emptyArray()),
        textColor,
        text
    )

    game.combatEngine?.addFloatingText(
        /* loc = */ Vector2f(ship.location.x, ship.location.y + 100),
        /* text = */ text,
        /* size = */ 40f,
        /* color = */ textColor,
        /* attachedTo = */ ship,
        /* flashFrequency = */ 1f,
        /* flashDuration = */ 0f
    )
}

/**
 * Time how long it takes to run [func] and run [onFinished] afterwards.
 */
inline fun <T> trace(onFinished: (result: T, millis: Long) -> Unit, func: () -> T): T {
    var result: T
    val millis = measureTimeMillis { result = func() }
    onFinished(result, millis)
    return result
}

/**
 * Time how long it takes to run [func].
 */
inline fun <T>
        trace(func: () -> T): T =
    trace(
        onFinished = { result, ms -> game.logger.d { "Took ${ms}ms to produce ${if (result != null) result!!::class.simpleName else "null"}." } },
        func = func
    )

/**
 * Returns a color based on whether the specified stage has been completed.
 * @param isCompleted A function that tests whether the current stage is complete or not
 * @param defaultColor The color to return if the stage is not complete
 * @param completeColor The color to return if the stage is complete
 */
fun textColorOrElseGrayIf(
    defaultColor: Color = Misc.getTextColor(),
    completeColor: Color = Misc.getGrayColor(),
    isCompleted: () -> Boolean
): Color =
    if (isCompleted())
        completeColor
    else
        defaultColor

fun Color.modify(red: Int = this.red, green: Int = this.green, blue: Int = this.blue, alpha: Int = this.alpha) =
    Color(red, green, blue, alpha)

fun <T> eatBugs(func: () -> T) = runCatching { func() }.getOrNull()

fun IntervalUtil(intervalSecs: Float) = IntervalUtil(intervalSecs, intervalSecs)

/**
 * Goes through all constellations, starting with the specified one, and tries to find somewhere to fit the system in.
 * Does not generate jump points, call `StarSystemAPI.autogenerateHyperspaceJumpPoints()` when done.
 */
fun StarSystemAPI.placeInSector(
    preferUnvisited: Boolean = true,
    startAtHyperspaceLocation: Vector2f = game.sector.playerFleet.locationInHyperspace
): Boolean {
    val newSystem = this

    // Find a constellation to add it to
    val constellations =
        game.sector.getConstellations()
            .prefer { if (preferUnvisited) it.systems.all { system -> system.lastPlayerVisitTimestamp == 0L } else true }
            .sortedByDescending { it.location.distanceFrom(startAtHyperspaceLocation) }


    for (constellation in constellations) {
        val xCoords = constellation.systems.mapNotNull { it.location.x }
        val yCoords = constellation.systems.mapNotNull { it.location.y }
        val xRange = (xCoords.minOrNull()!! - 500)..(xCoords.maxOrNull()!! + 500)
        val yRange = (yCoords.minOrNull()!! - 500)..(yCoords.maxOrNull()!! + 500)

        // Try 15000 points per constellation
        for (i in 0..15000) {
            val point = Vector2f(xRange.random(), yRange.random())


            val doesPointIntersectWithAnySystems = constellation.systems.any { system ->
                doCirclesIntersect(
                    centerA = point,
                    radiusA = newSystem.maxRadiusInHyperspace,
                    centerB = system.location,
                    radiusB = system.maxRadiusInHyperspace
                )
            }

            if (!doesPointIntersectWithAnySystems) {
                newSystem.constellation = constellation
                constellation.systems.add(newSystem)
                newSystem.location.set(point)
                game.logger.i { "System ${newSystem.baseName} added to the ${constellation.name} constellation." }
                return true
            }
        }
    }
    return false
}

fun VisualPanelAPI.showPeople(
    people: List<PersonAPI>,
    hidePreviousPeople: Boolean = true,
    withRelationshipBar: Boolean = true
) {
    if (hidePreviousPeople) {
        if (people.isEmpty())
            this.hideFirstPerson()
        if (people.size < 2)
            this.hideSecondPerson()
        if (people.size < 3)
            this.hideThirdPerson()
    }

    people.forEachIndexed { index, person ->
        when (index) {
            0 -> this.showPersonInfo(person, true, withRelationshipBar)
            1 -> this.showSecondPerson(person)
            2 -> this.showThirdPerson(person)
        }
    }
}

fun OptionPanelAPI.addOption(
    text: String,
    data: Any?,
    color: Color? = null,
    tooltip: String? = null
) {
    if (color != null && tooltip != null)
        this.addOption(text, data, color, tooltip)
    else if (color == null && tooltip != null)
        this.addOption(text, data, tooltip)
    else if (color != null)
        this.addOption(text, data, color, null)
    else
        this.addOption(text, data)
}