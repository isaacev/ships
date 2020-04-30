package frontend.game.agents

import backend.Color
import backend.memory.Managed
import backend.memory.ModelCatalog
import backend.memory.Shader
import backend.memory.ShaderCatalog
import backend.memory.SimpleEntity
import backend.memory.SimpleModel
import backend.memory.TextureCatalog
import backend.utils.Loader
import frontend.game.hexagons.HexCubeCoord
import frontend.game.hexagons.HexDirection

data class ShipStyle(val hull: Color, val sails: Color)

class Ship(val coord: HexCubeCoord, val heading: HexDirection, val style: ShipStyle, model: SimpleModel) :
    SimpleEntity(model) {
    init {
        val cartesian = coord.toCartesian()
        updateEntity(x = cartesian.x, y = cartesian.y, yaw = heading.toDegrees())
    }

    override fun setShaderUniforms(shader: Shader) {
        shader.setUniform("shipTexture", 0)
            .setUniform("hullColor", style.hull)
            .setUniform("sailColor", style.sails)
    }
}

class ShipFactory : Managed {
    private val shaders = ShaderCatalog()
    private val textures = TextureCatalog()
    private val models = ModelCatalog()

    fun newShipAt(coord: HexCubeCoord, heading: HexDirection, style: ShipStyle): Ship {
        val model = models.getOrLoad("ship") {
            val uniforms = listOf(
                "modelViewMatrix",
                "projectionMatrix",
                "shipTexture",
                "hullColor",
                "sailColor",
                "lightDirection",
                "lightColor",
                "lightBias"
            )
            val shader = shaders.getOrLoad("ship", uniforms)
            val texture = textures.getOrLoad("textures/registration.png")
            val mesh = Loader.loadMesh("/resources/models/ship.obj")
            SimpleModel(texture, shader, mesh)
        }
        return Ship(coord, heading, style, model)
    }

    override fun free() {
        val totalModels = models.size
        shaders.free()
        textures.free()
        models.free()
        println("free $totalModels ship models")
    }
}
