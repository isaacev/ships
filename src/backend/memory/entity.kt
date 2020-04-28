package backend.memory

import backend.Degrees
import backend.Units
import org.joml.Vector3f

object ConversionConstants {
    const val X_SCALAR: Units = 1f
    const val X_OFFSET: Units = 0f

    const val Z_SCALAR: Units = 1f
    const val Z_OFFSET: Units = 0f
}

/**
 * Entities are an abstraction that represents a unique renderable thing
 * in the game world. Entities have a reference to a Model. The combination
 * of the Model data with the Entity's position and rotation data makes it
 * possible for an Entity to be rendered to the screen.
 *
 * Multiple entities may have references to the same Model. For this reason,
 * Entities ARE NOT responsible for the destruction of their Model. Model
 * destruction will ALWAYS be handled by higher-level systems.
 */
abstract class Entity<Key>(val model: Model<Key>) {
    val pos: Vector3f = Vector3f(0f, 0f, 0f)
    val rot: Vector3f = Vector3f(0f, 0f, 0f)

    fun updateEntity(x: Units = pos.x, y: Units = pos.z, yaw: Degrees = rot.y) {
        pos.x = (x * ConversionConstants.X_SCALAR) + ConversionConstants.X_OFFSET
        pos.z = (y * ConversionConstants.Z_SCALAR) + ConversionConstants.Z_OFFSET
        rot.y = yaw
    }

    open fun setShaderUniforms(shader: Shader) {
        shader.setUniform("textureSampler", 0)
    }

    abstract fun getMeshPositionOffset(key: Key): Vector3f

    abstract fun getMeshRotationOffset(key: Key): Vector3f

    abstract fun getMeshScale(key: Key): Float
}
