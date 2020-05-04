package frontend.game.effects

import backend.Degrees
import backend.animation.Easing
import backend.engine.Duration
import backend.engine.Milliseconds
import backend.graphics.Transform
import backend.memory.Mesh
import backend.memory.Shader
import backend.memory.SimpleTexture
import backend.memory.Texture
import backend.particles.Particle
import backend.particles.ParticleEmitter
import frontend.Configs
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL11

private const val FIREBALL_START_SCALE = .1f

private class FireballParticle(override val pos: Vector3f, val size: Float) : Particle {
    override val alpha = 1f
    override var age: Milliseconds = 0f
    override var scale: Float = FIREBALL_START_SCALE
    override val rot: Degrees = (Math.random() * 360f).toFloat()

    override fun update(elapsed: Milliseconds): Boolean {
        age += elapsed

        val grow: Milliseconds = 300f
        val shrink: Milliseconds = 1_000f

        val growStart = .1f
        val holdStart = 1f * size
        val shrinkEnd = .2f

        scale = when {
            age < grow -> Easing.OutQuart.calc(age, grow, growStart, holdStart)
            else       -> Easing.InQuart.calc(age - grow, shrink, holdStart, shrinkEnd)
        }

        return age > grow + shrink
    }
}

private val shaderUniforms = listOf(
    "modelViewMatrix",
    "projectionMatrix",
    "explosionTexture",
    "lightDirection",
    "lightColor",
    "lightBias",
    "particleAlpha",
    "particleTint"
)

class FireballFactory : ParticleEmitter {
    private val shader = Shader("fireball", shaderUniforms)
    private val fireball: Texture = SimpleTexture("textures/fireball.png")
    private val mesh: Mesh = buildUnitQuad()

    private val fireballs: MutableList<FireballParticle> = ArrayList()

    fun spawn(at: Vector3f, size: Float) {
        fireballs.add(FireballParticle(at, size))
    }

    override fun nextFrame(delta: Duration) {
        val elapsed = delta.requestAll()
        fireballs.removeAll { it.update(elapsed) }
    }

    override fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f) {
        GL11.glDepthMask(false)
        GL11.glEnable(GL11.GL_BLEND)

        shader.bind()
        fireball.bind()

        // Set common uniforms
        shader.setUniform("projectionMatrix", projectionMatrix)
            .setUniform("lightDirection", Configs.Lighting.DIRECTION)
            .setUniform("lightColor", Configs.Lighting.COLOR)
            .setUniform("lightBias", Configs.Lighting.BIAS)
            .setUniform("explosionTexture", 0)

        // Render each particle individually
        val copyOfViewMatrix = Matrix4f(viewMatrix)
        fireballs.forEach { particle ->
            val modelMatrix = transform.buildModelMatrix(particle.pos, particle.scale)
            val rotZ = Math.toRadians(particle.rot.toDouble())
            copyOfViewMatrix.set(viewMatrix)
                .transpose3x3(modelMatrix)
                .scale(particle.scale)
                .rotateZ(rotZ.toFloat())
            val modelViewMatrix = transform.getModelViewMatrix(modelMatrix, copyOfViewMatrix)
                .scale(particle.scale)

            shader.setUniform("modelViewMatrix", modelViewMatrix)
                .setUniform("particleAlpha", particle.alpha)
                .setUniform("particleTint", Vector3f(1f))

            // Draw the mesh
            ARBVertexArrayObject.glBindVertexArray(mesh.vaoId)
            GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
        }

        fireball.unbind()
        shader.unbind()

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDepthMask(true)
    }

    override fun free() {
        shader.free()
        fireball.free()
        mesh.free()
    }
}
