package frontend.game

import backend.engine.Duration
import backend.engine.GameLike
import backend.graphics.FocusedOrthographicCamera
import backend.graphics.WorldRenderer
import backend.inputs.DiscreteKey
import backend.inputs.Mouse
import backend.window.Window
import frontend.Configs
import frontend.game.hexagons.HexDirection
import frontend.game.hexagons.TileGrid

class Game : GameLike {
    private var camera = FocusedOrthographicCamera(HexDirection.BottomLeft)
    private var tiles: TileGrid? = null
    private var renderer = WorldRenderer()

    private var camPanLeft = DiscreteKey(Configs.Controls.CAMERA_PAN_LEFT)
    private var camPanRight = DiscreteKey(Configs.Controls.CAMERA_PAN_RIGHT)

    override fun load() {
        tiles = TileGrid(5)
    }

    override fun input(window: Window, mouse: Mouse) {
        camPanLeft.update(window)
        camPanRight.update(window)
    }

    override fun updateState(mouse: Mouse) {
        if (camPanLeft.use()) {
            camera.panLeft()
        }

        if (camPanRight.use()) {
            camera.panRight()
        }
    }

    override fun updateAnimation(delta: Duration) {
        camera.nextFrame(delta)
    }

    override fun render(window: Window) {
        renderer.render(window, camera, tiles)
    }

    override fun free() {
        tiles?.free()
    }
}
