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
    public void render(PoseStack pStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        player.getCapability(RPGCapability.INSTANCE).ifPresent(cap -> {
            if (cap.getGender() == 1 && cap.getChest() > 0.0f) {

                // 前フレームと現在フレームの補間（元コードと同等）
                float bounceYL = net.minecraft.util.Mth.lerp(partialTick, cap.getPrevBouncePosL(), cap.getBouncePosL());
                float bounceRotL = net.minecraft.util.Mth.lerp(partialTick, cap.getPrevBounceRotL(), cap.getBounceRotL());
                float bounceYR = net.minecraft.util.Mth.lerp(partialTick, cap.getPrevBouncePosR(), cap.getBouncePosR());
                float bounceRotR = net.minecraft.util.Mth.lerp(partialTick, cap.getPrevBounceRotR(), cap.getBounceRotR());

                drawBothBreasts(pStack, buffer, packedLight, player,
                        cap.getChest(), cap.getWidth(), cap.getHeight(),
                        cap.getChestY(), cap.getChestSep(), cap.getChestAng(),
                        bounceYL, bounceRotL, bounceYR, bounceRotR);
            }
        });
    }

    private void drawBothBreasts(PoseStack stack, MultiBufferSource buffer, int light, AbstractClientPlayer player,
                                 float size, float wScale, float hScale,
                                 float posY, float posSep, float angle,
                                 float bounceYL, float bounceRotL, float bounceYR, float bounceRotR) {
        stack.pushPose();

        // 親モデルの body に合わせる（元コードと同じ）
        this.getParentModel().body.translateAndRotate(stack);

        float baseY = 1.0F / 16.0F;
        stack.translate(0.0F, baseY + posY, -0.02F);

        stack.mulPose(Axis.XP.rotationDegrees(-24.5F));
        stack.scale(0.9995f, 1f, 1f);

        VertexConsumer builder = buffer.getBuffer(RenderType.entityCutout(player.getSkinTextureLocation()));

        float width = 4.0f * wScale;
        float height = 5.0f * hScale;
        float depth = (0.5f + (size * 3.0f)) * wScale;
        float taperRatio = Math.min(0.9f, size * 0.8f);
        float taper = depth * taperRatio;

        float vOffset = posY * 16.0f;
        float uOffset = posSep * 16.0f;
        float texV = 17 + vOffset;
        float sepX = posSep * 16.0f;

        // 右胸：元コードと等価の変換を適用
        stack.pushPose();
        stack.translate(-sepX / 16.0f, bounceYR, 0.0f);
        float totalR = angle + bounceRotR;
        if (totalR != 0.0f) stack.mulPose(Axis.YP.rotationDegrees(totalR));
        drawTaperedBox(stack, builder, light,
                -width, 0.0f, -depth,
                width, height, depth,
                taper, 16 - uOffset, texV);
        stack.popPose();

        // 左胸
        stack.pushPose();
        stack.translate(sepX / 16.0f, bounceYL, 0.0f);
        float totalL = -angle + bounceRotL;
        if (totalL != 0.0f) stack.mulPose(Axis.YP.rotationDegrees(totalL));
        drawTaperedBox(stack, builder, light,
                0.0f, 0.0f, -depth,
                width, height, depth,
                taper, 20 + uOffset, texV);
        stack.popPose();

        stack.popPose();
    }

    // 元コードの drawTaperedBreastBox と基本的に同等だが内部的に少し別の順序で頂点を作る
    private void drawTaperedBox(PoseStack stack, VertexConsumer builder, int light,
                                float x, float y, float z,
                                float dx, float dy, float dz,
                                float taper, float texU, float texV) {

        Matrix4f posMat = stack.last().pose();
        org.joml.Matrix3f normalMat = stack.last().normal();
        float s = 1.0f / 16.0f;

        float minX = x * s;
        float maxX = (x + dx) * s;
        float minY = y * s;
        float maxY = (y + dy) * s;
        float minZTop = (z + taper) * s;
        float minZBot = z * s;
        float maxZ = (z + dz + 1.0f) * s;

        float uStart = texU;
        float vStart = texV;

        float vFrontTop = vStart + 4.0f;
        float vFrontBot = vStart + 4.0f + dy;

        // 東面（右）
        addQuad(builder, posMat, normalMat, light,
                maxX, minY, minZTop, uStart + 4 + dx, vFrontTop,
                maxX, maxY, minZBot, uStart + 4 + dx, vFrontBot,
                maxX, maxY, maxZ, uStart + 4 + dx + 4, vFrontBot,
                maxX, minY, maxZ, uStart + 4 + dx + 4, vFrontTop,
                1, 0, 0);

        // 西面（左）
        addQuad(builder, posMat, normalMat, light,
                minX, minY, maxZ, uStart, vFrontTop,
                minX, maxY, maxZ, uStart, vFrontBot,
                minX, maxY, minZBot, uStart + 4, vFrontBot,
                minX, minY, minZTop, uStart + 4, vFrontTop,
                -1, 0, 0);

        // 下（分割して描画、元コードと同等）
        float vDownFront = vFrontBot;
        float vDownBack = vFrontBot - 3.0f;

        addQuad(builder, posMat, normalMat, light,
                maxX, maxY, maxZ, uStart + 4 + dx, vDownBack,
                minX, maxY, maxZ, uStart + 4, vDownBack,
                minX, maxY, minZBot, uStart + 4, vDownFront,
                maxX, maxY, minZBot, uStart + 4 + dx, vDownFront,
                0, 1, 0);

        addQuad(builder, posMat, normalMat, light,
                maxX, maxY, minZBot, uStart + 4 + dx, vDownFront,
                minX, maxY, minZBot, uStart + 4, vDownFront,
                minX, maxY, maxZ, uStart + 4, vDownBack,
                maxX, maxY, maxZ, uStart + 4 + dx, vDownBack,
                0, -1, 0);

        // 上
        float vUpFront = vFrontTop;
        float vUpBack = vFrontTop - 3.0f;

        addQuad(builder, posMat, normalMat, light,
                maxX, minY, minZTop, uStart + 4 + dx, vUpFront,
                minX, minY, minZTop, uStart + 4, vUpFront,
                minX, minY, maxZ, uStart + 4, vUpBack,
                maxX, minY, maxZ, uStart + 4 + dx, vUpBack,
                0, -1, 0);

        // 前面（北）
        addQuad(builder, posMat, normalMat, light,
                maxX, minY, minZTop, uStart + 4 + dx, vFrontTop,
                minX, minY, minZTop, uStart + 4, vFrontTop,
                minX, maxY, minZBot, uStart + 4, vFrontBot,
                maxX, maxY, minZBot, uStart + 4 + dx, vFrontBot,
                0, 0, -1);
    }

    private void addQuad(VertexConsumer builder, Matrix4f mat, org.joml.Matrix3f nMat, int light,
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

    private void vertex(VertexConsumer builder, Matrix4f posMat, org.joml.Matrix3f normMat,
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