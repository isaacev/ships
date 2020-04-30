package backend.graphics

import backend.Degrees
import backend.Units
import backend.animation.Animated
import backend.animation.Easing
import backend.engine.Duration
import backend.engine.Milliseconds
import backend.inputs.Mouse
import backend.window.Window
import frontend.game.hexagons.HexDirection
import org.joml.Intersectionf
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

interface Camera {
    val position: Vector3f
    val rotation: Vector3f
    val normal: Vector3f
    val viewMatrix: Matrix4f
    val fieldOfView: Degrees
    val zNear: Units
    val zFar: Units
}

class OrbitalCamera(viewingAngle: HexDirection) : Camera, Animated {
    companion object Constants {
        const val LATITUDE: Degrees = 45f
        const val RADIUS: Units = 20f
        const val CUTOFF: Degrees = 1f
        const val DURATION: Milliseconds = 200f
    }

    private var direction = viewingAngle
    private var angle = viewingAngle.toDegrees() - 90f // why add 90f? who tf knows
    private val focus = Vector3f()

    override val fieldOfView: Degrees = 60f
    override val zNear: Units = 0.001f
    override val zFar: Units = 1_000f

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

    // FIXME: cache this
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
        finishAngle = newDirection.toDegrees() - 90f // why add 90f? who tf knows

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

    fun mouseRayGroundPlaneIntersection(window: Window, mouse: Mouse, projectionMatrix: Matrix4f): Vector2f? {
        val x: Float = (2f * mouse.x) / window.getScreenWidth().toFloat() - 1f
        val y: Float = 1f - (2f * mouse.y) / window.getScreenHeight().toFloat()
        val z: Float = -1f

        val inverseProjectionMatrix = Matrix4f()
        inverseProjectionMatrix.set(projectionMatrix)
        inverseProjectionMatrix.invert()

        val vec = Vector4f()
        vec.set(x, y, z, 1f)
        vec.mul(inverseProjectionMatrix)
        vec.z = -1f
        vec.w = 0f

        val inverseViewMatrix = Matrix4f()
        inverseViewMatrix.set(viewMatrix)
        inverseViewMatrix.invert()
        vec.mul(inverseViewMatrix)

        val rayOrigin = position
        val rayDirection = Vector3f(vec.x, vec.y, vec.z)
        val planePoint = Vector3f(0f, 0f, 0f)
        val planeNormal = Vector3f(0f, 1f, 0f)
        val epsilon = 0.0001f // Used to catch divide by zero errors if ray parallel to plane

        val t = Intersectionf.intersectRayPlane(rayOrigin, rayDirection, planePoint, planeNormal, epsilon)

        return if (t == -1f) {
            // Ray didn't intersect the plane
            null
        } else {
            // Use the formula: `p(t) = rayOrigin + t * rayDirection` to compute the intersection point coordinates
            val point = rayOrigin.add(rayDirection.mul(t))
            Vector2f(point.x, point.z)
        }
    }
}
