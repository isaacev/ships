package backend.memory

import backend.Degrees
import backend.Units
import org.joml.Vector3f

object ConversionConstants {
    const val X_SCALAR: Units = 1f
    const val X_OFFSET: Units = 0f

    const val Y_SCALAR: Units = 1f
    const val Y_OFFSET: Units = 0f

    const val Z_SCALAR: Units = 1f
    const val Z_OFFSET: Units = 0f
}

abstract class Entity {
    internal val pos: Vector3f = Vector3f(0f, 0f, 0f)
    internal val rot: Vector3f = Vector3f(0f, 0f, 0f)
    internal var scale: Float = 1f
    internal abstract val shader: Shader
    internal abstract val texture: Texture
    internal open val isBillboard: Boolean = false

    internal abstract fun meshes(cb: (pos: Vector3f, rot: Vector3f, scale: Float, mesh: Mesh) -> Unit)

    internal fun updateEntity(
        x: Units = pos.x, y: Units = pos.y, z: Units = pos.z, yaw: Degrees = rot.y, scale: Float = this.scale
    ) {
        pos.x = (x * ConversionConstants.X_SCALAR) + ConversionConstants.X_OFFSET
        pos.y = (y * ConversionConstants.Y_SCALAR) + ConversionConstants.Y_OFFSET
        pos.z = (z * ConversionConstants.Z_SCALAR) + ConversionConstants.Z_OFFSET
        rot.y = yaw
        this.scale = scale
    }

    internal abstract fun setShaderUniforms(shader: Shader)
}

abstract class SimpleEntity(private val model: SimpleModel) : Entity() {
    override val shader: Shader
        get() = model.shader

    override val texture: Texture
        get() = model.texture

    override fun meshes(cb: (pos: Vector3f, rot: Vector3f, scale: Float, mesh: Mesh) -> Unit) {
        cb(pos, rot, scale, model.mesh)
    }
}

abstract class BillboardEntity(model: SimpleModel) : SimpleEntity(model) {
    override val isBillboard = true
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
