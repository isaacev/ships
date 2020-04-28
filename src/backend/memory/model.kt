package backend.memory

import backend.utils.Loader

class ModelCatalog : Managed {
    private val models: MutableMap<String, Model<Any>> = HashMap()

    fun <T> upload(name: String, model: Model<T>): Model<T> {
        models[name] = model as Model<Any>
        return model
    }

    fun <T> load(name: String, texture: Texture, shader: Shader, meshes: Map<T, String>): Model<T> {
        val loadedMeshes = meshes.mapValues { (_, filepath) -> Loader.loadMesh(filepath) }
        val model: Model<T> = Model(texture, shader, loadedMeshes)
        models[name] = model as Model<Any>
        return model
    }

    fun <T> get(name: String): Model<T>? {
        @Suppress("UNCHECKED_CAST") return models[name] as Model<T>?
    }

    val size: Int
        get() = models.size

    override fun free() {
        for ((_, model) in models) {
            model.free()
        }
    }
}

/**
 * Many Entities can use the same Model for rendering. When it's time to
 * render an Entity, the entity provides its Model to the Renderer. The
 * Renderer looks at the Model and for each Mesh, asked the Entity for
 * that Mesh's position and rotation.
 *
 * In the current implementation, Shaders are live for the duration of
 * the game so the Model SHOULD NOT call Shaders::cleanup
 */
class Model<Key>(val texture: Texture, val shader: Shader, val meshes: Map<Key, Mesh>) : Managed {
    override fun free() {
        for ((_, mesh) in meshes) {
            mesh.free()
        }

        // DO NOT call Shaders::cleanup
    }
}
