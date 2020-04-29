package backend.graphics

import backend.Degrees
import backend.Units
import backend.animation.Animated
import backend.animation.Easing
import backend.engine.Duration
import backend.engine.Milliseconds
import frontend.game.hexagons.HexDirection
import org.joml.Matrix4f
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

interface Camera {
    val position: Vector3f
    val rotation: Vector3f
    val normal: Vector3f
    val viewMatrix: Matrix4f
}

class FocusedOrthographicCamera(viewingAngle: HexDirection) : Camera, Animated {
    companion object Constants {
        const val LATITUDE: Degrees = 45f
        const val RADIUS: Units = 10f
        const val CUTOFF: Degrees = 1f
        const val DURATION: Milliseconds = 200f
    }

    private var direction = viewingAngle
    private var angle = viewingAngle.toDegrees()
    private val focus = Vector3f()

    // Animation variables
    private var isMoving = false
    private var startAngle = angle
    private var finishAngle = angle
    private var progress: Milliseconds = 0f

    override val position: Vector3f
        get() {
            val x = RADIUS * sin(Math.toRadians(LATITUDE.toDouble())) * cos(Math.toRadians(angle.toDouble()))
            val y = RADIUS * cos(Math.toRadians(LATITUDE.toDouble()))
            val z = RADIUS * sin(Math.toRadians(LATITUDE.toDouble())) * sin(Math.toRadians(angle.toDouble()))
            return Vector3f(x.toFloat() + focus.x, y.toFloat() + focus.y, z.toFloat() + focus.z)
        }

    override val normal: Vector3f
        get() {
            val x = RADIUS * sin(Math.toRadians(LATITUDE.toDouble())) * cos(Math.toRadians(angle.toDouble()))
            val y = RADIUS * cos(Math.toRadians(LATITUDE.toDouble()))
            val z = RADIUS * sin(Math.toRadians(LATITUDE.toDouble())) * sin(Math.toRadians(angle.toDouble()))
            return Vector3f(-x.toFloat(), -y.toFloat(), -z.toFloat())
        }

    override val rotation: Vector3f
        get() {
            val lat = -LATITUDE + 90f
            val lon = (angle + 270f) % 360
            return Vector3f(lat, lon, 0f)
        }

    override val viewMatrix: Matrix4f
        get() {
            val pos = position
            val rot = rotation
            val mat = Matrix4f().identity()

            // Apply the rotation...
            mat.rotate(Math.toRadians(rot.x.toDouble()).toFloat(), Vector3f(1f, 0f, 0f))
                .rotate(Math.toRadians(rot.y.toDouble()).toFloat(), Vector3f(0f, 1f, 0f))

            // ...then apply the translation
            mat.translate(-pos.x, -pos.y, -pos.z)

            return mat
        }

    override fun nextFrame(delta: Duration) {
        if (!isMoving) {
            return
        }

        progress += delta.request(DURATION - progress)
        angle = Easing.InOutQuad.calc(progress, DURATION, startAngle, finishAngle)

        if (abs(finishAngle - angle) < CUTOFF) {
            angle = finishAngle % 360
            isMoving = false
        }
    }

    fun panLeft() {
        val newDirection = when (direction) {
            HexDirection.Top         -> HexDirection.TopRight
            HexDirection.TopRight    -> HexDirection.BottomRight
            HexDirection.BottomRight -> HexDirection.Bottom
            HexDirection.Bottom      -> HexDirection.BottomLeft
            HexDirection.BottomLeft  -> HexDirection.TopLeft
            HexDirection.TopLeft     -> HexDirection.Top
        }
        moveTo(newDirection)
    }

    fun panRight() {
        val newDirection = when (direction) {
            HexDirection.Top         -> HexDirection.TopLeft
            HexDirection.TopRight    -> HexDirection.Top
            HexDirection.BottomRight -> HexDirection.TopRight
            HexDirection.Bottom      -> HexDirection.BottomRight
            HexDirection.BottomLeft  -> HexDirection.Bottom
            HexDirection.TopLeft     -> HexDirection.BottomLeft
        }
        moveTo(newDirection)
    }

    fun reset(newDirection: HexDirection) {
        moveTo(newDirection)
    }

    private fun moveTo(newDirection: HexDirection) {
        if (isMoving) {
            // Ignore because the camera is currently in motion
            return
        } else if (direction == newDirection) {
            // Ignore because the new heading is the same as the current heading
            return
        }

        direction = newDirection
        isMoving = true
        startAngle = angle
        finishAngle = newDirection.toDegrees() // why add 90f? who tf knows

        // Make sure the rotation uses the shortest arc. For example, when moving
        // from 315deg -> 45deg use the 90deg arc instead of the 270deg arc. In the
        // example, the shorter arc can be forced by using 405deg instead of 45deg.
        if (abs(finishAngle - startAngle) > 180) {
            if (finishAngle < startAngle) {
                finishAngle += 360
            } else {
                startAngle += 360
            }
        }

        progress = 0f
    }
}
