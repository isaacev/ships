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

abstract class Entity {
    val pos: Vector3f = Vector3f(0f, 0f, 0f)
    val rot: Vector3f = Vector3f(0f, 0f, 0f)
    abstract val shader: Shader
    abstract val texture: Texture

    abstract fun meshes(cb: (pos: Vector3f, rot: Vector3f, scale: Float, mesh: Mesh) -> Unit)

    fun updateEntity(x: Units = pos.x, y: Units = pos.z, yaw: Degrees = rot.y) {
        pos.x = (x * ConversionConstants.X_SCALAR) + ConversionConstants.X_OFFSET
        pos.z = (y * ConversionConstants.Z_SCALAR) + ConversionConstants.Z_OFFSET
        rot.y = yaw
    }

    abstract fun setShaderUniforms(shader: Shader)
}

abstract class SimpleEntity(private val model: SimpleModel) : Entity() {
    override val shader: Shader
        get() = model.shader

    override val texture: Texture
        get() = model.texture

    override fun meshes(cb: (pos: Vector3f, rot: Vector3f, scale: Float, mesh: Mesh) -> Unit) {
        cb(pos, rot, 1f, model.mesh)
    }
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
abstract class ComplexEntity<Key>(private val model: ComplexModel<Key>) : Entity() {
    override val shader: Shader
        get() = model.shader

    override val texture: Texture
        get() = model.texture

    abstract fun getMeshPositionOffset(key: Key): Vector3f

    abstract fun getMeshRotationOffset(key: Key): Vector3f

    abstract fun getMeshScale(key: Key): Float

    override fun meshes(cb: (pos: Vector3f, rot: Vector3f, scale: Float, mesh: Mesh) -> Unit) {
        model.meshes.forEach { (key, mesh) ->
            cb(getMeshPositionOffset(key), getMeshRotationOffset(key), getMeshScale(key), mesh)
        }
    }
}
