package frontend.game

import backend.color
import backend.engine.Duration
import backend.engine.GameLike
import backend.graphics.OrbitalCamera
import backend.graphics.Renderer
import backend.inputs.DiscreteKey
import backend.inputs.Mouse
import backend.memory.Entity
import backend.window.Window
import frontend.Configs
import frontend.game.agents.ShipFactory
import frontend.game.agents.ShipStyle
import frontend.game.hexagons.HexCubeCoord
import frontend.game.hexagons.HexDirection
import frontend.game.hexagons.TileGrid

class Game : GameLike {
    private var camera = OrbitalCamera(Configs.Camera.DEFAULT_ANGLE)
    private var tiles: TileGrid? = null
    private val shipFactory = ShipFactory()
    private var renderer = Renderer()
    private val entities: MutableList<Entity> = ArrayList()

    private var camPanLeft = DiscreteKey(Configs.Controls.CAMERA_PAN_LEFT)
    private var camPanRight = DiscreteKey(Configs.Controls.CAMERA_PAN_RIGHT)
    private var camReset = DiscreteKey(Configs.Controls.CAMERA_RESET)

    override fun load() {
        tiles = TileGrid(5)
        entities.add(
            shipFactory.newShipAt(
                HexCubeCoord(0, 0, 0),
                HexDirection.BottomRight,
                ShipStyle(hull = color(0x502716), sails = color(0xF0E3C7))
            )
        )
    }

    override fun input(window: Window, mouse: Mouse) {
        camPanLeft.update(window)
        camPanRight.update(window)
        camReset.update(window)
    }

    override fun updateState(mouse: Mouse) {
        if (camReset.use()) {
            camera.reset(Configs.Camera.DEFAULT_ANGLE)
        } else {
            if (camPanLeft.use()) {
                camera.panLeft()
            }

            if (camPanRight.use()) {
                camera.panRight()
            }
        }
    }

    override fun updateAnimation(delta: Duration) {
        camera.nextFrame(delta)
    }

    override fun render(window: Window) {
        renderer.render(window, camera, tiles, entities)
    }

    override fun free() {
        tiles?.free()
        shipFactory.free()
    }
}
