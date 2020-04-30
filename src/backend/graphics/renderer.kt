package backend.graphics

import backend.Degrees
import backend.Units
import backend.memory.Entity
import backend.window.ScreenPixels
import backend.window.Window
import frontend.Configs
import frontend.game.hexagons.TileGrid
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30.glBindVertexArray

class Renderer {
    private val transform = Transform()

    private fun renderEntity(projectionMatrix: Matrix4f, viewMatrix: Matrix4f, entity: Entity) {
        val shader = entity.shader
        shader.bind()

        // Set globals uniforms
        shader.setUniform("projectionMatrix", projectionMatrix).setUniform("lightDirection", Configs.Lighting.DIRECTION)
            .setUniform("lightColor", Configs.Lighting.COLOR).setUniform("lightBias", Configs.Lighting.BIAS)

        // Defer to entity for custom uniforms
        entity.setShaderUniforms(shader)
        entity.meshes { pos, rot, scale, mesh ->
            val modelViewMatrix = transform.getModelViewMatrix(rot, pos, scale, viewMatrix)
            shader.setUniform("modelViewMatrix", modelViewMatrix)

            // Draw the mesh
            entity.texture.bind()
            glBindVertexArray(mesh.vaoId)
            glDrawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0)
            entity.texture.unbind()
        }

        shader.unbind()
    }

    // TODO: remove TileGrid from method signature
    fun render(window: Window, camera: Camera, tiles: TileGrid?, entities: List<Entity>) {
        // Clear the framebuffer and other preparations for a fresh render
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
        glViewport(0, 0, window.getPixelWidth(), window.getPixelHeight())

        val projectionMatrix2 = transform.getProjectionMatrix(
            camera.fieldOfView, window.getScreenWidth(), window.getScreenHeight(), camera.zNear, camera.zFar
        )

        val viewMatrix = camera.viewMatrix
        tiles?.forEach { renderEntity(projectionMatrix2, viewMatrix, it) }
        entities.forEach { renderEntity(projectionMatrix2, viewMatrix, it) }
    }

    fun getProjectionMatrix(): Matrix4f {
        return transform.projectionMatrix
    }
}

private class Transform {
    private val modelViewMatrix = Matrix4f()
    val projectionMatrix = Matrix4f()

    fun getModelViewMatrix(rot: Vector3f, pos: Vector3f, scale: Float, viewMatrix: Matrix4f): Matrix4f {
        modelViewMatrix.identity().translate(pos).rotateX(Math.toRadians(-rot.x.toDouble()).toFloat())
            .rotateY(Math.toRadians(-rot.y.toDouble()).toFloat()).rotateZ(Math.toRadians(-rot.z.toDouble()).toFloat())
            .scale(scale)

        val copyOfViewMatrix = Matrix4f(viewMatrix)
        return copyOfViewMatrix.mul(modelViewMatrix)
    }

    fun getProjectionMatrix(
        fov: Degrees, width: ScreenPixels, height: ScreenPixels, zNear: Units, zFar: Units
    ): Matrix4f {
        return projectionMatrix.setPerspective(
            Math.toRadians(fov.toDouble()).toFloat(), width.toFloat() / height, zNear, zFar
        )
    }
}
