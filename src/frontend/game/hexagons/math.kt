package frontend.game.hexagons

import backend.Degrees
import backend.ToDegrees
import org.joml.Vector2f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * HEXAGON SIZING
 *
 * Functions for computing the sizes and distance between adjacent hexagons.
 *
 * @link https://www.redblobgames.com/grids/hexagons
 */

private const val SQRT_3: Float = 1.7320508076f

fun flatHexCorner(centerX: Float, centerY: Float, size: Float, corner: Int): Vector2f {
    val normalized = corner % 6
    val angleDeg = 60f * normalized
    val angleRad = PI / 180f * angleDeg
    val x = centerX + size * cos(angleRad.toFloat())
    val y = centerY + size * sin(angleRad.toFloat())
    return Vector2f(x, y)
}

/**
 * COORDINATE SYSTEMS
 *
 * @link https://www.redblobgames.com/grids/hexagons
 */

enum class HexDirection : ToDegrees {
    Top, TopRight, BottomRight, Bottom, BottomLeft, TopLeft;

    override fun toDegrees(): Degrees {
        return when (this) {
            Top         -> 0f
            TopRight    -> 60f
            BottomRight -> 120f
            Bottom      -> 180f
            BottomLeft  -> 240f
            TopLeft     -> 300f
        }
    }

    fun angleTo(other: HexDirection): Degrees {
        val diff = (other.toDegrees() - toDegrees() + 180f) % 360 - 180
        val normDiff = if (diff < -180f) {
            diff + 360
        } else {
            diff
        }
        return abs(normDiff) % 360
    }
}

data class HexCubeCoord(val x: Int, val y: Int, val z: Int) {
    init {
        assert(x + y + z == 0)
    }

    fun neighbor(dir: HexDirection): HexCubeCoord {
        return when (dir) {
            HexDirection.Top         -> HexCubeCoord(x + 0, y + 1, z - 1)
            HexDirection.TopRight    -> HexCubeCoord(x + 1, y + 0, z - 1)
            HexDirection.BottomRight -> HexCubeCoord(x + 1, y - 1, z + 0)
            HexDirection.Bottom      -> HexCubeCoord(x + 0, y - 1, z + 1)
            HexDirection.BottomLeft  -> HexCubeCoord(x - 1, y + 0, z + 1)
            HexDirection.TopLeft     -> HexCubeCoord(x - 1, y + 1, z + 0)
        }
    }

    fun distanceTo(other: HexCubeCoord): Int {
        val dx = abs(x - other.x)
        val dy = abs(y - other.y)
        val dz = abs(z - other.z)
        return (dx + dy + dz) / 2
    }

    fun toCartesian(size: Float = TILE_SIZE): Vector2f {
        val cartX: Float = size * ((3f / 2f) * x)
        val cartY: Float = size * ((SQRT_3 / 2) * x + (SQRT_3 * z))
        return Vector2f(cartX, cartY)
    }

    fun absMaxComponent(): Int {
        return max(abs(x), max(abs(y), abs(z)))
    }

    override fun toString(): String {
        return "Tile($x $y $z)"
    }
}

private fun roundToHex(x: Float, y: Float, z: Float): HexCubeCoord {
    var rx: Int = x.roundToInt()
    var ry: Int = y.roundToInt()
    var rz: Int = z.roundToInt()

    val xDiff = abs(rx - x)
    val yDiff = abs(ry - y)
    val zDiff = abs(rz - z)

    if (xDiff > yDiff && xDiff > zDiff) {
        rx = -ry - rz
    } else if (yDiff > zDiff) {
        ry = -rx - rz
    } else {
        rz = -rx - ry
    }

    return HexCubeCoord(rx, ry, rz)
}

fun pointToHex(point: Vector2f, size: Float = TILE_SIZE): HexCubeCoord {
    val x = ((2f / 3f) * point.x) / size
    val z = ((-1f / 3f) * point.x + (SQRT_3 / 3) * point.y) / size
    val y = -x - z
    return roundToHex(x, y, z)
}
