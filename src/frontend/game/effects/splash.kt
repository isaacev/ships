package frontend.game.effects

import backend.Degrees
import backend.color
import backend.engine.Duration
import backend.engine.Milliseconds
import backend.graphics.Transform
import backend.memory.Shader
import backend.particles.Particle
import backend.particles.ParticleEmitter
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL11
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

private class SplashParticle(override val pos: Vector3f, val vel: Vector3f, override val scale: Float) : Particle {
    override val alpha = 1f
    override var age: Milliseconds = 0f
    override var rot: Degrees = (Math.random() * 360f).toFloat()
    val color = color(0xB5C8F1)

    override fun update(elapsed: Milliseconds): Boolean {
        val gravity = .01f
        age += elapsed
        rot += 2f
        vel.y -= gravity
        pos.add(vel)

        return pos.y <= -(2 * scale)
    }
}

private val shaderUniforms = listOf(
    "modelViewMatrix", "projectionMatrix", "color"
)

class SplashFactory : ParticleEmitter {
    private val shader = Shader("splash", shaderUniforms)
    private val mesh = buildUnitQuad()
    private val particles: MutableList<SplashParticle> = ArrayList()

    fun spawn(at: Vector3f, average: Int, plusOrMinus: Int = 0, radius: Float = .5f, spout: Boolean = false) {
        assert(average > 0)
        val count = max(1, average + ((Math.random() * plusOrMinus) - plusOrMinus / 2).roundToInt())
        val distribution = 360f / count
        val offset = Math.random() * distribution
        for (i in 0 until count) {
            val lateral = .02f
            val vertical = .1f + (Math.random() / 20f).toFloat()
            val degree = Math.toRadians(offset + (distribution * i).toDouble())
                .toFloat()
            val start = Vector3f(at.x + radius * cos(degree), 0f, at.z + radius * sin(degree))
            val velocity = Vector3f(lateral * cos(degree), vertical, lateral * sin(degree))
            val size = (Math.random() / 5f).toFloat() + .3f
            particles.add(SplashParticle(start, velocity, size))
        }

        if (spout) {
            val height = .2f
            val start = Vector3f(at.x, 0f, at.z)
            val velocity = Vector3f(0f, height, 0f)
            val size = (Math.random() / 5f).toFloat() + .3f
            particles.add(SplashParticle(start, velocity, size))
        }
    }

    override fun nextFrame(delta: Duration) {
        val elapsed = delta.requestAll()
        particles.removeAll { it.update(elapsed) }
    }

    override fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f) {
        shader.bind()

        // Set common uniforms
        shader.setUniform("projectionMatrix", projectionMatrix)

        // Render each particle individually
        val copyOfViewMatrix = Matrix4f(viewMatrix)
        particles.forEach { particle ->
            val modelMatrix = transform.buildModelMatrix(particle.pos, particle.scale)
            val rotZ = Math.toRadians(particle.rot.toDouble())
            copyOfViewMatrix.set(viewMatrix)
                .transpose3x3(modelMatrix)
                .scale(particle.scale)
                .rotateZ(rotZ.toFloat())
            val modelViewMatrix = transform.getModelViewMatrix(modelMatrix, copyOfViewMatrix)
                .scale(particle.scale)

            shader.setUniform("modelViewMatrix", modelViewMatrix)
                .setUniform("color", particle.color)

            // Draw the mesh
            ARBVertexArrayObject.glBindVertexArray(mesh.vaoId)
            GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
        }

        shader.unbind()
    }

    override fun free() {
        shader.free()
        mesh.free()
    }
}
