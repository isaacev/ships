package backend.particles

import backend.Degrees
import backend.animation.Animated
import backend.engine.Milliseconds
import backend.graphics.Transform
import backend.memory.Managed
import org.joml.Matrix4f
import org.joml.Vector3f

interface Particle {
    val pos: Vector3f
    val rot: Degrees
    val scale: Float
    val alpha: Float
    val age: Milliseconds

    fun update(elapsed: Milliseconds): Boolean
}

interface ParticleEmitter : Managed, Animated {
    fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f)
}
