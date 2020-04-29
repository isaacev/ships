package backend.memory

import backend.utils.Loader

class ModelCatalog : Managed {
    private val models: MutableMap<String, Model> = HashMap()

    fun <M : Model> upload(name: String, model: M): M {
        models[name] = model
        return model
    }

    fun <T> loadComplexModel(name: String, texture: Texture, shader: Shader, meshes: Map<T, String>): ComplexModel<T> {
        val loadedMeshes = meshes.mapValues { (_, filepath) -> Loader.loadMesh(filepath) }
        val model = ComplexModel(texture, shader, loadedMeshes)
        models[name] = model
        return model
    }

    fun <M : Model> getOrLoad(name: String, loader: () -> M): M {
        val knownModel = models[name]
        return if (knownModel != null) {
            knownModel as M
        } else {
            val newModel = loader()
            models[name] = newModel
            newModel
        }
    }

    fun get(name: String): Model? {
        return models[name]
    }

    val size: Int
        get() = models.size

    override fun free() {
        for ((_, model) in models) {
            model.free()
        }
    }
}

abstract class Model(val texture: Texture, val shader: Shader) : Managed

/**
 * Many Entities can use the same Model for rendering. When it's time to
 * render an Entity, the entity provides its Model to the Renderer. The
 * Renderer looks at the Model and for each Mesh, asked the Entity for
 * that Mesh's position and rotation.
 *
 * In the current implementation, Shaders are live for the duration of
 * the game so the Model SHOULD NOT call Shaders::cleanup
 */
class SimpleModel(texture: Texture, shader: Shader, val mesh: Mesh) : Model(texture, shader) {
    override fun free() {
        mesh.free()
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
class ComplexModel<Key>(texture: Texture, shader: Shader, val meshes: Map<Key, Mesh>) : Model(texture, shader) {
    override fun free() {
        for ((_, mesh) in meshes) {
            mesh.free()
        }
    }
}
