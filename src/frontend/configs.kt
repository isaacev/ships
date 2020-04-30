package frontend

import backend.inputs.ButtonCode
import backend.inputs.KeyCode
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.*

object Configs {
    object Controls {
        const val CAMERA_PAN_UP: KeyCode = GLFW_KEY_W
        const val CAMERA_PAN_DOWN: KeyCode = GLFW_KEY_S
        const val CAMERA_PAN_LEFT: KeyCode = GLFW_KEY_A
        const val CAMERA_PAN_RIGHT: KeyCode = GLFW_KEY_D
        const val CAMERA_ZOOM_IN: KeyCode = GLFW_KEY_E
        const val CAMERA_ZOOM_OUT: KeyCode = GLFW_KEY_Q
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
