package frontend

import backend.inputs.KeyCode
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_KEY_E
import org.lwjgl.glfw.GLFW.GLFW_KEY_Q

object Configs {
    object Controls {
        const val CAMERA_PAN_LEFT: KeyCode = GLFW_KEY_Q
        const val CAMERA_PAN_RIGHT: KeyCode = GLFW_KEY_E
    }

    object Lighting {
        private const val BIAS_AMBIENT = .3f
        private const val BIAS_DIFFUSE = .8f

        val DIRECTION = Vector3f(.3f, -1f, .5f)
        val COLOR = Vector3f(1f, 1f, 1f)
        val BIAS = Vector2f(BIAS_AMBIENT, BIAS_DIFFUSE)
    }
}
