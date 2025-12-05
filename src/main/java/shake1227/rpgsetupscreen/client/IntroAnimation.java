package shake1227.rpgsetupscreen.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

public class IntroAnimation {
    private static boolean active = false;
    private static long startTime;

    private static RemotePlayer cameraDummy;
    private static Vec3 startCamPos;
    private static float startYRot;
    private static float startXRot;

    // 設定保存用
    private static double originalSensitivity;
    private static boolean originalHideGui;
    private static CameraType originalCameraType;

    // 演出設定 (ミリ秒)
    private static final long DELAY_BARS = 500;
    private static final long DURATION_BARS = 2000;

    private static final long DELAY_TEXT = 3000;
    private static final long DURATION_TEXT_IN = 500;
    private static final long DELAY_TEXT_OUT = 11000;
    private static final long DURATION_TEXT_OUT = 500;

    private static final long DURATION_CAMERA = 13000;
    private static final long DURATION_WAIT = 1000;
    private static final long DURATION_TOTAL = DURATION_CAMERA + DURATION_WAIT;

    private static final Random random = new Random();

    public static void start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (active) return;

        active = true;
        startTime = System.currentTimeMillis();

        // 設定保存 & ロック
        originalSensitivity = mc.options.sensitivity().get();
        originalHideGui = mc.options.hideGui;
        originalCameraType = mc.options.getCameraType();

        mc.options.sensitivity().set(0.0);
        mc.options.hideGui = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON); // 一人称固定

        // カメラ用ダミー作成
        cameraDummy = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "IntroCam"));
        cameraDummy.setInvisible(true);
        cameraDummy.setNoGravity(true);

        // カメラ開始位置 (プレイヤーの背後上空)
        float playerYRot = mc.player.getYRot();
        double rad = Math.toRadians(playerYRot + 160);
        double dist = 15.0;
        double offsetX = -Math.sin(rad) * dist;
        double offsetZ = Math.cos(rad) * dist;
        double offsetY = 8.0;

        startCamPos = mc.player.getPosition(1.0f).add(offsetX, offsetY, offsetZ);

        // プレイヤーを見る向き
        Vec3 playerEye = mc.player.getEyePosition(1.0f);
        Vec3 diff = playerEye.subtract(startCamPos);
        double d0 = diff.horizontalDistance();
        startYRot = (float)(Mth.atan2(diff.z, diff.x) * (double)(180F / (float)Math.PI)) - 90.0F;
        startXRot = (float)(-(Mth.atan2(diff.y, d0) * (double)(180F / (float)Math.PI)));

        updateDummyPosition(0);
        mc.setCameraEntity(cameraDummy);
    }

    public static boolean isActive() {
        return active;
    }

    public static void stop() {
        if (!active) return;
        active = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        cameraDummy = null;

        // 設定復元
        mc.options.sensitivity().set(originalSensitivity);
        mc.options.hideGui = originalHideGui;
        mc.options.setCameraType(originalCameraType);
    }

    // 毎フレーム呼び出し (RenderTick)
    public static void update() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cameraDummy == null) {
            stop();
            return;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > DURATION_TOTAL) {
            stop();
            return;
        }

        // 強制設定 (F5対策)
        mc.options.sensitivity().set(0.0);
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        updateDummyPosition(elapsed);
    }

    private static void updateDummyPosition(long elapsed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float progress = Mth.clamp((float)elapsed / (float)DURATION_CAMERA, 0f, 1f);
        // イージング
        float t = progress == 1.0f ? 1.0f : 1.0f - (float)Math.pow(2, -10 * progress);

        Vec3 endPos = mc.player.getEyePosition(1.0f);
        double curX = Mth.lerp(t, startCamPos.x, endPos.x);
        double curY = Mth.lerp(t, startCamPos.y, endPos.y);
        double curZ = Mth.lerp(t, startCamPos.z, endPos.z);

        float endYRot = mc.player.getYRot();
        float endXRot = mc.player.getXRot();
        float curYRot = Mth.rotLerp(t, startYRot, endYRot);
        float curXRot = Mth.rotLerp(t, startXRot, endXRot);

        // 位置・回転をセット (補間無効化のためprevも同じ値にする)
        cameraDummy.setPos(curX, curY, curZ);
        cameraDummy.xo = curX; cameraDummy.yo = curY; cameraDummy.zo = curZ;

        cameraDummy.setYRot(curYRot);
        cameraDummy.yRotO = curYRot;

        cameraDummy.setXRot(curXRot);
        cameraDummy.xRotO = curXRot;

        cameraDummy.yHeadRot = curYRot;
        cameraDummy.yHeadRotO = curYRot;
    }

    public static void render(GuiGraphics g, int width, int height) {
        if (!active) return;

        long elapsed = System.currentTimeMillis() - startTime;

        // 手前に描画
        g.pose().pushPose();
        g.pose().translate(0, 0, 600);

        // 1. 黒帯
        float barProgress = 0f;
        if (elapsed > DELAY_BARS) {
            barProgress = Mth.clamp((float)(elapsed - DELAY_BARS) / (float)DURATION_BARS, 0f, 1f);
            float f = 1 - barProgress;
            barProgress = 1 - f * f * f;
        }

        int maxBarHeight = height / 2;
        int currentBarHeight = (int)(maxBarHeight * (1.0f - barProgress));

        if (currentBarHeight > 0) {
            g.fill(0, 0, width, currentBarHeight, 0xFF000000);
            g.fill(0, height - currentBarHeight, width, height, 0xFF000000);
        }

        // 2. グリッチテキスト
        if (elapsed >= DELAY_TEXT && elapsed < DELAY_TEXT_OUT + DURATION_TEXT_OUT) {
            float alpha = 1.0f;
            if (elapsed < DELAY_TEXT + DURATION_TEXT_IN) {
                alpha = (float)(elapsed - DELAY_TEXT) / DURATION_TEXT_IN;
            } else if (elapsed > DELAY_TEXT_OUT) {
                alpha = 1.0f - (float)(elapsed - DELAY_TEXT_OUT) / DURATION_TEXT_OUT;
            }
            alpha = Mth.clamp(alpha, 0f, 1f);

            if (alpha > 0.05f) {
                int cx = width / 2;
                int cy = height / 2;

                // グリッチ強度計算
                float glitchIntensity = 0.0f;
                // 出現・消失時は激しく
                if (elapsed < DELAY_TEXT + 800 || elapsed > DELAY_TEXT_OUT - 500) {
                    glitchIntensity = 1.0f;
                } else if (random.nextInt(50) == 0) { // たまにノイズ
                    glitchIntensity = 0.3f;
                }

                // WELCOME
                drawCustomGlitchText(g, "WELCOME", cx, cy - 40, 5.0f, alpha, glitchIntensity);

                // MCID
                String name = Minecraft.getInstance().player.getName().getString();
                drawCustomGlitchText(g, name, cx, cy + 20, 2.5f, alpha, glitchIntensity);
            }
        }

        g.pose().popPose();
    }

    // カスタムグリッチテキストレンダラー
    private static void drawCustomGlitchText(GuiGraphics g, String text, int x, int y, float scale, float baseAlpha, float intensity) {
        if (intensity <= 0.01f) {
            // 通常描画 (影なし、白)
            drawScaledText(g, text, x, y, scale, 0xFFFFFFFF, (int)(baseAlpha * 255));
            return;
        }

        int alpha = (int)(baseAlpha * 255);

        // ランダムなズレ
        float shiftX = (random.nextFloat() - 0.5f) * 10.0f * intensity;
        float shiftY = (random.nextFloat() - 0.5f) * 5.0f * intensity;

        // 1. 赤レイヤー (左にズレる)
        if (random.nextBoolean()) {
            int redAlpha = (int)(alpha * (0.5f + random.nextFloat() * 0.5f));
            drawScaledText(g, text, (int)(x - shiftX - 2), (int)(y - shiftY), scale, 0xFFFF0000, redAlpha);
        }

        // 2. 青レイヤー (右にズレる)
        if (random.nextBoolean()) {
            int blueAlpha = (int)(alpha * (0.5f + random.nextFloat() * 0.5f));
            drawScaledText(g, text, (int)(x + shiftX + 2), (int)(y + shiftY), scale, 0xFF0000FF, blueAlpha);
        }

        // 3. 白メインレイヤー (たまに消える/点滅)
        if (random.nextFloat() > intensity * 0.3f) {
            drawScaledText(g, text, (int)(x + shiftX/2), (int)(y + shiftY/2), scale, 0xFFFFFFFF, alpha);
        }

        // 4. ランダムな矩形ノイズ (文字の上に適当な白い棒を描画してグリッチ感を出す)
        if (random.nextFloat() < intensity * 0.5f) {
            int barW = (int)(random.nextInt(100) * scale);
            int barH = (int)(random.nextInt(5) * scale);
            int barX = x - barW / 2 + (int)((random.nextFloat() - 0.5f) * 50);
            int barY = y + (int)((random.nextFloat() - 0.5f) * 20);
            g.fill(barX, barY, barX + barW, barY + barH, (alpha << 24) | 0xFFFFFF);
        }
    }

    private static void drawScaledText(GuiGraphics g, String text, int x, int y, float scale, int colorRGB, int alpha) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);

        Component comp = Component.literal(text);
        int w = Minecraft.getInstance().font.width(comp);
        // 中央揃えのためのオフセット
        int offsetX = -w / 2;
        int offsetY = -Minecraft.getInstance().font.lineHeight / 2;

        // 色にアルファを適用
        int color = (alpha << 24) | (colorRGB & 0x00FFFFFF);

        // 影なしで描画
        g.drawString(Minecraft.getInstance().font, comp, offsetX, offsetY, color, false);

        g.pose().popPose();
    }
}