package backend.memory

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.nio.ByteBuffer

typealias TextureId = Int

class TextureCatalog : Managed {
    private val textures: MutableMap<String, Texture> = HashMap()

    private fun load(filename: String): Texture {
        val texture = SimpleTexture(filename)
        textures[filename] = texture
        return texture
    }

    private fun load(underlay: String, overlay: String): Texture {
        val texture = TextureWithOverlay(underlay, overlay)
        textures["$underlay $overlay"] = texture
        return texture
    }

    fun get(filename: String): Texture? {
        return textures[filename]
    }

    fun getOrLoad(filename: String): Texture {
        return get(filename) ?: load(filename)
    }

    fun getOrLoad(underlay: String, overlay: String): Texture {
        return get("$underlay $overlay") ?: load(underlay, overlay)
    }

    override fun free() {
        for ((_, texture) in textures) {
            texture.free()
        }
    }
}

interface Texture : Managed {
    fun bind()

    fun unbind()
}

class SimpleTexture(private val filename: String) : Texture {
    private val textureId: TextureId
    private var width: Int = -1
    private var height: Int = -1

    val id: TextureId
        get() = textureId

    init {
        var buf: ByteBuffer? = null
        MemoryStack.stackPush()
            .use { stack ->
                val w = stack.mallocInt(1)
                val h = stack.mallocInt(1)
                val channels = stack.mallocInt(1)

                buf = STBImage.stbi_load(filename, w, h, channels, 4)

                @Suppress("SENSELESS_COMPARISON") if (buf == null) {
                    error("unable to load $filename: ${STBImage.stbi_failure_reason()}")
                }

                width = w.get()
                height = h.get()
            }

        textureId = createTexture(buf!!)
        STBImage.stbi_image_free(buf)
        println("load texture (from $filename)")
    }

    override fun bind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)
    }

    override fun unbind() {
        GL30.glBindVertexArray(0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    override fun free() {
        println("free texture (from $filename)")
        GL11.glDeleteTextures(textureId)
    }

    private fun createTexture(buf: ByteBuffer): TextureId {
        val textureId = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId)

        // Tell OpenGL how to unpack the RGBA bytes. Each component is 1 byte in size
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST)

        // Upload the texture data
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf
        )

        // Generate the mipmap
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D)

        return textureId
    }
}

class TextureWithOverlay(underlay: String, overlay: String) : Texture {
    private val underlay = SimpleTexture(underlay)
    private val overlay = SimpleTexture(overlay)

    override fun bind() {
        GL13.glActiveTexture(GL13.GL_TEXTURE0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, underlay.id)

        GL13.glActiveTexture(GL13.GL_TEXTURE1)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, overlay.id)
    }

    override fun unbind() {
        GL30.glBindVertexArray(0)
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
    }

    override fun free() {
        underlay.free()
        overlay.free()
    }
}
