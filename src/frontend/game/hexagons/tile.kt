package frontend.game.hexagons

import backend.Color
import backend.color
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

val FALLBACK_TINT = color(0xffffff)

class Tile(val coord: HexCubeCoord, model: SimpleModel) : SimpleEntity(model) {
    var overlay: TileOverlay? = null

    override fun setShaderUniforms(shader: Shader) {
        shader.setUniform("overlayChoice", overlay?.toChoice() ?: -1)
            .setUniform("tileTexture", 0)
            .setUniform("overlayTexture", 1)
            .setUniform("overlayTint", overlay?.tint ?: FALLBACK_TINT)
            .setUniform(
                "overlayRotation", Math.toRadians(
                    overlay?.toRotation()
                        ?.toDouble() ?: 0.0
                )
            )
    }
}

class TileOverlay(private val which: Which, val direction: HexDirection, val tint: Color) {
    enum class Which {
        Circle, Terminus, Straight, Obtuse, Start;
    }

    fun toChoice(): Int {
        return when (which) {
            Which.Circle   -> 0
            Which.Terminus -> 1
            Which.Straight -> 2
            Which.Obtuse   -> 3
            Which.Start    -> 4
        }
    }

    fun toRotation(): Float {
        return direction.toDegrees()
    }
}

class TileGrid(private val gridSize: Int) : Managed {
    private val models = ModelCatalog()
    private val textures = TextureCatalog()
    private val shaders = ShaderCatalog()
    private val tiles: MutableMap<HexCubeCoord, Tile> = HashMap()

    init {
        val uniforms = listOf(
            "modelViewMatrix",
            "projectionMatrix",
            "tileTexture",
            "overlayTexture",
            "overlayRotation",
            "overlayChoice",
            "overlayTint",
            "lightDirection",
            "lightColor",
            "lightBias"
        )
        val texture = textures.getOrLoad("textures/tile_default.png", "textures/overlays.png")
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
                    tile.updateEntity(x = cartesianCoord.x, z = cartesianCoord.y)
                }
            }
        }
    }

    fun forEach(cb: (Tile) -> Unit) {
        tiles.forEach { (_, tile) -> cb(tile) }
    }

    fun getTile(coord: HexCubeCoord): Tile? {
        return tiles[coord]
    }

    private fun isWithinGrid(coord: HexCubeCoord): Boolean {
        return coord.absMaxComponent() <= gridSize
    }

    fun allNavigableNeighbors(coord: HexCubeCoord, blocked: Set<HexCubeCoord>): List<HexCubeCoord> {
        return HexDirection.values()
            .map { coord.neighbor(it) }
            .filter { isWithinGrid(it) && !blocked.contains(it) }
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

    val texCenterX = 1f / 2f
    val texCenterY = 1f / 2f
    val texScalar = .9f / 2f

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
