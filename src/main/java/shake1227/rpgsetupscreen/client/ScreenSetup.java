package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;

import java.util.UUID;

public class ScreenSetup extends Screen {
    private int gender = 0;
    private float valW = 1.0f, valH = 1.0f, valC = 0.0f;
    private float valY = 0.0f, valSep = 0.0f, valAng = 0.0f;
    private ForgeSlider sC, sY, sSep, sAng;

    private String targetUUID = "";

    public ScreenSetup() { super(Component.translatable("gui.rpgsetupscreen.title")); }

    public void setEditTarget(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca) {
        this.targetUUID = targetId.toString();
        this.gender = g;
        this.valW = w;
        this.valH = h;
        this.valC = c;
        this.valY = cy;
        this.valSep = cs;
        this.valAng = ca;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.male"), b -> {
            gender = 0; setSlidersVisible(false); updatePreview();
        }).bounds(cx - 105, cy - 80, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.female"), b -> {
            gender = 1; setSlidersVisible(true); updatePreview();
        }).bounds(cx + 5, cy - 80, 100, 20).build());

        this.addRenderableWidget(new ForgeSlider(cx - 100, cy - 50, 200, 20, Component.translatable("gui.rpgsetupscreen.width"), Component.empty(), 0.5, 1.5, valW, 0.01, 2, true) {
            @Override protected void applyValue() { valW = (float)getValue(); updatePreview(); }
        });
        this.addRenderableWidget(new ForgeSlider(cx - 100, cy - 25, 200, 20, Component.translatable("gui.rpgsetupscreen.height"), Component.empty(), 0.5, 1.5, valH, 0.01, 2, true) {
            @Override protected void applyValue() { valH = (float)getValue(); updatePreview(); }
        });

        sC = new ForgeSlider(cx - 100, cy, 200, 20, Component.translatable("gui.rpgsetupscreen.chest"), Component.empty(), 0.0, 1.0, valC, 0.01, 2, true) {
            @Override protected void applyValue() { valC = (float)getValue(); updatePreview(); }
        };
        this.addRenderableWidget(sC);

        // --- 詳細設定 (可動域を制限) ---
        // Y位置: -0.1 (少し上) ~ 0.3 (お腹あたり)
        sY = new ForgeSlider(cx - 100, cy + 25, 200, 20, Component.literal("Chest Pos Y"), Component.empty(), -0.1, 0.3, valY, 0.01, 2, true) {
            @Override protected void applyValue() { valY = (float)getValue(); updatePreview(); }
        };
        this.addRenderableWidget(sY);

        // 間隔: 0.0 (密着) ~ 0.25 (腕の付け根あたり)
        sSep = new ForgeSlider(cx - 100, cy + 50, 200, 20, Component.literal("Chest Sep"), Component.empty(), 0.0, 0.25, valSep, 0.01, 2, true) {
            @Override protected void applyValue() { valSep = (float)getValue(); updatePreview(); }
        };
        this.addRenderableWidget(sSep);

        // 角度: 0.0 ~ 45.0度
        sAng = new ForgeSlider(cx - 100, cy + 75, 200, 20, Component.literal("Chest Angle"), Component.empty(), 0.0, 45.0, valAng, 0.1, 1, true) {
            @Override protected void applyValue() { valAng = (float)getValue(); updatePreview(); }
        };
        this.addRenderableWidget(sAng);

        setSlidersVisible(gender == 1);

        Component btnText = targetUUID.isEmpty() ? Component.translatable("gui.rpgsetupscreen.next") : Component.literal("Save");
        this.addRenderableWidget(Button.builder(btnText, b -> {
            if (targetUUID.isEmpty()) {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminGui(null));
            } else {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup("", gender, valW, valH, valC, valY, valSep, valAng, targetUUID));
                this.onClose();
            }
        }).bounds(cx - 50, cy + 105, 100, 20).build());

        updatePreview();
    }

    private void setSlidersVisible(boolean visible) {
        sC.visible = visible;
        sY.visible = visible;
        sSep.visible = visible;
        sAng.visible = visible;
    }

    private void updatePreview() {
        if(this.minecraft.player != null) {
            this.minecraft.player.getCapability(RPGCapability.INSTANCE).ifPresent(c -> {
                c.setGender(gender); c.setWidth(valW); c.setHeight(valH);
                c.setChest(valC); c.setChestY(valY); c.setChestSep(valSep); c.setChestAng(valAng);
            });
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);

        int previewX = this.width / 5;
        int previewY = this.height / 2 + 80;
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX, previewY, 70, (float)(previewX) - mx, (float)(previewY - 120) - my, this.minecraft.player);

        if (!targetUUID.isEmpty()) {
            g.drawCenteredString(this.font, "Editing Target Mode", this.width / 2, 20, 0xFF5555);
        }
    }

    @Override public boolean shouldCloseOnEsc() { return targetUUID.isEmpty(); }
}