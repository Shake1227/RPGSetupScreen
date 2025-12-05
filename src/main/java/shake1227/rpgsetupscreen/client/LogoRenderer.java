package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;
import shake1227.rpgsetupscreen.RPGSetupScreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class LogoRenderer {
    private static final ResourceLocation DYNAMIC_LOGO_LOC = new ResourceLocation(RPGSetupScreen.MODID, "dynamic_logo");
    private static boolean hasLogo = false;
    private static int imgWidth, imgHeight;
    private static boolean initialized = false;

    public static final int DRAW_WIDTH = 200;

    public static void init() {
        if (initialized) return;
        initialized = true;

        File file = FMLPaths.CONFIGDIR.get().resolve("rpgscreen_logo.png").toFile();

        if (!file.exists()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                File worldConfig = mc.getSingleplayerServer().getWorldPath(new LevelResource("serverconfig")).resolve("rpgscreen_logo.png").toFile();
                if (worldConfig.exists()) {
                    file = worldConfig;
                }
            }
        }

        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                NativeImage img = NativeImage.read(is);
                imgWidth = img.getWidth();
                imgHeight = img.getHeight();
                DynamicTexture tex = new DynamicTexture(img);

                Minecraft.getInstance().getTextureManager().register(DYNAMIC_LOGO_LOC, tex);
                hasLogo = true;
                System.out.println("[RPGSetupScreen] Logo loaded. Size: " + imgWidth + "x" + imgHeight);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static int getLogoHeight() {
        init();
        if (!hasLogo || imgWidth == 0) return 0;
        return (int)((float)imgHeight / imgWidth * DRAW_WIDTH);
    }

    public static void render(GuiGraphics g, int screenWidth) {
        init();
        if (hasLogo) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, DYNAMIC_LOGO_LOC);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int drawH = getLogoHeight();
            int x = screenWidth - DRAW_WIDTH - 20;
            int y = 20;

            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);

            // Draw with correct texture coordinates (0,0 to width,height)
            g.blit(DYNAMIC_LOGO_LOC, x, y, DRAW_WIDTH, drawH, 0.0F, 0.0F, imgWidth, imgHeight, imgWidth, imgHeight);

            g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }
}