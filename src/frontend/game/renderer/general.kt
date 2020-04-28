package frontend.game.renderer

import backend.window.Window
import org.lwjgl.opengl.GL11.*

fun startRenderingFrame(window: Window) {
    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
    glViewport(0, 0, window.getPixelWidth(), window.getPixelHeight())
    window.updateProjectionMatrix()
}
