package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import shake1227.rpgsetupscreen.RPGSetupScreen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LogoRenderer {
    private static final ResourceLocation DYNAMIC_LOGO_LOC = new ResourceLocation(RPGSetupScreen.MODID, "dynamic_logo");
    private static boolean hasLogo = false;
    private static int imgWidth, imgHeight;

    public static final int DRAW_WIDTH = 200;

    public static void setLogoData(byte[] data) {
        if (data == null || data.length == 0) return;

        Minecraft.getInstance().execute(() -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                NativeImage img = NativeImage.read(is);
                imgWidth = img.getWidth();
                imgHeight = img.getHeight();
                DynamicTexture tex = new DynamicTexture(img);

                Minecraft.getInstance().getTextureManager().register(DYNAMIC_LOGO_LOC, tex);
                hasLogo = true;
            } catch (IOException e) {
                e.printStackTrace();
                hasLogo = false;
            }
        });
    }

    public static int getLogoHeight() {
        if (!hasLogo || imgWidth == 0) return 0;
        return (int)((float)imgHeight / imgWidth * DRAW_WIDTH);
    }

    public static void render(GuiGraphics g, int screenWidth) {
        if (hasLogo) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, DYNAMIC_LOGO_LOC);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int drawH = getLogoHeight();
            int x = screenWidth - DRAW_WIDTH - 20;
            int y = 20;

            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            g.blit(DYNAMIC_LOGO_LOC, x, y, 0, 0, DRAW_WIDTH, drawH, DRAW_WIDTH, drawH);
            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}