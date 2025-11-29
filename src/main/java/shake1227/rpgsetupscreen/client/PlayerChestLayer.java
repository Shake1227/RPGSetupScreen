package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import shake1227.rpgsetupscreen.setup.RPGCapability;

@OnlyIn(Dist.CLIENT)
public class PlayerChestLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerChestLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            if (cap.getGender() == 1 && cap.getChest() > 0.0f) {
                renderBreasts(poseStack, buffer, packedLight, player, cap.getChest());
            }
        });
    }

    private void renderBreasts(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float size) {
        poseStack.pushPose();

        // 1. 体の動きに追従
        this.getParentModel().body.translateAndRotate(poseStack);

        // 2. 位置調整
        // Y:少し下げる(0.20), Z:体に密着させる(-0.11)
        poseStack.translate(0.0F, 0.20F, -0.11F);

        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutout(player.getSkinTextureLocation()));

        // テクスチャ設定
        float tw = 64.0f;
        float th = 64.0f;
        int texU = 20;
        int texV = 20;

        // 形状パラメータ
        float width = 4.0f;
        float height = 5.5f; // 少し縦長に

        // 厚み計算 (0.5 ~ 5.0)
        float maxDepth = 0.5f + (size * 4.5f);
        float minDepth = maxDepth * 0.3f; // 外側は内側の3割程度の厚みにする（なだらかな傾斜）

        // --- 左胸 (Left Breast) ---
        poseStack.pushPose();
        {
            poseStack.translate(0.12F, 0.0F, 0.0F);
            // 上向き傾斜のみ適用 (Y回転は形状で表現するため削除)
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));

            // 左胸: 左側(xMin)が内側(厚い), 右側(xMax)が外側(薄い)
            // xMin=-2.0(Inner), xMax=2.0(Outer)
            drawTaperedBulge(poseStack, builder, packedLight,
                    width, height,
                    maxDepth, minDepth, // 左(内)を厚く、右(外)を薄く
                    texU + 4, texV, tw, th
            );
        }
        poseStack.popPose();

        // --- 右胸 (Right Breast) ---
        poseStack.pushPose();
        {
            poseStack.translate(-0.12F, 0.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F));

            // 右胸: 左側(xMin)が外側(薄い), 右側(xMax)が内側(厚い)
            // xMin=-2.0(Outer), xMax=2.0(Inner)
            drawTaperedBulge(poseStack, builder, packedLight,
                    width, height,
                    minDepth, maxDepth, // 左(外)を薄く、右(内)を厚く
                    texU, texV, tw, th
            );
        }
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * 左右で厚みが異なる（テーパー状）胸を描画するメソッド
     */
    private void drawTaperedBulge(PoseStack stack, VertexConsumer builder, int light,
                                  float w, float h,
                                  float dLeft, float dRight, // 左右それぞれの厚み
                                  int u, int v, float tw, float th) {

        Matrix4f matrix = stack.last().pose();
        Matrix3f normal = stack.last().normal();

        // 頂点座標 (1/16スケール)
        float xMin = -w / 16.0f / 2.0f;
        float xMax = w / 16.0f / 2.0f;

        float yTop = -h / 16.0f / 2.0f;
        float yMid = yTop + (h * 0.6f / 16.0f); // トップの位置
        float yBot = yTop + (h / 16.0f);

        // Z座標
        float zBase = 0.0f; // 体表面

        // 左右それぞれのピーク位置（厚み）
        float zPeakLeft = -dLeft / 16.0f;
        float zPeakRight = -dRight / 16.0f;

        // 貫通対策: 埋め込み深度を0.15F(約2.4ピクセル)に制限
        // 体の厚み(0.25F)を超えないようにする
        float zInside = 0.15f;

        // UV
        float vTop = v;
        float vMid = v + 3.5f;
        float vBot = v + 6.0f;
        float uMin = u;
        float uMax = u + w;

        // 法線計算用のベクトル定義 (照明対策)
        // 上面: 上(-Y)と前(-Z)を向く
        // 下面: 下(+Y)と前(-Z)を向く

        // === 1. 前面 (Upper Slope) ===
        // 明るくするため法線Yをマイナス(上向き)にする
        addQuad(builder, matrix, normal, light,
                xMin, yTop, zBase, uMin, vTop,      // TL
                xMin, yMid, zPeakLeft, uMin, vMid,  // BL
                xMax, yMid, zPeakRight, uMax, vMid, // BR
                xMax, yTop, zBase, uMax, vTop,      // TR
                0, -0.8f, -0.6f // Normal: 上・前
        );

        // === 2. 下面 (Lower Slope) ===
        // 影をつけるため法線Yをプラス(下向き)にする
        addQuad(builder, matrix, normal, light,
                xMin, yMid, zPeakLeft, uMin, vMid,   // TL
                xMin, yBot, zBase, uMin, vBot,       // BL
                xMax, yBot, zBase, uMax, vBot,       // BR
                xMax, yMid, zPeakRight, uMax, vMid,  // TR
                0, 0.8f, -0.6f // Normal: 下・前
        );

        // === 3. 側面 (Sides) - 隙間埋め ===
        // 頂点を zInside (体の内側) まで引っ張って三角形を描き、隙間を埋める

        // 左側面 (Left Side Fill)
        // Top-Inner Triangle
        addTri(builder, matrix, normal, light,
                xMin, yTop, zBase, uMin, vTop,
                xMin, yTop, zInside, uMin, vTop,
                xMin, yMid, zPeakLeft, uMin, vMid,
                -1, 0, 0
        );
        // Bot-Inner Triangle
        addTri(builder, matrix, normal, light,
                xMin, yMid, zPeakLeft, uMin, vMid,
                xMin, yBot, zInside, uMin, vBot,
                xMin, yBot, zBase, uMin, vBot,
                -1, 0, 0
        );
        // Middle Filler (Peak to Inside)
        addTri(builder, matrix, normal, light,
                xMin, yTop, zInside, uMin, vTop,
                xMin, yBot, zInside, uMin, vBot,
                xMin, yMid, zPeakLeft, uMin, vMid,
                -1, 0, 0
        );

        // 右側面 (Right Side Fill)
        // Top-Inner Triangle
        addTri(builder, matrix, normal, light,
                xMax, yTop, zInside, uMax, vTop,
                xMax, yTop, zBase, uMax, vTop,
                xMax, yMid, zPeakRight, uMax, vMid,
                1, 0, 0
        );
        // Bot-Inner Triangle
        addTri(builder, matrix, normal, light,
                xMax, yBot, zInside, uMax, vBot,
                xMax, yMid, zPeakRight, uMax, vMid,
                xMax, yBot, zBase, uMax, vBot,
                1, 0, 0
        );
        // Middle Filler
        addTri(builder, matrix, normal, light,
                xMax, yBot, zInside, uMax, vBot,
                xMax, yTop, zInside, uMax, vTop,
                xMax, yMid, zPeakRight, uMax, vMid,
                1, 0, 0
        );

        // 上面・下面の隙間埋め (薄いポリゴンで蓋をする)
        // Top Gap
        addQuad(builder, matrix, normal, light,
                xMin, yTop, zInside, uMin, vTop,
                xMin, yTop, zBase, uMin, vTop,
                xMax, yTop, zBase, uMax, vTop,
                xMax, yTop, zInside, uMax, vTop,
                0, -1, 0 // 上向き
        );
        // Bot Gap
        addQuad(builder, matrix, normal, light,
                xMin, yBot, zBase, uMin, vBot,
                xMin, yBot, zInside, uMin, vBot,
                xMax, yBot, zInside, uMax, vBot,
                xMax, yBot, zBase, uMax, vBot,
                0, 1, 0 // 下向き
        );
    }

    // 四角形描画ヘルパー
    private void addQuad(VertexConsumer builder, Matrix4f mat, Matrix3f nMat, int light,
                         float x1, float y1, float z1, float u1, float v1,
                         float x2, float y2, float z2, float u2, float v2,
                         float x3, float y3, float z3, float u3, float v3,
                         float x4, float y4, float z4, float u4, float v4,
                         float nx, float ny, float nz) {
        vertex(builder, mat, nMat, x1, y1, z1, u1, v1, light, nx, ny, nz);
        vertex(builder, mat, nMat, x2, y2, z2, u2, v2, light, nx, ny, nz);
        vertex(builder, mat, nMat, x3, y3, z3, u3, v3, light, nx, ny, nz);
        vertex(builder, mat, nMat, x4, y4, z4, u4, v4, light, nx, ny, nz);
    }

    // 三角形描画ヘルパー
    private void addTri(VertexConsumer builder, Matrix4f mat, Matrix3f nMat, int light,
                        float x1, float y1, float z1, float u1, float v1,
                        float x2, float y2, float z2, float u2, float v2,
                        float x3, float y3, float z3, float u3, float v3,
                        float nx, float ny, float nz) {
        vertex(builder, mat, nMat, x1, y1, z1, u1, v1, light, nx, ny, nz);
        vertex(builder, mat, nMat, x2, y2, z2, u2, v2, light, nx, ny, nz);
        vertex(builder, mat, nMat, x3, y3, z3, u3, v3, light, nx, ny, nz);
        // 三角形なので4点目は3点目と同じにして縮退させる（またはVertexConsumerの仕様に合わせて描画）
        vertex(builder, mat, nMat, x3, y3, z3, u3, v3, light, nx, ny, nz);
    }

    private void vertex(VertexConsumer builder, Matrix4f posMat, Matrix3f normMat,
                        float x, float y, float z, float u, float v,
                        int light, float nx, float ny, float nz) {

        Vector4f pos = new Vector4f(x, y, z, 1.0f);
        pos.mul(posMat);

        Vector3f norm = new Vector3f(nx, ny, nz);
        norm.mul(normMat);
        norm.normalize();

        builder.vertex(pos.x, pos.y, pos.z)
                .color(255, 255, 255, 255)
                .uv(u / 64.0f, v / 64.0f)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(norm.x, norm.y, norm.z)
                .endVertex();
    }
}