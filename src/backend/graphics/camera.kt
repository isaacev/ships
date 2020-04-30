package backend.graphics

import backend.Degrees
import backend.ToDegrees
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

enum class Pitch : ToDegrees {
    Highest, Middle, Lowest;

    override fun toDegrees(): Degrees {
        return when (this) {
            Highest -> 20f
            Middle  -> 45f
            Lowest  -> 70f
        }
    }
}

private class AnimatedDegree<T : ToDegrees>(initial: T) : Animated {
    private var value: T = initial
    private var angle: Degrees = initial.toDegrees()
    private var isMoving: Boolean = false
    private var startAngle: Degrees = initial.toDegrees()
    private var finishAngle: Degrees = initial.toDegrees()
    private var progress: Milliseconds = 0f

    fun getValue(): T {
        return value
    }

    fun moveTo(newValue: T) {
        if (isMoving) {
            // Ignore because the camera is currently in motion
            return
        } else if (value == newValue) {
            // Ignore because the new heading is the same as the current heading
            return
        }

        value = newValue
        isMoving = true
        startAngle = angle
        finishAngle = newValue.toDegrees()

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

    fun getFloat(): Float {
        return angle
    }

    fun getDouble(): Double {
        return angle.toDouble()
    }

    override fun nextFrame(delta: Duration) {
        if (!isMoving) {
            return
        }

        progress += delta.request(OrbitalCamera.DURATION - progress)
        angle = Easing.InOutQuad.calc(progress, OrbitalCamera.DURATION, startAngle, finishAngle)

        if (abs(finishAngle - angle) < OrbitalCamera.CUTOFF) {
            angle = finishAngle % 360
            isMoving = false
        }
    }
}

class OrbitalCamera(yaw: HexDirection, pitch: Pitch) : Camera, Animated {
    companion object Constants {
        const val RADIUS: Units = 20f
        const val CUTOFF: Degrees = 1f
        const val DURATION: Milliseconds = 200f
    }

    private val originalYaw = yaw
    private val originalPitch = pitch
    private val yaw = AnimatedDegree(yaw)
    private val pitch = AnimatedDegree(pitch)

    // private var direction = yaw
    // private var angle = yaw.toDegrees() - 90f // why add 90f? who tf knows
    private val focus = Vector3f()

    override val fieldOfView: Degrees = 60f
    override val zNear: Units = 0.001f
    override val zFar: Units = 1_000f

    override val position: Vector3f
        get() {
            // Necessary because toRadians uses x-axis as 0deg, we want to use the y-axis as the 0deg instead
            val offsetYaw = yaw.getDouble() - 90.0

            val x = RADIUS * sin(Math.toRadians(pitch.getDouble())) * cos(Math.toRadians(offsetYaw))
            val y = RADIUS * cos(Math.toRadians(pitch.getDouble()))
            val z = RADIUS * sin(Math.toRadians(pitch.getDouble())) * sin(Math.toRadians(offsetYaw))
            return Vector3f(x.toFloat() + focus.x, y.toFloat() + focus.y, z.toFloat() + focus.z)
        }

    override val normal: Vector3f
        get() {
            // Necessary because toRadians uses x-axis as 0deg, we want to use the y-axis as the 0deg instead
            val offsetYaw = yaw.getDouble() - 90.0

            val x = RADIUS * sin(Math.toRadians(pitch.getDouble())) * cos(Math.toRadians(offsetYaw))
            val y = RADIUS * cos(Math.toRadians(pitch.getDouble()))
            val z = RADIUS * sin(Math.toRadians(pitch.getDouble())) * sin(Math.toRadians(offsetYaw))
            return Vector3f(-x.toFloat(), -y.toFloat(), -z.toFloat())
        }

    override val rotation: Vector3f
        get() {
            val lat = -pitch.getFloat() + 90f
            val lon = (yaw.getFloat() + 180f) % 360f
            return Vector3f(lat, lon, 0f)
        }

    // FIXME: cache this
    override val viewMatrix: Matrix4f
        get() {
            val pos = position
            val rot = rotation
            val mat = Matrix4f().identity()

            // Apply the rotation...
            val xRad = Math.toRadians(rot.x.toDouble())
            val yRad = Math.toRadians(rot.y.toDouble())
            mat.rotate(xRad.toFloat(), Vector3f(1f, 0f, 0f))
                .rotate(yRad.toFloat(), Vector3f(0f, 1f, 0f))

            // ...then apply the translation
            mat.translate(-pos.x, -pos.y, -pos.z)

            return mat
        }

    override fun nextFrame(delta: Duration) {
        yaw.nextFrame(delta)
        pitch.nextFrame(delta.fresh())
    }

    fun panUp() {
        val newPitch = when (pitch.getValue()) {
            Pitch.Highest -> Pitch.Highest
            Pitch.Middle  -> Pitch.Highest
            Pitch.Lowest  -> Pitch.Middle
        }
        moveTo(pitch = newPitch)
    }

    fun panDown() {
        val newPitch = when (pitch.getValue()) {
            Pitch.Highest -> Pitch.Middle
            Pitch.Middle  -> Pitch.Lowest
            Pitch.Lowest  -> Pitch.Lowest
        }
        moveTo(pitch = newPitch)
    }

    fun panLeft() {
        val newYaw = when (yaw.getValue()) {
            HexDirection.Top         -> HexDirection.TopRight
            HexDirection.TopRight    -> HexDirection.BottomRight
            HexDirection.BottomRight -> HexDirection.Bottom
            HexDirection.Bottom      -> HexDirection.BottomLeft
            HexDirection.BottomLeft  -> HexDirection.TopLeft
            HexDirection.TopLeft     -> HexDirection.Top
        }
        moveTo(yaw = newYaw)
    }

    fun panRight() {
        val newYaw = when (yaw.getValue()) {
            HexDirection.Top         -> HexDirection.TopLeft
            HexDirection.TopRight    -> HexDirection.Top
            HexDirection.BottomRight -> HexDirection.TopRight
            HexDirection.Bottom      -> HexDirection.BottomRight
            HexDirection.BottomLeft  -> HexDirection.Bottom
            HexDirection.TopLeft     -> HexDirection.BottomLeft
        }
        moveTo(yaw = newYaw)
    }

    fun reset() {
        moveTo(yaw = originalYaw, pitch = originalPitch)
    }

    private fun moveTo(yaw: HexDirection = this.yaw.getValue(), pitch: Pitch = this.pitch.getValue()) {
        this.yaw.moveTo(yaw)
        this.pitch.moveTo(pitch)
    }

    fun mouseRayGroundPlaneIntersection(window: Window, mouse: Mouse, projectionMatrix: Matrix4f): Vector2f? {
        val x: Float = (2f * mouse.x) / window.getScreenWidth() - 1f
        val y: Float = 1f - (2f * mouse.y) / window.getScreenHeight()
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
