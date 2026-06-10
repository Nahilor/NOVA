package renderer.geometry;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import renderer.math.Vector2;
import renderer.math.Vector3;

/**
 * A robust, dependency-free Wavefront .obj loader that parses 3D geometry
 * and returns a unified TriangleMesh.
 */
public final class OBJLoader {

    private static class VertexKey {
        final int posIdx;
        final int uvIdx;
        final int normIdx;

        VertexKey(int posIdx, int uvIdx, int normIdx) {
            this.posIdx = posIdx;
            this.uvIdx = uvIdx;
            this.normIdx = normIdx;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof VertexKey)) return false;
            VertexKey other = (VertexKey) o;
            return this.posIdx == other.posIdx && this.uvIdx == other.uvIdx && this.normIdx == other.normIdx;
        }

        @Override
        public int hashCode() {
            int result = posIdx;
            result = 31 * result + uvIdx;
            result = 31 * result + normIdx;
            return result;
        }
    }

    /**
     * Loads a TriangleMesh from a Wavefront .obj file.
     * 
     * @param filePath The absolute or relative path to the .obj file.
     * @return The loaded TriangleMesh.
     * @throws IOException If an I/O error occurs.
     */
    public static TriangleMesh load(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return parse(reader);
        }
    }

    /**
     * Loads a TriangleMesh from an InputStream (useful for resources).
     * 
     * @param in The InputStream of the .obj file.
     * @return The loaded TriangleMesh.
     * @throws IOException If an I/O error occurs.
     */
    public static TriangleMesh load(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            return parse(reader);
        }
    }

    private static TriangleMesh parse(BufferedReader reader) throws IOException {
        List<Vector3> tempPos = new ArrayList<>();
        List<Vector2> tempUvs = new ArrayList<>();
        List<Vector3> tempNorms = new ArrayList<>();

        List<Integer> indicesList = new ArrayList<>();
        List<VertexKey> uniqueVertices = new ArrayList<>();
        Map<VertexKey, Integer> vertexCache = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Split line by whitespace
            String[] tokens = line.split("\\s+");
            if (tokens.length == 0) continue;

            String type = tokens[0];
            switch (type) {
                case "v": // Vertex position
                    if (tokens.length >= 4) {
                        double x = Double.parseDouble(tokens[1]);
                        double y = Double.parseDouble(tokens[2]);
                        double z = Double.parseDouble(tokens[3]);
                        tempPos.add(new Vector3(x, y, z));
                    }
                    break;

                case "vt": // Texture coordinates
                    if (tokens.length >= 3) {
                        double u = Double.parseDouble(tokens[1]);
                        double v = Double.parseDouble(tokens[2]);
                        tempUvs.add(new Vector2(u, v));
                    }
                    break;

                case "vn": // Vertex normal
                    if (tokens.length >= 4) {
                        double x = Double.parseDouble(tokens[1]);
                        double y = Double.parseDouble(tokens[2]);
                        double z = Double.parseDouble(tokens[3]);
                        tempNorms.add(new Vector3(x, y, z));
                    }
                    break;

                case "f": // Face indices
                    // Parse all face vertex keys on the line
                    List<Integer> facePos = new ArrayList<>();
                    List<Integer> faceUv = new ArrayList<>();
                    List<Integer> faceNorm = new ArrayList<>();

                    for (int i = 1; i < tokens.length; i++) {
                        String token = tokens[i].trim();
                        if (token.isEmpty()) continue;
                        parseFaceToken(token, facePos, faceUv, faceNorm, tempPos.size(), tempUvs.size(), tempNorms.size());
                    }

                    // Perform fan triangulation for polygon faces (N-gons)
                    int numFaceVertices = facePos.size();
                    for (int i = 2; i < numFaceVertices; i++) {
                        addIndex(facePos.get(0), faceUv.get(0), faceNorm.get(0), uniqueVertices, vertexCache, indicesList);
                        addIndex(facePos.get(i - 1), faceUv.get(i - 1), faceNorm.get(i - 1), uniqueVertices, vertexCache, indicesList);
                        addIndex(facePos.get(i), faceUv.get(i), faceNorm.get(i), uniqueVertices, vertexCache, indicesList);
                    }
                    break;

                default:
                    // Ignore group markers, material library declarations, etc.
                    break;
            }
        }

        // Reconstruct unified flat arrays
        int numUniqueVertices = uniqueVertices.size();
        Vector3[] finalPositions = new Vector3[numUniqueVertices];
        Vector3[] finalNormals = tempNorms.isEmpty() ? null : new Vector3[numUniqueVertices];
        Vector2[] finalUvs = tempUvs.isEmpty() ? null : new Vector2[numUniqueVertices];

        for (int i = 0; i < numUniqueVertices; i++) {
            VertexKey key = uniqueVertices.get(i);
            finalPositions[i] = tempPos.get(key.posIdx);
            
            if (finalNormals != null && key.normIdx != -1) {
                finalNormals[i] = tempNorms.get(key.normIdx);
            }
            if (finalUvs != null && key.uvIdx != -1) {
                finalUvs[i] = tempUvs.get(key.uvIdx);
            }
        }

        int[] finalIndices = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) {
            finalIndices[i] = indicesList.get(i);
        }

        return new TriangleMesh(finalPositions, finalNormals, finalUvs, finalIndices);
    }

    private static void parseFaceToken(
        String token,
        List<Integer> posIndices,
        List<Integer> uvIndices,
        List<Integer> normIndices,
        int numPos,
        int numUvs,
        int numNorms
    ) {
        String[] parts = token.split("/", -1);
        int v = -1, vt = -1, vn = -1;

        // Position index (always present)
        int rawV = Integer.parseInt(parts[0]);
        v = rawV < 0 ? rawV + numPos : rawV - 1;

        // TexCoord index (optional)
        if (parts.length > 1 && !parts[1].isEmpty()) {
            int rawVt = Integer.parseInt(parts[1]);
            vt = rawVt < 0 ? rawVt + numUvs : rawVt - 1;
        }

        // Normal index (optional)
        if (parts.length > 2 && !parts[2].isEmpty()) {
            int rawVn = Integer.parseInt(parts[2]);
            vn = rawVn < 0 ? rawVn + numNorms : rawVn - 1;
        }

        posIndices.add(v);
        uvIndices.add(vt);
        normIndices.add(vn);
    }

    private static void addIndex(
        int posIdx,
        int uvIdx,
        int normIdx,
        List<VertexKey> uniqueVertices,
        Map<VertexKey, Integer> vertexCache,
        List<Integer> indicesList
    ) {
        VertexKey key = new VertexKey(posIdx, uvIdx, normIdx);
        Integer cachedIndex = vertexCache.get(key);
        if (cachedIndex == null) {
            int newIndex = uniqueVertices.size();
            uniqueVertices.add(key);
            vertexCache.put(key, newIndex);
            indicesList.add(newIndex);
        } else {
            indicesList.add(cachedIndex);
        }
    }
}
