package frontend.game

import backend.engine.Duration
import backend.engine.GameLike
import backend.inputs.Mouse
import backend.window.Window
import frontend.game.renderer.startRenderingFrame

class Game: GameLike {
    override fun load() {
        // TODO("Not yet implemented")
    }

    override fun input(window: Window, mouse: Mouse) {
        // TODO("Not yet implemented")
    }

    override fun updateState(mouse: Mouse) {
        // TODO("Not yet implemented")
    }

    override fun updateAnimation(delta: Duration) {
        // TODO("Not yet implemented")
    }

    override fun render(window: Window) {
        startRenderingFrame(window)

        // TODO: do other rendering
    }

    override fun free() {
        // TODO("Not yet implemented")
    }
}
