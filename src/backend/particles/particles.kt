package backend.particles

import backend.animation.Animated
import backend.graphics.Transform
import backend.memory.Managed
import org.joml.Matrix4f

interface ParticleEmitter : Managed, Animated {
    fun update()

    fun render(transform: Transform, projectionMatrix: Matrix4f, viewMatrix: Matrix4f)
}
