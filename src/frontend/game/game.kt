package frontend.game

import backend.color
import backend.engine.Duration
import backend.engine.GameLike
import backend.graphics.OrbitalCamera
import backend.graphics.Pitch
import backend.graphics.Renderer
import backend.graphics.Zoom
import backend.inputs.DiscreteClick
import backend.inputs.DiscreteKey
import backend.inputs.Mouse
import backend.window.Window
import frontend.Configs
import frontend.game.agents.Ship
import frontend.game.agents.ShipFactory
import frontend.game.agents.Style
import frontend.game.effects.FireballFactory
import frontend.game.effects.SplashFactory
import frontend.game.hexagons.HexCubeCoord
import frontend.game.hexagons.HexDirection
import frontend.game.hexagons.TileGrid
import frontend.game.hexagons.pointToHex
import org.joml.Vector3f

class Game : GameLike {
    private var camera = OrbitalCamera(HexDirection.Top, Pitch.Middle, Zoom.Close)
    private val shipFactory = ShipFactory()
    private var renderer = Renderer()
    private val entities: MutableList<Ship> = ArrayList()

    private lateinit var tiles: TileGrid
    private lateinit var fireballFactory: FireballFactory
    private lateinit var splashFactory: SplashFactory

    private var camPanUp = DiscreteKey(Configs.Controls.CAMERA_PAN_UP)
    private var camPanDown = DiscreteKey(Configs.Controls.CAMERA_PAN_DOWN)
    private var camPanLeft = DiscreteKey(Configs.Controls.CAMERA_PAN_LEFT)
    private var camPanRight = DiscreteKey(Configs.Controls.CAMERA_PAN_RIGHT)
    private var camZoomIn = DiscreteKey(Configs.Controls.CAMERA_ZOOM_IN)
    private var camZoomOut = DiscreteKey(Configs.Controls.CAMERA_ZOOM_OUT)
    private var camReset = DiscreteKey(Configs.Controls.CAMERA_RESET)
    private var selectTile = DiscreteClick(Configs.Controls.SELECT_TILE)

    private var hoverCoord: HexCubeCoord? = null
    private lateinit var pirateShip: Ship
    private lateinit var imperialShip: Ship

    override fun load() {
        tiles = TileGrid(5)

        val pirateStyle = Style(hull = color(0x502716), sails = color(0xF0E3C7))
        pirateShip = shipFactory.newShipAt(HexCubeCoord(0, 0, 0), HexDirection.Bottom, pirateStyle)
        entities.add(pirateShip)

        val imperialStyle = Style(hull = color(0x273448), sails = color(0xE8D78E))
        imperialShip = shipFactory.newShipAt(HexCubeCoord(-2, 2, 0), HexDirection.BottomLeft, imperialStyle)
        entities.add(imperialShip)

        fireballFactory = FireballFactory()
        splashFactory = SplashFactory()
    }

    override fun input(window: Window, mouse: Mouse) {
        camPanUp.update(window)
        camPanDown.update(window)
        camPanLeft.update(window)
        camPanRight.update(window)
        camZoomIn.update(window)
        camZoomOut.update(window)
        camReset.update(window)
        selectTile.update(window)

        hoverCoord = camera.mouseRayGroundPlaneIntersection(window, mouse, renderer.getProjectionMatrix())
            ?.let { pointToHex(it) }
    }

    override fun updateState(mouse: Mouse) {
        if (camReset.use()) {
            camera.reset()
        } else {
            if (camPanUp.use()) camera.panUp()
            if (camPanDown.use()) camera.panDown()
            if (camPanLeft.use()) camera.panLeft()
            if (camPanRight.use()) camera.panRight()
            if (camZoomIn.use()) camera.zoomIn()
            if (camZoomOut.use()) camera.zoomOut()
        }

        if (selectTile.use() && hoverCoord != null) {
            val fireAtCartesian = hoverCoord!!.toCartesian()
            val x = fireAtCartesian.x
            val z = fireAtCartesian.y
            if (imperialShip.coord == hoverCoord) {
                fireballFactory.spawn(Vector3f(x, .3f, z), 1f)
                splashFactory.spawn(Vector3f(x, 0f, z), 6, plusOrMinus = 2, radius = .3f, spout = true)
            } else {
                splashFactory.spawn(Vector3f(x, 0f, z), 6, plusOrMinus = 2, radius = .2f, spout = true)
            }
        }
    }

    override fun updateAnimation(delta: Duration) {
        camera.nextFrame(delta)
        fireballFactory.nextFrame(delta.fresh())
        splashFactory.nextFrame(delta.fresh())
    }

    override fun render(window: Window) {
        renderer.render(window, camera, tiles, entities, listOf(splashFactory, fireballFactory))
    }

    override fun free() {
        tiles.free()
        shipFactory.free()
        fireballFactory.free()
        splashFactory.free()
    }
}
