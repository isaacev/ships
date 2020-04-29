package frontend.game.hexagons

import backend.memory.Managed
import backend.memory.Mesh
import backend.memory.ModelCatalog
import backend.memory.Shader
import backend.memory.ShaderCatalog
import backend.memory.SimpleEntity
import backend.memory.SimpleModel
import backend.memory.Texture
import backend.memory.TextureCatalog
import org.joml.Vector3f

const val TILE_SIZE = 1f

class Tile(val coord: HexCubeCoord, model: SimpleModel) : SimpleEntity(model) {
    override fun setShaderUniforms(shader: Shader) {
        shader.setUniform("tileTexture", 0)
    }
}

class TileGrid(gridSize: Int) : Managed {
    private val models = ModelCatalog()
    private val textures = TextureCatalog()
    private val shaders = ShaderCatalog()
    private val tiles: MutableMap<HexCubeCoord, Tile> = HashMap()

    init {
        val uniforms =
            listOf("modelViewMatrix", "projectionMatrix", "tileTexture", "lightDirection", "lightColor", "lightBias")
        val texture = textures.getOrLoad("textures/tiles.png")
        val shader = shaders.getOrLoad("tile", uniforms)
        val model = makeTileModel(texture, shader, models)
        for (x in -gridSize..+gridSize) {
            for (y in -gridSize..+gridSize) {
                val z = 0 - x - y
                if (z >= -gridSize && z <= +gridSize) {
                    val coord = HexCubeCoord(x, y, z)
                    val tile = Tile(coord, model)
                    tiles[coord] = tile
                    val cartesianCoord = coord.toCartesian(TILE_SIZE)
                    tile.updateEntity(x = cartesianCoord.x, y = cartesianCoord.y)
                }
            }
        }
    }

    fun forEach(cb: (Tile) -> Unit) {
        tiles.forEach { (_, tile) -> cb(tile) }
    }

    override fun free() {
        val totalModels = models.size
        textures.free()
        shaders.free()
        models.free()
        println("free $totalModels terrain models")
    }
}

private fun makeTileModel(texture: Texture, shader: Shader, models: ModelCatalog): SimpleModel {
    val builder = Mesh.Builder()

    val corA = flatHexCorner(0f, 0f, TILE_SIZE, 0)
    val corB = flatHexCorner(0f, 0f, TILE_SIZE, 1)
    val corC = flatHexCorner(0f, 0f, TILE_SIZE, 2)
    val corD = flatHexCorner(0f, 0f, TILE_SIZE, 3)
    val corE = flatHexCorner(0f, 0f, TILE_SIZE, 4)
    val corF = flatHexCorner(0f, 0f, TILE_SIZE, 5)

    val vecA = Vector3f(corA.x, 0f, corA.y)
    val vecB = Vector3f(corB.x, 0f, corB.y)
    val vecC = Vector3f(corC.x, 0f, corC.y)
    val vecD = Vector3f(corD.x, 0f, corD.y)
    val vecE = Vector3f(corE.x, 0f, corE.y)
    val vecF = Vector3f(corF.x, 0f, corF.y)

    val normal = Mesh.Builder.normal(vecA, vecC, vecB)

    val texCenterX = 1f / 8f
    val texCenterY = 1f / 8f
    val texScalar = .9f / 8f

    val texA = flatHexCorner(texCenterX, texCenterY, texScalar, 0)
    val texB = flatHexCorner(texCenterX, texCenterY, texScalar, 1)
    val texC = flatHexCorner(texCenterX, texCenterY, texScalar, 2)
    val texD = flatHexCorner(texCenterX, texCenterY, texScalar, 3)
    val texE = flatHexCorner(texCenterX, texCenterY, texScalar, 4)
    val texF = flatHexCorner(texCenterX, texCenterY, texScalar, 5)

    val idxA = builder.addVertex(vecA, normal, texA)
    val idxB = builder.addVertex(vecB, normal, texB)
    val idxC = builder.addVertex(vecC, normal, texC)
    val idxD = builder.addVertex(vecD, normal, texD)
    val idxE = builder.addVertex(vecE, normal, texE)
    val idxF = builder.addVertex(vecF, normal, texF)

    builder.addPolygon(idxA, idxC, idxB)
    builder.addPolygon(idxA, idxD, idxC)
    builder.addPolygon(idxA, idxE, idxD)
    builder.addPolygon(idxA, idxF, idxE)

    return models.upload("hexagon", SimpleModel(texture, shader, builder.toMesh()))
}
