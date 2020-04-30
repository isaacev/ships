package backend.window

import backend.inputs.ButtonCode
import backend.inputs.KeyCode
import org.joml.Vector2i
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL32
import org.lwjgl.system.MemoryUtil
import kotlin.math.roundToInt

typealias ScreenPixels = Int
typealias ScreenCoordinate = Vector2i
typealias FrameCoordinate = Vector2i

interface WindowConfigs {
    val title: String
    val width: ScreenPixels
    val height: ScreenPixels
    val vSync: Boolean
}

class Window(configs: WindowConfigs) {
    private val title = configs.title
    private var handle = 0L
    private val screenSize = ScreenCoordinate(configs.width, configs.height)
    private val pixelSize = FrameCoordinate(screenSize)
    private var devicePixelRatio = 1
    private var wasResized = false
    private var vSync = configs.vSync

    init {
        // Create an error callback that's just STDERR
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before this
        if (!GLFW.glfwInit()) error("unable to initialize GLFW")

        // Configure window hints
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GL11.GL_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GL11.GL_TRUE)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GL11.GL_TRUE)

        // Create the window
        handle = GLFW.glfwCreateWindow(screenSize.x, screenSize.y, title, MemoryUtil.NULL, MemoryUtil.NULL)
        if (handle == MemoryUtil.NULL) error("unable to create the window")

        // After the window has been created, update our assumption
        // about the window's pixel dimensions. This becomes relevant
        // if the monitor is 4k/retina because in that case the screen
        // coordinates WILL NOT map 1:1 to pixel coordinates. The ratio
        // might be 1:2 or 1:4.
        //
        // For most calculations, screen coordinates are sufficient. But
        // when performing many render calls, if we pass screen coordinates
        // instead of pixel coordinates and are rendering on a 4k/retina
        // display, the graphics will be fuzzy or scaled-down.
        val widthBuffer = BufferUtils.createIntBuffer(1)
        val heightBuffer = BufferUtils.createIntBuffer(1)
        GLFW.glfwGetFramebufferSize(handle, widthBuffer, heightBuffer)
        pixelSize.x = widthBuffer.get(0)
        pixelSize.y = heightBuffer.get(0)
        devicePixelRatio = (pixelSize.x.toFloat() / screenSize.x.toFloat()).roundToInt()

        // Create a resize callback (screen coordinates)
        GLFW.glfwSetWindowSizeCallback(handle) { _, width, height ->
            wasResized = true

            screenSize.x = width
            pixelSize.x = width * devicePixelRatio

            screenSize.y = height
            pixelSize.y = height * devicePixelRatio
        }

        // Create a key callback. This will be called every
        // time a key is pressed, repeated, or released.
        GLFW.glfwSetKeyCallback(handle) { window, key, _, action, _ ->
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE) {
                GLFW.glfwSetWindowShouldClose(window, true)
            }
        }

        // Get the resolution of the primary monitor and use that
        // resolution to center the window in the monitor.
        val videoMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor())
        val offsetX = (videoMode.width() - screenSize.x) / 2
        val offsetY = (videoMode.height() - screenSize.y) / 2
        GLFW.glfwSetWindowPos(handle, offsetX, offsetY)

        // Make the OpenGL context current
        GLFW.glfwMakeContextCurrent(handle)

        // Pass the vSync setting to the graphics framework
        if (vSync) {
            GLFW.glfwSwapInterval(1)
        }

        // Make the window visible
        GLFW.glfwShowWindow(handle)
        GL.createCapabilities()

        // Clear the window
        GL11.glClearColor(0f, 0f, 0f, 0f)

        // Enable depth testing for 3d graphics
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_STENCIL_TEST)

        // Don't render polygons that face away from the camera
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)

        // Antialiasing
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST)
        GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4)
        GL32.glProvokingVertex(GL32.GL_FIRST_VERTEX_CONVENTION)

        // Support for transparencies
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }

    fun getHandle(): Long {
        return handle
    }

    fun shouldClose(): Boolean {
        return GLFW.glfwWindowShouldClose(handle)
    }

    fun hasVSync(): Boolean {
        return vSync
    }

    fun doesNotHaveVerticalSync(): Boolean {
        return !hasVSync()
    }

    fun getScreenWidth(): Int {
        return screenSize.x
    }

    fun getScreenHeight(): Int {
        return screenSize.y
    }

    fun getPixelWidth(): Int {
        return pixelSize.x
    }

    fun getPixelHeight(): Int {
        return pixelSize.y
    }

    fun getDevicePixelRatio(): Int {
        return devicePixelRatio
    }

    fun isButtonPressed(buttonCode: ButtonCode): Boolean {
        return GLFW.glfwGetMouseButton(handle, buttonCode) == GLFW.GLFW_PRESS
    }

    fun isButtonReleased(buttonCode: ButtonCode): Boolean {
        return GLFW.glfwGetMouseButton(handle, buttonCode) == GLFW.GLFW_RELEASE
    }

    fun isKeyPressed(keyCode: KeyCode): Boolean {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_PRESS
    }

    fun isKeyReleased(keyCode: KeyCode): Boolean {
        return GLFW.glfwGetKey(handle, keyCode) == GLFW.GLFW_RELEASE
    }

    fun resetDrawSettings() {
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_STENCIL_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glCullFace(GL11.GL_BACK)
    }

    fun update() {
        GLFW.glfwSwapBuffers(handle)
        GLFW.glfwPollEvents()
    }
}
