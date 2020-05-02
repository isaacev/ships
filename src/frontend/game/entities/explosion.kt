package frontend.game.entities

import backend.memory.BillboardEntity
import backend.memory.Managed
import backend.memory.Mesh
import backend.memory.ModelCatalog
import backend.memory.Shader
import backend.memory.ShaderCatalog
import backend.memory.SimpleModel
import backend.memory.TextureCatalog
import org.joml.Vector2f
import org.joml.Vector3f

class Explosion(model: SimpleModel, center: Vector3f, scale: Float) : BillboardEntity(model) {
    init {
        updateEntity(center.x, center.y, center.z, scale = scale)
    }

    override fun setShaderUniforms(shader: Shader) {
        shader.setUniform("explosionTexture", 0);
    }
}

class ExplosionFactory : Managed {
    private val models = ModelCatalog()
    private val shaders = ShaderCatalog()
    private val textures = TextureCatalog()
    private val entities: MutableList<Explosion> = ArrayList()

    val explosions: List<Explosion>
        get() = entities

    fun spawnAt(scale: Float, center: Vector3f) {
        val model = models.get("explosion") as SimpleModel? ?: models.upload("explosion", buildExplosionModel())
        val explosion = Explosion(model, center, scale)
        entities.add(explosion)
    }

    override fun free() {
        models.free()
        shaders.free()
        textures.free()
    }

    private fun buildExplosionModel(): SimpleModel {
        val explosionUniforms = listOf(
            "modelViewMatrix", "projectionMatrix", "explosionTexture", "lightDirection", "lightColor", "lightBias"
        )
        val explosionShader = shaders.getOrLoad("explosion", explosionUniforms)
        val explosionTexture = textures.getOrLoad("textures/explosion.png")
        val explosionMesh = buildExplosionMesh()
        return SimpleModel(explosionTexture, explosionShader, explosionMesh)
    }

    private fun buildExplosionMesh(): Mesh {
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
}
