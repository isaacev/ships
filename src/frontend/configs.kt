package frontend

import backend.inputs.ButtonCode
import backend.inputs.KeyCode
import frontend.game.hexagons.HexDirection
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

object Configs {
    object Camera {
        val DEFAULT_ANGLE: HexDirection = HexDirection.Top
    }

    object Controls {
        const val CAMERA_PAN_LEFT: KeyCode = GLFW_KEY_Q
        const val CAMERA_PAN_RIGHT: KeyCode = GLFW_KEY_E
        const val CAMERA_RESET: KeyCode = GLFW_KEY_R

        const val SELECT_TILE: ButtonCode = GLFW_MOUSE_BUTTON_1
    }

    object Lighting {
        private const val BIAS_AMBIENT = .6f
        private const val BIAS_DIFFUSE = .8f

        val DIRECTION = Vector3f(0f, -1f, 0f)
        val COLOR = Vector3f(1f, 1f, 1f)
        val BIAS = Vector2f(BIAS_AMBIENT, BIAS_DIFFUSE)
    }
}
