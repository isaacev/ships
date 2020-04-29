package backend.memory;

import org.joml.GeometryUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh implements Managed {
    private final int vaoId;
    private final List<Integer> vboIdList;
    private final int vertexCount;

    public Mesh(float[] positions, float[] textCoords, float[] normals, int[] indices) {
        FloatBuffer posBuffer = null;
        FloatBuffer textCoordsBuffer = null;
        FloatBuffer vecNormalsBuffer = null;
        IntBuffer indicesBuffer = null;
        try {
            vertexCount = indices.length;
            vboIdList = new ArrayList<>();

            vaoId = glGenVertexArrays();
            glBindVertexArray(vaoId);

            // Position VBO
            int vboId = glGenBuffers();
            vboIdList.add(vboId);
            posBuffer = MemoryUtil.memAllocFloat(positions.length);
            posBuffer.put(positions).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);

            // Color coordinates VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            textCoordsBuffer = MemoryUtil.memAllocFloat(textCoords.length);
            textCoordsBuffer.put(textCoords).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, textCoordsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);

            // Vertex normals VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            vecNormalsBuffer = MemoryUtil.memAllocFloat(normals.length);
            vecNormalsBuffer.put(normals).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vboId);
            glBufferData(GL_ARRAY_BUFFER, vecNormalsBuffer, GL_STATIC_DRAW);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);

            // Index VBO
            vboId = glGenBuffers();
            vboIdList.add(vboId);
            indicesBuffer = MemoryUtil.memAllocInt(indices.length);
            indicesBuffer.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vboId);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);

            glBindBuffer(GL_ARRAY_BUFFER, 0);
            glBindVertexArray(0);
        } finally {
            if (posBuffer != null) {
                MemoryUtil.memFree(posBuffer);
            }

            if (textCoordsBuffer != null) {
                MemoryUtil.memFree(textCoordsBuffer);
            }

            if (vecNormalsBuffer != null) {
                MemoryUtil.memFree(vecNormalsBuffer);
            }

            if (indicesBuffer != null) {
                MemoryUtil.memFree(indicesBuffer);
            }
        }
    }

    public int getVaoId() {
        return this.vaoId;
    }

    public int getVertexCount() {
        return this.vertexCount;
    }

    public void deleteBuffers() {
        glDisableVertexAttribArray(0);

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    @Override
    public void free() {
        glDisableVertexAttribArray(0);

        // Delete the VBOs
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        for (int vboId : vboIdList) {
            glDeleteBuffers(vboId);
        }

        // Delete the VAO
        glBindVertexArray(0);
        glDeleteVertexArrays(vaoId);
    }

    public static class Builder {
        public List<Vector3f> vertices = new ArrayList<>();
        public List<Vector2f> textures = new ArrayList<>();
        public List<Vector3f> normals = new ArrayList<>();
        public List<Vector3i> polygons = new ArrayList<>();

        public int addVertex(Vector3f vertex, Vector3f normal, Vector2f texture) {
            vertices.add(vertex);
            normals.add(normal);
            textures.add(texture);
            return vertices.size() - 1;
        }

        public void addPolygon(int v0, int v1, int v2) {
            polygons.add(new Vector3i(v0, v1, v2));
        }

        public Mesh toMesh() {
            return new Mesh(
                vec3f_to_arr(vertices),
                vec2f_to_arr(textures),
                vec3f_to_arr(normals),
                vec3i_to_arr(polygons));
        }

        public static Vector3f normal(Vector3f a, Vector3f b, Vector3f c) {
            Vector3f dest = new Vector3f();
            GeometryUtils.normal(a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, dest);
            return dest;
        }

        private static float[] vec3f_to_arr(List<Vector3f> list) {
            float[] arr = new float[list.size() * 3];
            int i = 0;
            for (Vector3f vec : list) {
                arr[i * 3] = vec.x;
                arr[i * 3 + 1] = vec.y;
                arr[i * 3 + 2] = vec.z;
                i++;
            }
            return arr;
        }

        private static int[] vec3i_to_arr(List<Vector3i> list) {
            int[] arr = new int[list.size() * 3];
            int i = 0;
            for (Vector3i vec : list) {
                arr[i * 3] = vec.x;
                arr[i * 3 + 1] = vec.y;
                arr[i * 3 + 2] = vec.z;
                i++;
            }
            return arr;
        }

        private static float[] vec2f_to_arr(List<Vector2f> list) {
            float[] arr = new float[list.size() * 2];
            int i = 0;
            for (Vector2f vec : list) {
                arr[i * 2] = vec.x;
                arr[i * 2 + 1] = vec.y;
                i++;
            }
            return arr;
        }
    }
}
