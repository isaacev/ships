package frontend.game.effects

import backend.Units
import backend.UnitsPerSecond
import backend.animation.Easing
import backend.engine.Duration
import backend.engine.Milliseconds
import backend.graphics.Transform
import backend.memory.Mesh
import backend.memory.Shader
import backend.memory.SimpleTexture
import backend.memory.Texture
import backend.particles.ParticleEmitter
import frontend.Configs
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.ARBVertexArrayObject
import org.lwjgl.opengl.GL11
import kotlin.math.max
import kotlin.math.min

private const val START_PARTICLE_SCALE = .3f
private const val MID_PARTICLE_SCALE = .3f
private const val END_PARTICLE_SCALE = .6f

private const val MIN_PARTICLE_ALPHA = 0f
private const val MAX_PARTICLE_ALPHA = .7f

private const val MAX_PARTICLE_AGE: Milliseconds = 4000f
private const val PARTICLE_MIDPOINT: Milliseconds = 100f
private const val SPACE_BETWEEN_PARTICLES: Units = .05f

private class SmokeParticle(val pos: Vector3f, var rot: Float, var scale: Float) {
    var age: Milliseconds = 0f
    var alpha: Float = MIN_PARTICLE_ALPHA
    val tint: Vector3f = Vector3f(1f)
    val rand = Math.random()
        .toFloat()
    val randScale = (rand / 2f) + .75f
    val randRot = rand - .5f

    fun update(elapsed: Milliseconds) {
        age += elapsed
        rot += randRot
        scale = randScale * if (age < PARTICLE_MIDPOINT) {
            START_PARTICLE_SCALE
        } else {
            Easing.OutQuad.calc(
                age, MAX_PARTICLE_AGE, START_PARTICLE_SCALE, END_PARTICLE_SCALE
            )
        }
        alpha = if (age < PARTICLE_MIDPOINT) {
            Easing.InOutQuad.calc(age, PARTICLE_MIDPOINT, MIN_PARTICLE_ALPHA, MAX_PARTICLE_ALPHA)
        } else {
            Easing.InOutQuad.calc(
                age - PARTICLE_MIDPOINT, MAX_PARTICLE_AGE - PARTICLE_MIDPOINT, MAX_PARTICLE_ALPHA, MIN_PARTICLE_ALPHA
            )
        }
        tint.set(Easing.InOutQuad.calc(age, MAX_PARTICLE_AGE, .7f, 1f))
    }
}

private fun buildParticleMesh(): Mesh {
    val builder = Mesh.Builder()

    val half = 1f / 2f;
    val vecA = Vector3f(-half, +half, 0f)
    val vecB = Vector3f(+half, +half, 0f)
    val vecC = Vector3f(-half, -half, 0f)
    val vecD = Vector3f(+half, -half, 0f)

    val norm = Mesh.Builder.normal(vecA, vecC, vecD)

    val texA = Vector2f(0f, 1f)
    val texB = Vector2f(1f, 1f)
    val texC = Vector2f(0f, 0f)
    val texD = Vector2f(1f, 0f)

    val idxA = builder.addVertex(vecA, norm, texA)
    val idxB = builder.addVertex(vecB, norm, texB)
    val idxC = builder.addVertex(vecC, norm, texC)
    val idxD = builder.addVertex(vecD, norm, texD)

    builder.addPolygon(idxA, idxC, idxB)
    builder.addPolygon(idxB, idxC, idxD)

    return builder.toMesh()
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

class CannonballFactory : ParticleEmitter {
    // Particle emitter data and assets
    private val shader = Shader("explosion", shaderUniforms)
    private val texture: Texture = SimpleTexture("textures/explosion.png")
    private val mesh: Mesh = buildParticleMesh()
    private val emitters: MutableList<CannonballParticleEmitter> = ArrayList()

    fun spawn(start: Vector3f, finish: Vector3f, speed: UnitsPerSecond) {
        emitters.add(CannonballParticleEmitter(shader, texture, mesh, start, finish, speed))
    }

    override fun update() {
        emitters.removeAll { it.update(); it.isFinished() }
    }

    override fun nextFrame(delta: Duration) {
        emitters.forEach { it.nextFrame(delta.fresh()) }
    }

    override fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f) {
        emitters.forEach { it.render(transform, projectionMatrix, viewMatrix) }
    }

    override fun free() {
        shader.free()
        texture.free()
        mesh.free()
        emitters.forEach { it.free() }
    }
}

private class CannonballParticleEmitter(
    val shader: Shader, val texture: Texture, val mesh: Mesh, start: Vector3f, finish: Vector3f, speed: UnitsPerSecond
) : ParticleEmitter {
    // Alive particles
    private val particles: MutableList<SmokeParticle> = ArrayList()

    // State data
    private val trajectory = Vector3f()
    private val distance: Units
    private val spawnOffset = Vector3f()
    private val nextSpawnLocation = Vector3f(start)
    private var emitterAge: Milliseconds = 0f
    private var totalSpawned = 0
    private val totalToBeSpawned: Int
    private val duration: Milliseconds
    private val durationBetweenSpawns: Milliseconds

    init {
        finish.sub(start, trajectory)
        trajectory.normalize(SPACE_BETWEEN_PARTICLES, spawnOffset)
        distance = trajectory.length()
        totalToBeSpawned = max(1, (distance / SPACE_BETWEEN_PARTICLES).toInt())

        assert(distance != 0f)
        duration = if (speed == 0f) {
            0f
        } else {
            (1000f * distance) / speed
        }
        durationBetweenSpawns = duration / totalToBeSpawned
    }

    override fun update() {
        particles.removeAll { it.age > MAX_PARTICLE_AGE }
    }

    override fun nextFrame(delta: Duration) {
        val elapsed = delta.requestAll()
        emitterAge += elapsed

        val expectedSpawns = min(max(1, (emitterAge / durationBetweenSpawns).toInt()), totalToBeSpawned)
        while (totalSpawned < expectedSpawns) {
            val pos = nextSpawnLocation
            nextSpawnLocation.add(spawnOffset)
            val rot = Math.random()
                .toFloat() * 360f
            particles.add(SmokeParticle(Vector3f(pos), rot, START_PARTICLE_SCALE))
            totalSpawned++
        }

        particles.forEach { it.update(elapsed) }
    }

    override fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f) {
        shader.bind()
        texture.bind()

        // Set common uniforms
        shader.setUniform("projectionMatrix", projectionMatrix)
            .setUniform("lightDirection", Configs.Lighting.DIRECTION)
            .setUniform("lightColor", Configs.Lighting.COLOR)
            .setUniform("lightBias", Configs.Lighting.BIAS)
            .setUniform("explosionTexture", 0)

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
                .setUniform("particleAlpha", particle.alpha)
                .setUniform("particleTint", particle.tint)

            // Draw the mesh
            ARBVertexArrayObject.glBindVertexArray(mesh.vaoId)
            GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.vertexCount, GL11.GL_UNSIGNED_INT, 0)
        }

        texture.unbind()
        shader.unbind()
    }

    fun isFinished(): Boolean {
        return (totalSpawned >= totalToBeSpawned && particles.isEmpty())
    }

    override fun free() {
        shader.free()
        texture.free()
        mesh.free()
    }
}
