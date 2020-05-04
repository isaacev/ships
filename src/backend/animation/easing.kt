package backend.animation

import kotlin.math.pow

enum class Easing {
    Linear, InQuad, OutQuad, InOutQuad, InQuart, OutQuart, InOutQuart;

    private fun scale(rawT: Float): Float {
        val t = rawT.coerceIn(0f, 1f)
        return when (this) {
            Linear     -> t

            InQuad     -> t * t
            OutQuad    -> t * (2 - t)
            InOutQuad  -> if (t < .5f) {
                2 * t * t
            } else {
                -1 + (4 - 2 * t) * t
            }

            InQuart    -> t * t * t * t
            OutQuart   -> 1 - (t - 1).pow(4)
            InOutQuart -> if (t < .5f) {
                8 * t * t * t * t
            } else {
                1 - 8 * (t - 1).pow(4)
            }
        }
    }

    fun calc(t: Float, duration: Float, start: Float, finish: Float): Float {
        return start + scale(t / duration) * (finish - start)
    }
}
