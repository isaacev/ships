package backend.graphics

import backend.Degrees
import backend.ToDegrees
import backend.ToUnits
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

enum class Zoom : ToUnits {
    Closest, Middle, Farthest;

    override fun toUnits(): Units {
        return when (this) {
            Closest  -> 8f
            Middle   -> 14f
            Farthest -> 20f
        }
    }
}

private class AnimatedUnit<T : ToUnits>(initial: T) : Animated {
    private var value: T = initial
    private var units: Units = initial.toUnits()
    private var isMoving: Boolean = false
    private var startUnits: Degrees = initial.toUnits()
    private var finishUnits: Degrees = initial.toUnits()
    private var progress: Milliseconds = 0f

    fun getValue(): T {
        return value
    }

    fun getFloat(): Float {
        return units
    }

    fun moveTo(newValue: T) {
        if (isMoving) {
            // Ignore because the camera is currently in motion
            return
        } else if (value == newValue) {
            // Ignore because the new value is the same as the current heading
            return
        }

        value = newValue
        isMoving = true
        startUnits = units
        finishUnits = newValue.toUnits()
        progress = 0f
    }

    override fun nextFrame(delta: Duration) {
        if (!isMoving) {
            return
        }

        progress += delta.request(OrbitalCamera.DURATION - progress)
        units = Easing.InOutQuad.calc(progress, OrbitalCamera.DURATION, startUnits, finishUnits)

        if (abs(finishUnits - units) < OrbitalCamera.UNITS_CUTOFF) {
            units = finishUnits
            isMoving = false
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

    override fun nextFrame(delta: Duration) {
        if (!isMoving) {
            return
        }

        progress += delta.request(OrbitalCamera.DURATION - progress)
        angle = Easing.InOutQuad.calc(progress, OrbitalCamera.DURATION, startAngle, finishAngle)

        if (abs(finishAngle - angle) < OrbitalCamera.CUTOFF_DEGREES) {
            angle = finishAngle % 360
            isMoving = false
        }
    }
}

class OrbitalCamera(yaw: HexDirection, pitch: Pitch, zoom: Zoom) : Camera, Animated {
    companion object Constants {
        const val CUTOFF_DEGREES: Degrees = 1f
        const val UNITS_CUTOFF: Units = .1f
        const val DURATION: Milliseconds = 200f
    }

    private val originalYaw = yaw
    private val originalPitch = pitch
    private val originalZoom = zoom

    private val yaw = AnimatedDegree(yaw)
    private val pitch = AnimatedDegree(pitch)
    private val zoom = AnimatedUnit(zoom)

    private val focus = Vector3f()

    private val cachedPosition = Vector3f()
    private val cachedRotation = Vector3f()
    private val cachedNormal = Vector3f()
    private val cachedViewMatrix = Matrix4f()

    override val fieldOfView: Degrees = 60f
    override val zNear: Units = 0.001f
    override val zFar: Units = 1_000f

    init {
        updateCachedValues()
    }

    override val position: Vector3f
        get() = cachedPosition

    override val normal: Vector3f
        get() = cachedNormal

    override val rotation: Vector3f
        get() = cachedRotation

    override val viewMatrix: Matrix4f
        get() = cachedViewMatrix

    override fun nextFrame(delta: Duration) {
        yaw.nextFrame(delta)
        pitch.nextFrame(delta.fresh())
        zoom.nextFrame(delta.fresh())
        updateCachedValues()
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

    fun zoomIn() {
        val newZoom = when (zoom.getValue()) {
            Zoom.Closest  -> Zoom.Closest
            Zoom.Middle   -> Zoom.Closest
            Zoom.Farthest -> Zoom.Middle
        }
        moveTo(zoom = newZoom)
    }

    fun zoomOut() {
        val newZoom = when (zoom.getValue()) {
            Zoom.Closest  -> Zoom.Middle
            Zoom.Middle   -> Zoom.Farthest
            Zoom.Farthest -> Zoom.Farthest
        }
        moveTo(zoom = newZoom)
    }

    fun reset() {
        moveTo(yaw = originalYaw, pitch = originalPitch, zoom = originalZoom)
    }

    private fun moveTo(
        yaw: HexDirection = this.yaw.getValue(), pitch: Pitch = this.pitch.getValue(), zoom: Zoom = this.zoom.getValue()
    ) {
        this.yaw.moveTo(yaw)
        this.pitch.moveTo(pitch)
        this.zoom.moveTo(zoom)
        updateCachedValues()
    }

    private fun updateCachedValues() {
        calcCameraPosition(zoom.getFloat(), focus, yaw.getFloat(), pitch.getFloat(), cachedPosition)
        calcCameraRotation(yaw.getFloat(), pitch.getFloat(), cachedRotation)
        calcCameraNormal(zoom.getFloat(), yaw.getFloat(), pitch.getFloat(), cachedNormal)
        calcCameraViewMatrix(cachedPosition, cachedRotation, cachedViewMatrix)
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

private fun calcCameraPosition(radius: Units, focus: Vector3f, yaw: Degrees, pitch: Degrees, dest: Vector3f) {
    // Subtract 90deg from the yaw because the toRadians function expects
    // degrees relative to the x-axis but the original yaw value is
    // relative to the y-axis instead
    val yawRad = Math.toRadians((yaw - 90f).toDouble())
    val pitchRad = Math.toRadians(pitch.toDouble())

    val x = focus.x + radius * sin(pitchRad) * cos(yawRad)
    val y = focus.y + radius * cos(pitchRad)
    val z = focus.z + radius * sin(pitchRad) * sin(yawRad)

    dest.set(x, y, z)
}

private fun calcCameraNormal(radius: Units, yaw: Degrees, pitch: Degrees, dest: Vector3f) {
    // Subtract 90deg from the yaw because the toRadians function expects
    // degrees relative to the x-axis but the original yaw value is
    // relative to the y-axis instead
    val yawRad = Math.toRadians((yaw - 90f).toDouble())
    val pitchRad = Math.toRadians(pitch.toDouble())

    val x = -radius * sin(pitchRad) * cos(yawRad)
    val y = -radius * cos(pitchRad)
    val z = -radius * sin(pitchRad)

    dest.set(x, y, z)
}

private fun calcCameraRotation(yaw: Degrees, pitch: Degrees, dest: Vector3f) {
    val lat = -pitch + 90f
    val lon = (yaw + 180f) % 360f

    dest.set(lat, lon, 0f)
}

private fun calcCameraViewMatrix(pos: Vector3f, rot: Vector3f, dest: Matrix4f) {
    dest.identity()

    // Apply the rotation...
    val xRad = Math.toRadians(rot.x.toDouble())
    val yRad = Math.toRadians(rot.y.toDouble())
    dest.rotate(xRad.toFloat(), 1f, 0f, 0f)
        .rotate(yRad.toFloat(), 0f, 1f, 0f)

    // ...then apply the translation
    dest.translate(-pos.x, -pos.y, -pos.z)
}
