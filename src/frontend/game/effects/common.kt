package frontend.game.effects

import backend.memory.Mesh
import org.joml.Vector2f
import org.joml.Vector3f

internal fun buildUnitQuad(): Mesh {
    val builder = Mesh.Builder()

    val half = 1f / 2f;
    val vecA = Vector3f(-half, +half, 0f)
    val vecB = Vector3f(+half, +half, 0f)
    val vecC = Vector3f(-half, -half, 0f)
    val vecD = Vector3f(+half, -half, 0f)

    val norm = Mesh.Builder.normal(vecA, vecC, vecD)

    val texA = Vector2f(0f, 1f)
    val texB = Vector2f(1f, 1f)
    val texC = Vector2f(0f, 0f)
    val texD = Vector2f(1f, 0f)

    val idxA = builder.addVertex(vecA, norm, texA)
    val idxB = builder.addVertex(vecB, norm, texB)
    val idxC = builder.addVertex(vecC, norm, texC)
    val idxD = builder.addVertex(vecD, norm, texD)

    builder.addPolygon(idxA, idxC, idxB)
    builder.addPolygon(idxB, idxC, idxD)

    return builder.toMesh()
}
