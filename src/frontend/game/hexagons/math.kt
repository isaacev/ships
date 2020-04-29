package frontend.game.hexagons

import backend.Degrees
import org.joml.Vector2f
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
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

fun flatHexCorners(centerX: Float, centerY: Float, size: Float): Array<Vector2f> {
    return arrayOf(
        flatHexCorner(centerX, centerY, size, 0),
        flatHexCorner(centerX, centerY, size, 1),
        flatHexCorner(centerX, centerY, size, 2),
        flatHexCorner(centerX, centerY, size, 3),
        flatHexCorner(centerX, centerY, size, 4),
        flatHexCorner(centerX, centerY, size, 5)
    )
}

fun flatHexWidth(size: Float): Float {
    return 2f * size
}

fun flatHexHeight(size: Float): Float {
    return SQRT_3 * size
}

fun flatHexHorizontalCenterDist(size: Float): Float {
    return flatHexWidth(size) * 0.75f
}

fun flatHexVerticalCenterDist(size: Float): Float {
    return flatHexHeight(size)
}

/**
 * COORDINATE SYSTEMS
 *
 * @link https://www.redblobgames.com/grids/hexagons
 */

enum class HexDirection {
    Top, TopRight, BottomRight, Bottom, BottomLeft, TopLeft;

    fun toDegrees(): Degrees {
        return when (this) {
            Top         -> 0f
            TopRight    -> 60f
            BottomRight -> 120f
            Bottom      -> 180f
            BottomLeft  -> 240f
            TopLeft     -> 300f
        }
    }
}

data class HexCubeCoord(val x: Int, val y: Int, val z: Int) {
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

    fun toCartesian(size: Float): Vector2f {
        val cartX = flatHexHorizontalCenterDist(size) * x.toFloat()
        val cartY = flatHexVerticalCenterDist(size) * ((y - z) / 2f)
        return Vector2f(cartX, cartY)
    }

    override fun toString(): String {
        return "Tile($x $y $z)"
    }
}
