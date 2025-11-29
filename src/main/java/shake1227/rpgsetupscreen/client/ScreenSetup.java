package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;

public class ScreenSetup extends Screen {
    private int gender = 0;
    private float valW = 1.0f, valH = 1.0f, valC = 0.0f;
    private ForgeSlider sC;

    public ScreenSetup() { super(Component.translatable("gui.rpgsetupscreen.title")); }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        // 性別ボタン
        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.male"), b -> {
            gender = 0; sC.visible = false; updatePreview();
        }).bounds(cx - 105, cy - 60, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.female"), b -> {
            gender = 1; sC.visible = true; updatePreview();
        }).bounds(cx + 5, cy - 60, 100, 20).build());

        // スライダー
        this.addRenderableWidget(new ForgeSlider(cx - 100, cy - 30, 200, 20, Component.translatable("gui.rpgsetupscreen.width"), Component.empty(), 0.5, 1.5, 1.0, 0.01, 2, true) {
            @Override protected void applyValue() { valW = (float)getValue(); updatePreview(); }
        });
        this.addRenderableWidget(new ForgeSlider(cx - 100, cy - 5, 200, 20, Component.translatable("gui.rpgsetupscreen.height"), Component.empty(), 0.5, 1.5, 1.0, 0.01, 2, true) {
            @Override protected void applyValue() { valH = (float)getValue(); updatePreview(); }
        });

        sC = new ForgeSlider(cx - 100, cy + 20, 200, 20, Component.translatable("gui.rpgsetupscreen.chest"), Component.empty(), 0.0, 1.0, 0.0, 0.01, 2, true) {
            @Override protected void applyValue() { valC = (float)getValue(); updatePreview(); }
        };
        sC.visible = false;
        this.addRenderableWidget(sC);

        // 次へボタン
        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.next"), b -> {
            RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminGui(null));
        }).bounds(cx - 50, cy + 60, 100, 20).build());
    }

    private void updatePreview() {
        if(this.minecraft.player != null) {
            this.minecraft.player.getCapability(RPGCapability.INSTANCE).ifPresent(c -> {
                c.setGender(gender); c.setWidth(valW); c.setHeight(valH); c.setChest(valC);
            });
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);

        // 3Dプレビュー描画位置調整
        // x: 画面幅の1/5, y: 中心より少し下, scale: 70 (大きく), mouse追従
        int previewX = this.width / 5;
        int previewY = this.height / 2 + 80;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX, previewY, 70, (float)(previewX) - mx, (float)(previewY - 120) - my, this.minecraft.player);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}