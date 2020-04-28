package backend.memory

import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.opengl.GL20.*
import org.lwjgl.system.MemoryStack

typealias ProgramId = Int
typealias ShaderId = Int
typealias UniformName = String
typealias UniformId = Int

class Shader(private val directory: String, uniforms: List<UniformName>) : Managed {
    private val programId: ProgramId = glCreateProgram()
    private val vertexId: ShaderId
    private val fragmentId: ShaderId
    private val uniforms: Map<UniformName, UniformId>

    init {
        if (programId == 0) error("unable to create shader program")

        vertexId = readAndLoadShader(directory, "vertex.glsl", GL_VERTEX_SHADER)
        fragmentId = readAndLoadShader(directory, "fragment.glsl", GL_FRAGMENT_SHADER)
        link()

        this.uniforms = (uniforms.map { name ->
            val uniformId: UniformId = glGetUniformLocation(programId, name)
            if (uniformId < 0) error("could not find uniform named $name (is it unused?)")
            name to uniformId
        }).toMap()
    }

    override fun free() {
        println("free $directory shader")
        unbind()
        if (programId != 0) {
            glDeleteProgram(programId)
        }
    }

    fun link() {
        glLinkProgram(programId)
        if (glGetProgrami(programId, GL_LINK_STATUS) == 0) {
            val reason = glGetProgramInfoLog(programId, 1024)
            error("unable to link shader: $reason")
        }

        if (vertexId != 0) {
            glDetachShader(programId, vertexId)
        }

        if (fragmentId != 0) {
            glDetachShader(programId, fragmentId)
        }
    }

    fun bind() {
        glUseProgram(programId)
    }

    fun unbind() {
        glUseProgram(0)
    }

    fun setUniform(name: UniformName, value: Boolean): Shader {
        return if (value) {
            setUniform(name, 1)
        } else {
            setUniform(name, 0)
        }
    }

    fun setUniform(name: UniformName, num: Int): Shader {
        glUniform1i(uniforms[name] ?: error("tried to set unknown uniform named $name"), num)
        return this
    }

    fun setUniform(name: UniformName, vec: Vector2f): Shader {
        glUniform2f(uniforms[name] ?: error("tried to set unknown uniform named $name"), vec.x, vec.y)
        return this
    }

    fun setUniform(name: UniformName, vec: Vector3f): Shader {
        glUniform3f(uniforms[name] ?: error("tried to set unknown uniform named $name"), vec.x, vec.y, vec.z)
        return this
    }

    fun setUniform(name: UniformName, matrix: Matrix4f): Shader {
        MemoryStack.stackPush().use { stack ->
            glUniformMatrix4fv(
                uniforms[name] ?: error("tried to set unknown uniform named $name"),
                false,
                matrix.get(stack.mallocFloat(16))
            )
        }
        return this
    }

    private fun readShader(directory: String, filename: String): String {
        return Shader::class.java.getResource("/resources/shaders/$directory/$filename").readText()
    }

    private fun readAndLoadShader(directory: String, filename: String, shaderType: Int): ShaderId {
        val shaderId: ShaderId = glCreateShader(shaderType)
        if (shaderId == 0) {
            val type = when (shaderType) {
                GL_VERTEX_SHADER   -> "vertex"
                GL_FRAGMENT_SHADER -> "fragment"
                else               -> "no. $shaderType"
            }
            error("unable to create $type shader")
        }

        val shaderCode = readShader(directory, filename)
        glShaderSource(shaderId, shaderCode)
        glCompileShader(shaderId)
        if (glGetShaderi(shaderId, GL_COMPILE_STATUS) == 0) {
            val reason = glGetShaderInfoLog(shaderId, 1024)
            error("unable to compile shader: $reason")
        }

        println("load shader (from /resources/shaders/$directory/$filename)")
        glAttachShader(programId, shaderId)
        return shaderId
    }
}

class ShaderCatalog(config: Map<String, List<UniformName>>) : Managed {
    private var shaders: Map<String, Shader?> = HashMap()

    val size: Int
        get() = shaders.size

    init {
        shaders = config.mapValues { (directory, uniforms) ->
            Shader(directory, uniforms)
        }
    }

    override fun free() {
        for ((_, shader) in shaders) {
            shader?.free()
        }
    }

    fun get(name: String): Shader? {
        return shaders[name]
    }
}
