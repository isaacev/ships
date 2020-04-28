package backend.inputs

import backend.window.Window
import org.joml.Vector2f
import org.lwjgl.glfw.GLFW

typealias ButtonCode = Int

class Mouse(window: Window) {
    private val prev = Vector2f(-1f, -1f)
    private val curr = Vector2f(0f, 0f)
    private var inWindow = false

    init {
        GLFW.glfwSetCursorPosCallback(window.getHandle()) { _, x, y ->
            curr.x = x.toFloat()
            curr.y = y.toFloat()
        }

        GLFW.glfwSetCursorEnterCallback(window.getHandle()) { _, entered ->
            inWindow = entered
        }
    }

    val x: Float
        get() = curr.x

    val y: Float
        get() = curr.y

    fun getCurrent(): Vector2f {
        return curr
    }

    fun input(window: Window) {
        prev.x = curr.x
        prev.y = curr.y
    }
}

/**
 * The DiscreteClick class is a tiny state-machine that tracks the state of a
 * single mouse button so that an action is only performed when the button is
 * pressed for the first time and not when the button is held down between
 * multiple frames.
 */
class DiscreteClick(private val buttonCode: ButtonCode) {
    private var isBeingPressed = false
    private var isAvailableToUse = false

    fun update(window: Window) {
        if (window.isButtonPressed(buttonCode) && !isBeingPressed) {
            isBeingPressed = true
            isAvailableToUse = true
        } else if (window.isButtonReleased(buttonCode)) {
            isBeingPressed = false
        }
    }

    fun use(): Boolean {
        return if (isAvailableToUse) {
            isAvailableToUse = false
            true
        } else {
            false
        }
    }
}
