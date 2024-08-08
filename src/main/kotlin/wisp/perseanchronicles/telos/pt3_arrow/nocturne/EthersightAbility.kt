package wisp.perseanchronicles.telos.pt3_arrow.nocturne

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.graphics.SpriteAPI
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.impl.campaign.terrain.BaseTiledTerrain
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CampaignEngine
import com.fs.starfarer.campaign.CampaignTerrain
import com.fs.starfarer.campaign.CustomCampaignEntity
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.ext.plus
import org.lazywizard.lazylib.ext.rotate
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import wisp.perseanchronicles.common.fx.CampaignCustomRenderer
import wisp.perseanchronicles.common.fx.CustomRenderer
import wisp.perseanchronicles.game
import wisp.perseanchronicles.telos.pt3_arrow.MenriSystemCreator
import wisp.perseanchronicles.telos.pt3_arrow.SmoothScrollPlayerCampaignZoomScript
import wisp.questgiver.wispLib.distanceFromPlayerInHyperspace
import wisp.questgiver.wispLib.equalsAny
import wisp.questgiver.wispLib.modify
import wisp.questgiver.wispLib.random
import java.awt.Color
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class EthersightAbility : BaseToggleAbility() {
    companion object {
        const val BOOSTED_MAX_ZOOM = 10f
    }

    @Transient
    private val HYPERSPACE_RANGE = 20000f

    @Transient
    protected var phaseAngle = 0f

    @Transient
    var customRenderer: CampaignCustomRenderer? = null

    @Transient
    var visionEntity: CustomCampaignEntityAPI? = null

    @Transient
    private var objDensityInternal: IntervalUtil? = IntervalUtil(0.03f, 0.04f)

    @Transient
    private var nebulaDensityInterval: IntervalUtil? = IntervalUtil(0.03f, 0.04f)

    @Transient
    private var nocturneEntity: SectorEntityToken? = null

    @Transient
    private var lastLoadedSector: WeakReference<SectorAPI>? = null

    override fun runWhilePaused() = true
    override fun getActiveLayers(): EnumSet<CampaignEngineLayers?>? = EnumSet.of(CampaignEngineLayers.ABOVE)

    override fun advance(amount: Float) {
        super.advance(amount)

        if (!turnedOn) {
            // If player has the ability on, then loads a save where they don't, the max zoom will be wrong, so reset it.
            // Fixed by 0.97a
            if (game.sector.campaignUI.maxZoomFactor != SmoothScrollPlayerCampaignZoomScript.getVanillaMaxZoom()
                && game.sector != lastLoadedSector?.get()
                && game.settings.gameVersion != "0.96a-RC10"
            ) {
                game.sector.campaignUI.maxZoomFactor = SmoothScrollPlayerCampaignZoomScript.getVanillaMaxZoom()
                lastLoadedSector = WeakReference(game.sector)
            }

            return
        }

        if (!isUsable) {
            deactivate()
            return
        }

        val days = Global.getSector().clock.convertToDays(if (game.sector.isPaused) 0f else amount)
        phaseAngle += days * 360f * 10f
        phaseAngle = Misc.normalizeAngle(phaseAngle)

        if (visionEntity == null || visionEntity?.containingLocation != Global.getSector().playerFleet.containingLocation) {
            visionEntity = Global.getSector().playerFleet.containingLocation.addCustomEntity(
                "perseanchronicles_ethersight", "Devmode is on. Tell your friends about Persean Chronicles today!",
                "PerseanChronicles_CustomRenderer_Nebula", Factions.INDEPENDENT, this
            )
            visionEntity?.setFixedLocation(-100000f, -100000f)
            visionEntity?.radius = 5f
            customRenderer = visionEntity?.customPlugin as? CampaignCustomRenderer
        }

        if (!game.sector.isPaused) {
            if (objDensityInternal == null)
                objDensityInternal = IntervalUtil(0.03f, 0.04f)
            objDensityInternal?.advance(amount)

            if (nebulaDensityInterval == null)
                nebulaDensityInterval = IntervalUtil(0.03f, 0.04f)
            nebulaDensityInterval?.advance(amount)
        }
    }

    override fun activateImpl() {
        CampaignEngine.getInstance().uiData.campaignZoom = 5f
    }

    /**
     * Will be called once when level is 0 and consistently when level >0.
     * @param level
     */
    override fun applyEffect(amount: Float, level: Float) {
        if (turnedOn) {
            setMaxZoom(BOOSTED_MAX_ZOOM)
            setDimmedScreen(true)
        }
    }

    override fun deactivateImpl() {
        turnedOn = false
        setDimmedScreen(false)
        customRenderer?.clear()

        // Reset max zoom to vanilla value over time.
        val vanillaMaxZoomLocal = SmoothScrollPlayerCampaignZoomScript.getVanillaMaxZoom()

        if (CampaignEngine.getInstance().uiData.campaignZoom > vanillaMaxZoomLocal) {
            game.sector.addTransientScript(
                SmoothScrollPlayerCampaignZoomScript(
                    endingZoom = vanillaMaxZoomLocal,
                    endingMaxZoom = vanillaMaxZoomLocal
                )
            )
        } else {
            setMaxZoom(vanillaMaxZoomLocal)
        }
    }

    private fun setMaxZoom(zoomMult: Float) {
        game.sector.campaignUI.maxZoomFactor = zoomMult
    }

    override fun cleanupImpl() {
        setDimmedScreen(false)
        customRenderer?.clear()
    }

    override fun hasTooltip() = true
    override fun isTooltipExpandable() = false
    override fun isUsable() = game.sector?.playerFleet?.isInHyperspace == false && !isSystemHidden()

    override fun createTooltip(tooltip: TooltipMakerAPI?, expanded: Boolean) {
        var status = " (off)"
        if (turnedOn) {
            status = " (on)"
        }

        val systemHidden = isSystemHidden()
        val title = tooltip!!.addTitle(spec.name + status)
        title.highlightLast(status)
        title.setHighlightColor(Misc.getGrayColor())

        val pad = 3f
        val opad = 10f

        tooltip.addPara(
            "By focusing, you are able to observe stellar objects and fleets from afar.", opad
        )
//        tooltip.addPara("Hostile fleets are red.", pad, Color.RED, "red")

        // Ethersense
        if (game.sector.currentLocation.isHyperspace) {
            return
        }

        val entities = if (systemHidden) emptyList() else getEntitiesInSystem()
        tooltip.addSpacer(pad)

        if (entities.isEmpty()) {
            if (systemHidden)
                tooltip.addPara(
                    "This system is void of information. You feel somehow more blind than before you first tasted Ether.",
                    Misc.getNegativeHighlightColor(),
                    pad
                )
            else {
                tooltip.addPara(
                    "This system has no objects of note.",
                    Misc.getTextColor(),
                    pad
                )
            }
        } else {
//            tooltip.addPara(
//                "You sense entities within the sector, each one a pattern, and something within you catalogues them all.", opad
//            )

            tooltip.setBulletedListMode("  - ")
            for ((name, group) in entities
                .filter { it !is CampaignFleetAPI }
                .groupBy { token ->
                    if (systemHidden || token.hasTag(Tags.THEME_HIDDEN)) "<unknown pattern>"
                    else
                        (token as? CustomCampaignEntity)?.spec?.defaultName
                            ?: token.name
                }
                .entries
                .sortedWith(compareBy<Map.Entry<String, List<SectorEntityToken>>> { it.value.first() is CampaignFleetAPI }
                    .thenBy { it.value.first().fullName }
                )) {
                tooltip.addPara(
                    "%s",
                    pad,
                    Misc.getTextColor(), //group.first().indicatorColor,
                    name + if (group.size > 1) " x${group.size}" else ""
                )
            }

            val fleetsCount = entities.count { it is CampaignFleetAPI }
            if (fleetsCount > 0) {
                tooltip.addPara(
                    "Fleet x${fleetsCount}",
                    pad,
                )
            }
            tooltip.setBulletedListMode(null)
        }
    }

    /**
     * Disable Ethersight in systems with the "hidden" tag (except for Menri).
     */
    private fun isSystemHidden() =
        game.sector.playerFleet.starSystem?.hasTag(Tags.THEME_HIDDEN) ?: false
                && game.sector.playerFleet.starSystem.baseName != MenriSystemCreator.systemBaseName

    override fun render(layer: CampaignEngineLayers, viewport: ViewportAPI) {
        if (!turnedOn) return

        val fleet = game.sector.playerFleet
        // Changed my mind, it shouldn't work in hyperspace.
        if (fleet.isInHyperspace) return
        val ignoreIds = setOf("PerseanChronicles_CustomRenderer_Nebula")

        if (fleet.isInHyperspace) {
            // Hyperspace
            renderUsingClouds(getHyperspaceObjects(ignoreIds), viewport)
        } else {
            // In-system
            renderUsingClouds(getSystemObjects(viewport, ignoreIds), viewport)

            // Nebulae (don't limit by distance)
            renderUsingClouds(getNebulaObjects(), viewport)
        }
    }

    private fun getNebulaObjects() =
        game.sector.currentLocation.terrainCopy
            .filter { it.type == Terrain.NEBULA }

    private fun getSystemObjects(
        viewport: ViewportAPI,
        ignoreIds: Set<String>
    ) =
        game.sector.currentLocation.allEntities
            .asSequence()
            .filter { isObjectVisible(it, viewport) }
            .filterNot { obj ->
                (obj is CampaignTerrain && obj.type.equalsAny(Terrain.RADIO_CHATTER, Terrain.ASTEROID_FIELD))
                        || obj.tags.any { it.equalsAny(Tags.ORBITAL_JUNK) }
                        || ignoreIds.contains(obj.customEntityType)
                        || (obj is CampaignTerrain && obj.type == Terrain.NEBULA)
                //                            || (obj is CustomCampaignEntity && obj.sprite == null) // This decides whether things like derelicts are shown.
            }
            .toList()

    private fun getHyperspaceObjects(
        ignoreIds: Set<String>
    ) =
        game.sector.currentLocation.fleets
            .asSequence()
            .filter { it.locationInHyperspace.distanceFromPlayerInHyperspace < HYPERSPACE_RANGE }
            .filterNot { obj -> ignoreIds.contains(obj.customEntityType) }
            .toList()

    private fun isObjectVisible(obj: SectorEntityToken, viewport: ViewportAPI) =
        when (obj) {
            // From [RingBand.render]
            is RingBandAPI -> viewport.isNearViewport(obj.location, obj.radius + obj.middleRadius + obj.bandWidthInEngine)
            else -> viewport.isNearViewport(obj.location, obj.radius + 500)
        }

    private fun renderUsingClouds(objs: List<SectorEntityToken>, view: ViewportAPI) {
        if (game.sector.isPaused) return

        val velocityScale = 0f
        val sizeScale = 1f
        val durationScale = 0.8f
        val rampUpScale = 1.0f
        val endSizeScale = 1.55f
        val densityScale = 0.08f // Lower is more dense
        val vel = Vector2f(100f * velocityScale, 100f * velocityScale)
            .rotate(Random.nextFloat() * 360f)
        objs.forEach { obj ->
            val radius = getRingRadiusForCloudRendering(obj)
            val size = getSizeForCloudRendering(obj)
            when {
                // Nebula clouds
                obj is CampaignTerrainAPI && obj.type == Terrain.NEBULA -> {
                    // Don't add every frame, helps control density/performance.
                    val nebulaDensityIntervalRef = nebulaDensityInterval
                    if (nebulaDensityIntervalRef?.intervalElapsed() != true)
                        return@forEach

                    val nebulaDensityScale = 0.90f // Lower is more dense
                    if (nebulaDensityIntervalRef.minInterval != nebulaDensityScale) {
                        nebulaDensityIntervalRef.setInterval(nebulaDensityScale, nebulaDensityScale * 1.2f)
                    }


                    getNebulaeCoords(obj, view)
                        .forEach { point ->
                            customRenderer?.addNebula(
                                location = point,
                                anchorLocation = Vector2f(0f, 0f),
                                velocity = vel,
                                size = size * sizeScale,
                                endSizeMult = endSizeScale,
                                duration = (1.2f..1.5f).random() * durationScale * 6,
                                inFraction = 0.1f * rampUpScale,
                                outFraction = 0.5f,
                                color = getEntityColor(obj),
                                type = CustomRenderer.NebulaType.NORMAL,
                                negative = false
                            )
                        }
                }

                else -> {
                    // Don't add every frame, helps control density/performance.
                    // For non-nebula objects, use the objDensityInternal interval.
                    val objDensityInternalRef = objDensityInternal
                    if (objDensityInternalRef?.intervalElapsed() != true) return

                    if (objDensityInternalRef.minInterval != densityScale) {
                        objDensityInternalRef.setInterval(densityScale, densityScale * 1.2f)
                    }

                    when {

                        // Rings, belts, any circles
                        obj is RingBandAPI || (obj is CampaignTerrainAPI && obj.type.equalsAny(
                            Terrain.ASTEROID_BELT,
                            Terrain.RING
                        )) -> {

                            customRenderer?.addNebula(
                                location = MathUtils.getRandomPointOnCircumference(obj.location, radius),
                                anchorLocation = Vector2f(0f, 0f),
                                velocity = vel,
                                size = size * sizeScale,
                                endSizeMult = endSizeScale,
                                duration = (1.2f..1.5f).random() * durationScale,
                                inFraction = 0.1f * rampUpScale,
                                outFraction = 0.5f,
                                color = getEntityColor(obj),
                                type = CustomRenderer.NebulaType.NORMAL,
                                negative = false
                            )
                        }

                        else -> {
                            customRenderer?.addNebula(
                                location = MathUtils.getRandomPointInCircle(obj.location, radius / 1.5f),
                                anchorLocation = Vector2f(0f, 0f),
                                velocity = vel,
                                size = size * sizeScale,
                                endSizeMult = endSizeScale,
                                duration = (1.2f..1.5f).random() * durationScale,
                                inFraction = 0.1f * rampUpScale,
                                outFraction = 0.5f,
                                color = getEntityColor(obj),
                                type = CustomRenderer.NebulaType.NORMAL,
                                negative = false
                            )
                        }

                    }
                }
            }
        }
    }

    private fun getRingRadiusForCloudRendering(obj: SectorEntityToken): Float {
        return ((obj as? CampaignTerrainAPI)?.plugin as? StarCoronaTerrainPlugin)?.ringParams?.bandWidthInEngine
            ?: ((obj as? CampaignTerrain)?.plugin as? BaseRingTerrain)?.ringParams?.middleRadius
            ?: (obj as? RingBandAPI)?.middleRadius
            ?: if (obj is CampaignTerrainAPI && obj.plugin is BaseTiledTerrain) obj.radius + 40f else null
                ?: (obj.radius + 25f)
    }

    private fun getSizeForCloudRendering(obj: SectorEntityToken): Float {
        return (obj as? RingBandAPI)?.bandWidthInEngine
            ?: ((obj as? CampaignTerrain)?.plugin as? BaseRingTerrain)?.ringParams?.bandWidthInEngine
            ?: getRingRadiusForCloudRendering(obj)
    }


    private fun setDimmedScreen(showEffect: Boolean) {
        val nocturneTagAndId = "PerseanChronicles_Telos_Nocturne"

        if (showEffect) {
            // If effect is ongoing and there's no nocturne entity, create one.
            if (nocturneEntity == null) {
                kotlin.runCatching {
                    nocturneEntity = game.sector.playerFleet.starSystem?.addCustomEntity(
                        nocturneTagAndId,
                        "",
                        nocturneTagAndId,
                        null
                    )
                        ?.apply { addTag(nocturneTagAndId) }
                }
                    .onFailure { game.logger.w(it) }
            }

            // Move it to the player's system or hyperspace if player has moved.
            if (nocturneEntity?.containingLocation != game.sector.playerFleet.starSystem) {
                nocturneEntity?.containingLocation?.removeEntity(nocturneEntity)
                game.sector.playerFleet.starSystem?.addEntity(nocturneEntity)
            }

            // Then move it to where they're looking.
            nocturneEntity?.setLocation(game.sector.viewport.center.x, game.sector.viewport.center.y)
        } else {
            // Remove any existing nocturne entities from the sector.
            game.sector.getCustomEntitiesWithTag(nocturneTagAndId)
                .forEach {
                    it.containingLocation?.removeEntity(it)
                    it.containingLocation = null
                }
            nocturneEntity = null
        }
    }

    private fun getEntityColor(obj: SectorEntityToken): Color = when (obj) {
        is PlanetAPI -> obj.spec.iconColor
        is CampaignFleetAPI -> when {
            obj.isHostileTo(game.sector.playerFleet) -> Color.RED
            else -> obj.faction.color
        }

        is JumpPointAPI -> Color(128, 100, 255) // ideally would get color from the sprite for recolor mods.
        is CampaignTerrainAPI ->
            when (obj.plugin.terrainId) {
                Terrain.CORONA -> obj.plugin.nameColor.modify(alpha = 15)
                Terrain.NEBULA -> obj.plugin.nameColor.modify(alpha = 120)
                else -> obj.plugin.nameColor.modify(alpha = 25)
            }

        is CustomCampaignEntity -> obj.sprite?.color?.modify(alpha = 25) ?: Color.RED.modify(alpha = 0)
        is RingBandAPI -> obj.color.modify(alpha = 25)
        else ->
            obj.indicatorColor?.modify(alpha = 15) ?: Color.GRAY
    }

    /**
     * From [BaseTiledTerrain].
     */
    private fun getNebulaeCoords(obj: CampaignTerrainAPI, v: ViewportAPI): List<Vector2f> {
        val plugin = obj.plugin as BaseTiledTerrain

        // BaseTiledTerrain.render
        var x = 0f //entity.location.x
        var y = 0f //entity.location.y
        val size: Float = plugin.tileSize
        val renderSize: Float = plugin.tileRenderSize

        val tiles = plugin.tiles
        val w: Float = tiles.size * size
        val h: Float = tiles[0].size * size
        x -= w / 2f
        y -= h / 2f
        val extraViewportMarginToRender = (renderSize - size) / 2f + 400f

        val llx: Float = v.llx
        val lly: Float = v.lly
        val vw: Float = v.visibleWidth
        val vh: Float = v.visibleHeight

        if (llx > x + w + extraViewportMarginToRender) return emptyList()
        if (lly > y + h + extraViewportMarginToRender) return emptyList()
        if (llx + vw + extraViewportMarginToRender < x) return emptyList()
        if (lly + vh + extraViewportMarginToRender < y) return emptyList()

        var xStart = ((llx - x - extraViewportMarginToRender) / size).toInt()
        if (xStart < 0) xStart = 0
        var yStart = ((lly - y - extraViewportMarginToRender) / size).toInt()
        if (yStart < 0) yStart = 0

        var xEnd = (((llx + vw - x + extraViewportMarginToRender) / size).toInt() + 1)
        if (xEnd >= tiles.size) xEnd = (tiles.size - 1)
        var yEnd = (((lly + vw - y + extraViewportMarginToRender) / size).toInt() + 1)
        if (yEnd >= tiles.size) yEnd = (tiles[0].size - 1)


        // BaseTiledTerrain.renderSubArea
        val newX = 0f //entity.location.x
        val newY = 0f //entity.location.y
        val startColumn = xStart
        val endColumn = xEnd
        val startRow = yStart
        val endRow = yEnd
        val ret = mutableListOf<Vector2f>()

        for (i in (startColumn..endColumn)) {
            if (i < 0 || i >= tiles.size) continue
            for (j in (startRow..endRow)) {
                if (j < 0 || j >= tiles[0].size) continue
                val texIndex = tiles[i][j]

                if (texIndex >= 0) {
                    val offRange = renderSize * 0.25f
                    val rand = Random((i + j * tiles.size).toLong() * 1000000)
                    val xOff: Float = -offRange / 2f + offRange * rand.nextFloat()
                    val yOff: Float = -offRange / 2f + offRange * rand.nextFloat()
                    val botLeftPoint = Vector2f(
                        newX + xOff - w / 2f + i * size + size / 2f - renderSize / 2f,
                        newY + yOff - h / 2f + j * size + size / 2f - renderSize / 2f
                    )
                    val center = botLeftPoint + Vector2f(renderSize / 2f, renderSize / 2f)
                    ret.add(
//                        CampaignUtils.toScreenCoordinates(
                        MathUtils.getRandomPointInCircle(
                            center,
                            renderSize / 2f
                        )
//                    )
                    )
                }
            }
        }

        return ret
    }

    private fun getEntitiesInSystem(): List<SectorEntityToken> {
        val fleet = fleet
        val location = fleet.containingLocation
        if (fleet.isInHyperspace) return emptyList()
        val result = mutableSetOf<SectorEntityToken>()

        val tags = listOf("neutrino", "neutrino_low", "station")

        val neutrinoSources = location.getEntities(CustomCampaignEntityAPI::class.java)
            .filterIsInstance<SectorEntityToken>()
            .filter { entity -> entity.tags.any { it in tags } }
        result.addAll(neutrinoSources)

        val fleets = location.fleets
            .filterNot { sysFleet -> sysFleet.isPlayerFleet || sysFleet.isStationMode || sysFleet.isHidden || sysFleet.isEmpty }
        result.addAll(fleets)

        return result.toList()
    }

    // <editor-fold desc="Deprecated neutrino detector-like graphics">

    @Deprecated("Using clouds now.")
    private fun renderWithOpengl(objs: List<SectorEntityToken>, viewport: ViewportAPI) {
        if (!this.isActive) return

        val level: Float = .524f//1f
        val alphaMult = level

        val bandWidthInTexture = 256f // vanilla 256
        var bandIndex: Float
//        val radStart = getRingRadius(obj)
//        val radEnd = radStart + 75f
        val radStart = 0f
        val startRad = Math.toRadians(0.0).toFloat()
        val endRad = Math.toRadians(360.0).toFloat()
        val spanRad = abs(endRad - startRad)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        if (texture == null) texture = Global.getSettings().getSprite("wisp_perseanchronicles_telos", "television")
        texture!!.bindTexture()
        GL11.glEnable(GL11.GL_BLEND)

        objs.forEach { obj ->
            val spikiness = getSpikiness(obj)
            val radius = getRingRadius(obj)
            val radEnd = radius * 2 + 75f
            val circ = (Math.PI * 2f * (radStart + radEnd) / 2f).toFloat()
            // magic number that fixes all performance problems, thank you tomatopaste
            val segmentAdjustment = when {
                radius < 0f -> 8f
                spikiness < 8f -> 30f
                spikiness < 10f -> 60f
                else -> 80f
            }
            val pixelsPerSegment = circ / segmentAdjustment
            val segments = (circ / pixelsPerSegment).toInt().toFloat()

            val anglePerSegment = spanRad / segments
            val loc: Vector2f = obj.location
            val x = loc.x
            val y = loc.y
            GL11.glPushMatrix()
//            GL11.glTranslatef(x, y, 0f)

            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

            val thickness = (radEnd - radStart) //* (1 + 1 / radStart)
            var texProgress = 0f
            val texHeight = texture!!.textureHeight
            val imageHeight = texture!!.height
            var texPerSegment = pixelsPerSegment * texHeight / imageHeight * bandWidthInTexture / thickness
            val totalTex = (texPerSegment * segments).toInt().toFloat().coerceAtLeast(1f)
            texPerSegment = totalTex / segments
            val texWidth = texture!!.textureWidth //* 3
            val imageWidth = texture!!.width
            val color = getEntityColor(obj)

            repeat(times = 2) { iter ->
                if (iter == 0) {
                    bandIndex = 1f
                } else {
                    bandIndex = 0f
                    texProgress = segments / 2f * texPerSegment
                }

                if (iter == 1) {
                    GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
                }

                val leftTX = bandIndex * texWidth * bandWidthInTexture / imageWidth
                val rightTX = (bandIndex + 1f) * texWidth * bandWidthInTexture / imageWidth - 0.001f
                GL11.glBegin(GL11.GL_QUAD_STRIP)

                repeat(times = segments.toInt() + 1) { i ->
                    val segIndex = i % segments.toInt()

                    val phaseAngleRad: Float = if (iter == 0) {
                        Math.toRadians(phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 29f)
                    } else {
                        Math.toRadians(-phaseAngle.toDouble()).toFloat() + (segIndex * anglePerSegment * 17f)
                    }

                    val pulseSin = sin(phaseAngleRad.toDouble()).toFloat()
                    val pulseMax = spikiness

                    val pulseAmount = pulseSin * pulseMax
                    val pulseInner = pulseAmount * 0.1f

                    val theta = anglePerSegment * segIndex
                    val cos = cos(theta.toDouble()).toFloat()
                    val sin = sin(theta.toDouble()).toFloat()
                    val rInner = radStart - pulseInner

                    var rOuter = radStart + thickness - pulseAmount

                    // Adds the spikes
                    // var grav = GraviticScanData(graviticScanAbility).apply { advance(.5f) }.getDataAt(angle)  //data.getDataAt(angle)
                    var grav = getSpikeSize(obj).coerceAtMost(750f)
                    grav *= 250f / 750f
                    grav *= level
                    rOuter += grav

                    var alpha = alphaMult
                    alpha *= 0.25f + (grav / 100).coerceAtMost(0.75f)

                    val x1 = cos * rInner
                    val y1 = sin * rInner
                    var x2 = cos * rOuter
                    var y2 = sin * rOuter
                    x2 += (cos(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                    y2 += (sin(phaseAngleRad.toDouble()) * pixelsPerSegment * 0.33f).toFloat()
                    GL11.glColor4ub(
                        color.red.toByte(),
                        color.green.toByte(),
                        color.blue.toByte(),
                        Byte.MAX_VALUE
                    )
                    GL11.glTexCoord2f(leftTX, texProgress)
                    GL11.glVertex2f(x1 + x, y1 + y)
                    GL11.glTexCoord2f(rightTX, texProgress)
                    GL11.glVertex2f(x2 + x, y2 + y)
                    texProgress += texPerSegment
                }

                GL11.glEnd()
            }
//            GL11.glPopMatrix()
        }
    }

    @Deprecated("Use getRingRadiusForCloudRendering instead")
    private fun getRingRadius(obj: SectorEntityToken): Float {
        return obj.radius - 25f
        //return obj.getRadius() + 25f;
    }

    @Transient
    @Deprecated("Using clouds now.")
    private var texture: SpriteAPI? = null

    @Deprecated("Using clouds now.")
    private fun getSpikiness(obj: SectorEntityToken) =
        when (obj) {
            game.sector.playerFleet -> 0f
            is CampaignFleetAPI -> 8f
            is JumpPointAPI -> 3f
            else -> .5f
        }

    @Deprecated("Using clouds now.")
    private fun getSpikeSize(obj: SectorEntityToken): Float =
        when (obj) {
            game.sector.playerFleet -> 0f
            is CampaignFleetAPI -> (Misc.getDangerLevel(obj) / 5f) * 750f
            else -> 0f
        }

    // </editor-fold>
}