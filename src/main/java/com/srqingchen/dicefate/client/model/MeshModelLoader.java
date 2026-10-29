package com.srqingchen.dicefate.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.Transparency;
import com.srqingchen.dicefate.DiceFate;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * dicefate:mesh 模型加载器 —— 消费 Blockbench 导出的多面体网格 JSON
 * （format dicefate:mesh/1：faces[].{verts,uv,number}），实现真实 d4..d24 骰子。
 *
 * 模型 JSON：{"loader": "dicefate:mesh", "mesh": "dicefate:die_d12",
 *            "texture": "dicefate:item/die_shape_d12_fategold", "scale": 0.44}
 *
 * 0.2.0 渲染修复（"贴图偏离/建模混乱"根因）：
 * 1. 顶点 v*scale+0.5 —— ItemTransform.apply 尾部统一 translate(-0.5)，
 *    几何必须位于 0..1 空间，原 ±scale 的居中网格会整体斜偏半格；
 * 2. UV 平面投影 —— 以面法线构造 U/V 切轴（V 取世界上方向在面内的投影），
 *    每顶点 (du,dv) 映射到 uv 矩形，替代"按顶点序固定取角"导致的
 *    第二三角形退化/面朝向不一/多边形拉花；
 * 3. ground 平移 (0,0.125,0) —— Java 直构绕过反序列化 ×0.0625，原 (0,2,0) 上浮 2 格。
 */
public final class MeshModelLoader implements UnbakedModelLoader<MeshModelLoader.MeshModel> {

    @Override
    public MeshModel read(JsonObject json, JsonDeserializationContext ctx) throws JsonParseException {
        if (!json.has("mesh") || !json.get("mesh").isJsonPrimitive()) {
            throw new JsonParseException("dicefate:mesh 模型缺少 \"mesh\" 字段");
        }
        Identifier meshId = Identifier.parse(json.get("mesh").getAsString());
        String texture = json.has("texture") ? json.get("texture").getAsString() : null;
        // 0.3.0：scale 解析链 item json → mesh json → 0.44（NaN 表示未指定）
        float scale = json.has("scale") ? json.get("scale").getAsFloat() : Float.NaN;
        return new MeshModel(meshId, texture, scale);
    }

    public static final class MeshModel implements UnbakedModel {
        private final Identifier meshId;
        private final String textureOverride;
        private final float scaleOverride;
        private List<MeshFace> faces;
        private float meshScale = Float.NaN;
        private String meshTexture;

        public MeshModel(Identifier meshId, String textureOverride, float scaleOverride) {
            this.meshId = meshId;
            this.textureOverride = textureOverride;
            this.scaleOverride = scaleOverride;
        }

        private float effectiveScale() {
            if (!Float.isNaN(scaleOverride)) return scaleOverride;
            if (!Float.isNaN(meshScale)) return meshScale;
            return 0.44F;
        }

        private String effectiveTexture() {
            return textureOverride != null ? textureOverride
                : (meshTexture != null ? meshTexture : meshTextureOf(meshId));
        }

        @Override
        public TextureSlots.Data textureSlots() {
            var mat = new net.minecraft.client.resources.model.sprite.Material(
                Identifier.parse(effectiveTexture()));
            return new TextureSlots.Data.Builder()
                .addTexture("texture", mat)
                .addTexture("particle", mat)   // 0.3.0：破碎粒子同贴图（原缺槽引用缺失）
                .build();
        }

        @Override
        public ItemTransforms transforms() {
            ItemTransform gui = new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625F, 0.625F, 0.625F));
            // 0.3.0：拆分第一/第三人称（原共用一组：第三人称嵌臂偏大）
            ItemTransform fp = new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.40F, 0.40F, 0.40F));
            ItemTransform tp = new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.15625F, 0), new Vector3f(0.375F, 0.375F, 0.375F));
            ItemTransform ground = new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0.125F, 0), new Vector3f(0.25F, 0.25F, 0.25F));
            ItemTransform fixed = new ItemTransform(new Vector3f(0, 30, 0), new Vector3f(0, 0, 0), new Vector3f(0.5F, 0.5F, 0.5F));
            return new ItemTransforms(fp, fp, fp, tp, ItemTransform.NO_TRANSFORM, gui, ground, fixed, fixed);
        }

        @Override
        public UnbakedGeometry geometry() {
            return (textureSlots, modelBaker, modelState, name) -> bakeQuads(textureSlots, modelBaker, name);
        }

        private QuadCollection bakeQuads(TextureSlots slots, ModelBaker baker, ModelDebugName name) {
            if (faces == null) {
                MeshData data = loadMesh(meshId);
                faces = data.faces();
                meshTexture = data.texture();
                meshScale = data.scale();
            }
            float scale = effectiveScale();
            var material = baker.materials().resolveSlot(slots, "texture", name);
            var materialInfo = baker.interner().materialInfo(
                BakedQuad.MaterialInfo.of(material, Transparency.NONE, -1, true, 0));

            QuadCollection.Builder builder = new QuadCollection.Builder();
            for (MeshFace face : faces) {
                addFace(builder, baker, face, materialInfo, scale);
            }
            return builder.build();
        }

        private void addFace(QuadCollection.Builder builder, ModelBaker baker, MeshFace face,
                             BakedQuad.MaterialInfo info, float scale) {
            List<float[]> verts = face.verts();
            if (verts.size() < 3) return;

            // —— 几何：v*scale + 0.5（0..1 空间居中） ——
            Vector3fc[] pv = new Vector3fc[verts.size()];
            for (int i = 0; i < verts.size(); i++) {
                float[] a = verts.get(i);
                pv[i] = baker.interner().vector(new Vector3f(
                    a[0] * scale + 0.5F, a[1] * scale + 0.5F, a[2] * scale + 0.5F));
            }

            // —— UV：平面投影 ——
            float[] n = normal(verts);
            Direction dir = nearestAxis(n);
            // V 轴：世界上方向在面内的投影；顶/底面退化为 +Z
            Vector3f vAxis = new Vector3f(0, 1, 0);
            if (Math.abs(n[1]) > 0.9F) vAxis.set(0, 0, 1);
            float dot = n[0] * vAxis.x + n[1] * vAxis.y + n[2] * vAxis.z;
            Vector3f vProj = new Vector3f(vAxis.x - n[0] * dot, vAxis.y - n[1] * dot, vAxis.z - n[2] * dot);
            if (vProj.lengthSquared() < 1e-6f) vProj.set(0, 0, 1);
            vProj.normalize();
            // U 轴 = V × N（保持右手系：U 在面上水平向右）
            Vector3f uAxis = new Vector3f(
                vProj.y * n[2] - vProj.z * n[1],
                vProj.z * n[0] - vProj.x * n[2],
                vProj.x * n[1] - vProj.y * n[0]);

            // 面中心
            float cx = 0, cy = 0, cz = 0;
            for (float[] a : verts) { cx += a[0]; cy += a[1]; cz += a[2]; }
            cx /= verts.size(); cy /= verts.size(); cz /= verts.size();

            // 各顶点 (du, dv)，归一到 [-1,1]
            float maxR = 1e-6f;
            float[] du = new float[verts.size()];
            float[] dv = new float[verts.size()];
            for (int i = 0; i < verts.size(); i++) {
                float rx = verts.get(i)[0] - cx, ry = verts.get(i)[1] - cy, rz = verts.get(i)[2] - cz;
                du[i] = rx * uAxis.x + ry * uAxis.y + rz * uAxis.z;
                dv[i] = rx * vProj.x + ry * vProj.y + rz * vProj.z;
                maxR = Math.max(maxR, Math.max(Math.abs(du[i]), Math.abs(dv[i])));
            }
            for (int i = 0; i < verts.size(); i++) { du[i] /= maxR; dv[i] /= maxR; }

            // uv 矩形
            float u1 = face.uv()[0], v1 = face.uv()[1], u2 = face.uv()[2], v2 = face.uv()[3];
            long[] uvs = new long[verts.size()];
            for (int i = 0; i < verts.size(); i++) {
                float u = (du[i] + 1) * 0.5F * (u2 - u1) + u1;
                float v = v1 + (v2 - v1) * (1 - (dv[i] + 1) * 0.5F);   // dv=+1(几何上) → v1(贴图顶)
                uvs[i] = UVPair.pack(info.sprite().getU(u), info.sprite().getV(v));
            }

            // 输出面：4 顶点单 Quad；>4 顶点三角扇
            if (verts.size() == 4) {
                builder.addUnculledFace(new BakedQuad(pv[0], pv[1], pv[2], pv[3],
                    uvs[0], uvs[1], uvs[2], uvs[3], dir, info));
            } else if (verts.size() == 3) {
                builder.addUnculledFace(new BakedQuad(pv[0], pv[1], pv[2], pv[2],
                    uvs[0], uvs[1], uvs[2], uvs[2], dir, info));
            } else {
                for (int i = 1; i + 1 < verts.size(); i++) {
                    builder.addUnculledFace(new BakedQuad(pv[0], pv[i], pv[i + 1], pv[i + 1],
                        uvs[0], uvs[i], uvs[i + 1], uvs[i + 1], dir, info));
                }
            }
        }

        private static float[] normal(List<float[]> verts) {
            float[] a = verts.get(0), b = verts.get(1), c = verts.get(2);
            float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
            float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            return l < 1e-6f ? new float[]{0, 1, 0} : new float[]{nx / l, ny / l, nz / l};
        }

        private static Direction nearestAxis(float[] n) {
            float ax = Math.abs(n[0]), ay = Math.abs(n[1]), az = Math.abs(n[2]);
            if (ay >= ax && ay >= az) return n[1] > 0 ? Direction.UP : Direction.DOWN;
            if (ax >= az) return n[0] > 0 ? Direction.EAST : Direction.WEST;
            return n[2] > 0 ? Direction.SOUTH : Direction.NORTH;
        }

        public record MeshFace(List<float[]> verts, float[] uv, int number) {}
    }

    /** 从资源包读取网格 JSON（含根级 texture/scale 兜底）。 */
    static MeshData loadMesh(Identifier meshId) {
        ResourceManager rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
        var res = rm.getResource(Identifier.fromNamespaceAndPath(meshId.getNamespace(),
            "models/mesh/" + meshId.getPath() + ".json"));
        List<MeshModel.MeshFace> out = new ArrayList<>();
        String texture = null;
        float scale = Float.NaN;
        if (res.isEmpty()) {
            DiceFate.LOGGER.warn("mesh {} 不存在", meshId);
            return new MeshData(out, texture, scale);
        }
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("texture")) texture = root.get("texture").getAsString();
            if (root.has("scale")) scale = root.get("scale").getAsFloat();
            for (var e : root.getAsJsonArray("faces")) {
                JsonObject f = e.getAsJsonObject();
                List<float[]> verts = new ArrayList<>();
                for (var v : f.getAsJsonArray("verts")) {
                    JsonArray arr = v.getAsJsonArray();
                    verts.add(new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()});
                }
                JsonArray uv = f.getAsJsonArray("uv");
                int number = f.has("number") ? f.get("number").getAsInt() : 0;
                out.add(new MeshModel.MeshFace(verts, new float[]{
                    uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                    uv.get(2).getAsFloat(), uv.get(3).getAsFloat()}, number));
            }
        } catch (Exception ex) {
            DiceFate.LOGGER.error("Failed to load mesh {}", meshId, ex);
        }
        return new MeshData(out, texture, scale);
    }

    record MeshData(List<MeshModel.MeshFace> faces, String texture, float scale) {}

    /** mesh JSON 内的兜底贴图（"dicefate:die_d6" → "dicefate:item/die_shape_d6"）。 */
    private static String meshTextureOf(Identifier meshId) {
        return meshId.getNamespace() + ":item/" + meshId.getPath().replace("die_", "die_shape_");
    }
}
