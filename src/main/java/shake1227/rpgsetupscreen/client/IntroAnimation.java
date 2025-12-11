package shake1227.rpgsetupscreen.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

public class IntroAnimation {
    private static final ResourceLocation OPTIONS_BACKGROUND = new ResourceLocation("textures/gui/options_background.png");

    private static boolean active = false;
    private static boolean canSkip = true;
    private static long startTime;
    private static Runnable onComplete = null;

    private static RemotePlayer cameraDummy;
    private static Vec3 relativeCamOffset;
    private static float startYRot;
    private static float startXRot;
    private static Vec3 targetPos = null;

    private static double originalSensitivity;
    private static CameraType originalCameraType;

    private static final long DELAY_CURTAIN = 1000;
    private static final long DURATION_CURTAIN = 2500;
    private static final long DURATION_CAMERA = 15000;
    private static final long DURATION_WAIT = 3500;
    private static final long DURATION_TOTAL = DURATION_CAMERA + DURATION_WAIT;

    private static final long DELAY_TEXT = 12500;
    private static final long DURATION_TEXT_IN = 1000;
    private static final long DELAY_TEXT_OUT = 16500;
    private static final long DURATION_TEXT_OUT = 2000;

    private static final Random random = new Random();

    private static long pauseStart = 0;
    private static long totalPausedTime = 0;
    private static boolean wasPaused = false;
    private static boolean firstFrame = true;

    public static void start() { start(true, null); }
    public static void start(boolean skippable) { start(skippable, null); }

    public static void start(boolean skippable, Vec3 target) {
        stop();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        active = true;
        canSkip = skippable;
        targetPos = target;
        startTime = System.currentTimeMillis();
        totalPausedTime = 0;
        wasPaused = false;
        firstFrame = true;

        originalSensitivity = mc.options.sensitivity().get();
        originalCameraType = mc.options.getCameraType();

        mc.options.sensitivity().set(0.0);
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        cameraDummy = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "IntroCam"));
        cameraDummy.setInvisible(true);
        cameraDummy.setNoGravity(true);

        float playerYRot = mc.player.getYRot();

        double dist = 4.0;
        double offsetY = 0.5;

        double rad = Math.toRadians(playerYRot + 180);
        double offsetX = -Math.sin(rad) * dist;
        double offsetZ = Math.cos(rad) * dist;

        relativeCamOffset = new Vec3(offsetX, offsetY, offsetZ);

        startYRot = playerYRot + 150.0F;
        startXRot = -15.0F;

        updateDummyPosition(0);
        mc.setCameraEntity(cameraDummy);
    }

    public static void setOnComplete(Runnable callback) {
        onComplete = callback;
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPaused() {
        Minecraft mc = Minecraft.getInstance();
        return mc.getOverlay() instanceof LoadingOverlay;
    }

    public static void skip() {
        if (!active || !canSkip) return;
        stop();
    }

    public static boolean shouldHideHead() {
        if (!active) return false;
        long elapsed = System.currentTimeMillis() - startTime - totalPausedTime;
        float progress = (float)elapsed / (float)DURATION_CAMERA;
        return progress > 0.8f;
    }

    public static void stop() {
        active = false;
        targetPos = null;
        totalPausedTime = 0;
        wasPaused = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (mc.getCameraEntity() == cameraDummy) {
                mc.setCameraEntity(mc.player);
            }
        }
        cameraDummy = null;

        if (originalCameraType != null) {
            mc.options.sensitivity().set(originalSensitivity);
            mc.options.setCameraType(originalCameraType);
        }

        if (onComplete != null) {
            onComplete.run();
            onComplete = null;
        }
    }

    public static void update() {
        if (!active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cameraDummy == null) {
            stop();
            return;
        }

        if (mc.getCameraEntity() != cameraDummy) {
            mc.setCameraEntity(cameraDummy);
        }

        if (isPaused()) {
            if (!wasPaused) {
                pauseStart = System.currentTimeMillis();
                wasPaused = true;
            }
            mc.options.sensitivity().set(0.0);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            long pausedElapsed = pauseStart - startTime - totalPausedTime;
            updateDummyPosition(Math.max(0, pausedElapsed));
            return;
        } else {
            if (wasPaused) {
                totalPausedTime += (System.currentTimeMillis() - pauseStart);
                wasPaused = false;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime - totalPausedTime;
        if (elapsed > DURATION_TOTAL) {
            stop();
            return;
        }

        mc.options.sensitivity().set(0.0);
        mc.options.setCameraType(CameraType.FIRST_PERSON);

        updateDummyPosition(elapsed);
    }

    private static void updateDummyPosition(long elapsed) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || cameraDummy == null) return;

        float progress = Mth.clamp((float)elapsed / (float)DURATION_CAMERA, 0f, 1f);
        float t = progress < 0.5 ? 4 * progress * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 3) / 2;

        Vec3 endPos = targetPos != null ? targetPos : mc.player.getPosition(1.0f);

        double curX = endPos.x + (1 - t) * relativeCamOffset.x;
        double curY = endPos.y + (1 - t) * relativeCamOffset.y;
        double curZ = endPos.z + (1 - t) * relativeCamOffset.z;

        float endYRot = mc.player.getYRot();
        float endXRot = mc.player.getXRot();

        float curYRot = Mth.rotLerp(t, startYRot, endYRot);
        float curXRot = Mth.rotLerp(t, startXRot, endXRot);

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

        if (firstFrame) {
            startTime = System.currentTimeMillis();
            firstFrame = false;
        }

        long current = System.currentTimeMillis();
        long elapsed;
        if (wasPaused) {
            elapsed = pauseStart - startTime - totalPausedTime;
        } else {
            elapsed = current - startTime - totalPausedTime;
        }
        elapsed = Math.max(0, elapsed);

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        if (canSkip) {
            g.drawString(Minecraft.getInstance().font, Component.translatable("gui.rpgsetupscreen.skip"), 10, 10, 0xFFFFFFFF, true);
        }

        int halfHeight = height / 2;
        int topBarBottomY = halfHeight;
        int bottomBarTopY = halfHeight;

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (elapsed < DELAY_CURTAIN) {
            drawTiledBackground(g, 0, 0, width, height);
        } else {
            float curtainTime = elapsed - DELAY_CURTAIN;
            float curtainProgress = Mth.clamp(curtainTime / (float)DURATION_CURTAIN, 0f, 1f);
            float x = curtainProgress;
            float ease = (float) (Math.pow(x, 5) / (Math.pow(x, 5) + Math.pow(1 - x, 5)));

            int openAmount = (int)(halfHeight * ease);

            topBarBottomY = halfHeight - openAmount;
            bottomBarTopY = halfHeight + openAmount;

            if (topBarBottomY > 0) {
                drawTiledBackground(g, 0, 0, width, topBarBottomY);
            }
            if (bottomBarTopY < height) {
                drawTiledBackground(g, 0, bottomBarTopY, width, height - bottomBarTopY);
            }
        }

        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (elapsed >= DELAY_TEXT && elapsed < DELAY_TEXT_OUT + DURATION_TEXT_OUT) {
            float alpha = 1.0f;
            if (elapsed < DELAY_TEXT + DURATION_TEXT_IN) {
                alpha = (float)(elapsed - DELAY_TEXT) / DURATION_TEXT_IN;
            }
            else if (elapsed > DELAY_TEXT_OUT) {
                alpha = 1.0f - (float)(elapsed - DELAY_TEXT_OUT) / DURATION_TEXT_OUT;
            }
            alpha = Mth.clamp(alpha, 0f, 1f);

            if (alpha > 0.05f) {
                int cx = width / 2;
                int cy = height / 2;

                float glitchIntensity;
                if (elapsed > DELAY_TEXT_OUT) {
                    float fadeProgress = 1.0f - alpha;
                    glitchIntensity = 0.5f + (fadeProgress * fadeProgress * 3.5f);
                }
                else if (elapsed < DELAY_TEXT + 800) {
                    glitchIntensity = 1.5f;
                }
                else if (random.nextInt(40) == 0) {
                    glitchIntensity = 0.6f;
                } else {
                    glitchIntensity = 0.05f;
                }

                drawCustomGlitchText(g, "WELCOME", cx, cy - 30, 3.0f, alpha, glitchIntensity);

                String name = Minecraft.getInstance().player.getName().getString();
                drawCustomGlitchText(g, name, cx, cy + 10, 4.0f, alpha, glitchIntensity * 1.2f);
            }
        }

        g.pose().popPose();
    }

    private static void drawTiledBackground(GuiGraphics g, int x, int y, int width, int height) {
        g.setColor(0.25F, 0.25F, 0.25F, 1.0F);
        int tileSize = 32;
        for (int dx = 0; dx < width; dx += tileSize) {
            for (int dy = 0; dy < height; dy += tileSize) {
                int drawW = Math.min(tileSize, width - dx);
                int drawH = Math.min(tileSize, height - dy);
                g.blit(OPTIONS_BACKGROUND, x + dx, y + dy, 0, 0, drawW, drawH, 32, 32);
            }
        }
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawCustomGlitchText(GuiGraphics g, String text, int x, int y, float scale, float baseAlpha, float intensity) {
        int alphaInt = (int)(baseAlpha * 255);
        if (alphaInt <= 5) return;

        float shakeX = (random.nextFloat() - 0.5f) * 10.0f * intensity;
        float shakeY = (random.nextFloat() - 0.5f) * 5.0f * intensity;

        g.pose().pushPose();
        g.pose().translate(x + shakeX, y + shakeY, 0);

        float scaleJitter = 1.0f;
        if (intensity > 1.5f) {
            scaleJitter = 1.0f + (random.nextFloat() - 0.5f) * 0.1f * intensity;
        }
        g.pose().scale(scale * scaleJitter, scale * scaleJitter, 1.0f);

        Component comp = Component.literal(text);
        int w = Minecraft.getInstance().font.width(comp);
        int offsetX = -w / 2;
        int offsetY = -Minecraft.getInstance().font.lineHeight / 2;

        if (intensity > 0.1f) {
            if (random.nextBoolean()) {
                float offX = (random.nextFloat() * 4.0f + 1.0f) * intensity;
                g.drawString(Minecraft.getInstance().font, comp, (int)(offsetX - offX), offsetY, (alphaInt << 24) | 0xFFFF0000, false);
            }
            if (random.nextBoolean()) {
                float offX = (random.nextFloat() * 4.0f + 1.0f) * intensity;
                g.drawString(Minecraft.getInstance().font, comp, (int)(offsetX + offX), offsetY, (alphaInt << 24) | 0xFF00FFFF, false);
            }
        }

        if (intensity < 2.0f || random.nextInt(3) != 0) {
            g.drawString(Minecraft.getInstance().font, comp, offsetX, offsetY, (alphaInt << 24) | 0xFFFFFFFF, false);
        }

        g.pose().popPose();

        int noiseCount = (intensity > 1.0f) ? (int)intensity + 1 : 1;

        for (int i = 0; i < noiseCount; i++) {
            if (intensity > 0.2f && random.nextInt(4) == 0) {
                int noiseW = (int)(random.nextInt(w + 30) * scale);
                int noiseH = (int)(random.nextInt(4) * scale);
                int noiseX = x - noiseW / 2 + (int)((random.nextFloat() - 0.5f) * 50 * intensity);
                int noiseY = y + (int)((random.nextFloat() - 0.5f) * 50 * intensity);

                int noiseAlpha = (int)(alphaInt * (0.5f + random.nextFloat() * 0.5f));
                g.fill(noiseX, noiseY, noiseX + noiseW, noiseY + noiseH, (noiseAlpha << 24) | 0xFFEEEEEE);
            }
        }
    }
}