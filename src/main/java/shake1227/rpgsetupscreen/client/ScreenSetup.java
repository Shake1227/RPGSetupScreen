package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.client.gui.widget.ForgeSlider;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCapability;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScreenSetup extends Screen {
    private int gender = 0;
    private float valW = 1.0f, valH = 1.0f, valC = 0.0f;
    private float valY = 0.0f, valSep = 0.0f, valAng = 0.0f;
    private boolean physicsEnabled = true;

    private ForgeSlider sW, sH, sC, sY, sSep, sAng;
    private Button btnMale, btnFemale, btnPhysics;

    private String targetUUID = "";
    private RemotePlayer previewPlayer;

    private long openTime;
    private static final int ANIM_DURATION = 600;
    private final List<Integer> initialYPositions = new ArrayList<>();

    private int titleY = 20;
    private boolean fromFloppy = false;

    public ScreenSetup() { super(Component.translatable("gui.rpgsetupscreen.title")); }

    public void setFromFloppy(boolean fromFloppy) { this.fromFloppy = fromFloppy; }

    public void setEditTarget(UUID targetId, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        this.targetUUID = targetId.toString();
        loadDefaults(g, w, h, c, cy, cs, ca, physics);
    }

    public void loadDefaults(int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        this.gender = g;
        this.valW = w;
        this.valH = h;
        this.valC = c;
        this.valY = cy;
        this.valSep = cs;
        this.valAng = ca;
        this.physicsEnabled = physics;
    }

    private float getForcedScale() {
        return 3.0f / (float)this.minecraft.getWindow().getGuiScale();
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        this.initialYPositions.clear();

        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);
        int vHeight = (int)(this.height / scale);

        int cx = vWidth / 2;
        int cy = vHeight / 2;

        if (this.minecraft != null && this.minecraft.level != null) {
            this.previewPlayer = new RemotePlayer(this.minecraft.level, this.minecraft.player.getGameProfile()) {
                @Override public boolean isSpectator() { return false; }
                @Override public boolean isModelPartShown(PlayerModelPart part) { return true; }
            };
            this.previewPlayer.setInvisible(false);
            this.previewPlayer.yBodyRot = 0.0f;
            this.previewPlayer.yHeadRot = 0.0f;
            this.previewPlayer.setYRot(0.0f);
        }

        btnMale = Button.builder(Component.translatable("gui.rpgsetupscreen.male"), b -> {
            gender = 0; setSlidersVisible(false); updatePreview(); refreshAllSliderMessages();
        }).bounds(cx - 105, cy - 80, 100, 20).build();
        if (!ClientConfigCache.enableGender) btnMale.active = false;
        this.addRenderableWidget(btnMale);

        btnFemale = Button.builder(Component.translatable("gui.rpgsetupscreen.female"), b -> {
            gender = 1; setSlidersVisible(true); updatePreview(); refreshAllSliderMessages();
        }).bounds(cx + 5, cy - 80, 100, 20).build();
        if (!ClientConfigCache.enableGender) btnFemale.active = false;
        this.addRenderableWidget(btnFemale);

        sW = new ForgeSlider(cx - 100, cy - 50, 200, 20, Component.translatable("gui.rpgsetupscreen.width"), Component.empty(), 0.5, 1.5, valW, 0.01, 2, true) {
            @Override protected void applyValue() { valW = (float)getValue(); updatePreview(); }
            @Override protected void updateMessage() { setMessage(getSliderMessage("gui.rpgsetupscreen.width", valW)); }
        };
        if (!ClientConfigCache.enableWidth) sW.active = false;
        this.addRenderableWidget(sW);

        sH = new ForgeSlider(cx - 100, cy - 25, 200, 20, Component.translatable("gui.rpgsetupscreen.height"), Component.empty(), 0.5, 1.5, valH, 0.01, 2, true) {
            @Override protected void applyValue() { valH = (float)getValue(); updatePreview(); refreshAllSliderMessages(); }
            @Override protected void updateMessage() { setMessage(getSliderMessage("gui.rpgsetupscreen.height", valH)); }
        };
        if (!ClientConfigCache.enableHeight) sH.active = false;
        this.addRenderableWidget(sH);

        sC = new ForgeSlider(cx - 100, cy, 200, 20, Component.translatable("gui.rpgsetupscreen.chest"), Component.empty(), 0.0, 1.0, valC, 0.01, 2, true) {
            @Override protected void applyValue() { valC = (float)getValue(); updatePreview(); }
            @Override protected void updateMessage() { setMessage(getSliderMessage("gui.rpgsetupscreen.chest", valC)); }
        };
        if (!ClientConfigCache.enableChest) sC.active = false;
        this.addRenderableWidget(sC);

        sY = new ForgeSlider(cx - 100, cy + 25, 200, 20, Component.translatable("gui.rpgsetupscreen.chest_y"), Component.empty(), -0.1, 0.3, valY, 0.01, 2, true) {
            @Override protected void applyValue() { valY = (float)getValue(); updatePreview(); }
        };
        if (!ClientConfigCache.enableChestY) sY.active = false;
        this.addRenderableWidget(sY);

        sSep = new ForgeSlider(cx - 100, cy + 50, 200, 20, Component.translatable("gui.rpgsetupscreen.chest_sep"), Component.empty(), 0.0, 0.25, valSep, 0.01, 2, true) {
            @Override protected void applyValue() { valSep = (float)getValue(); updatePreview(); }
        };
        if (!ClientConfigCache.enableChestSep) sSep.active = false;
        this.addRenderableWidget(sSep);

        sAng = new ForgeSlider(cx - 100, cy + 75, 200, 20, Component.translatable("gui.rpgsetupscreen.chest_ang"), Component.empty(), 0.0, 45.0, valAng, 0.1, 1, true) {
            @Override protected void applyValue() { valAng = (float)getValue(); updatePreview(); }
        };
        if (!ClientConfigCache.enableChestAng) sAng.active = false;
        this.addRenderableWidget(sAng);

        btnPhysics = Button.builder(Component.translatable("gui.rpgsetupscreen.physics", physicsEnabled ? "ON" : "OFF"), b -> {
            physicsEnabled = !physicsEnabled;
            b.setMessage(Component.translatable("gui.rpgsetupscreen.physics", physicsEnabled ? "ON" : "OFF"));
            updatePreview();
        }).bounds(cx + 105, cy, 80, 20).build();
        if (!ClientConfigCache.enablePhysics) btnPhysics.active = false;
        this.addRenderableWidget(btnPhysics);

        setSlidersVisible(gender == 1);
        refreshAllSliderMessages();

        Component btnText;
        if (fromFloppy) btnText = Component.translatable("gui.rpgsetupscreen.done");
        else btnText = targetUUID.isEmpty() ? Component.translatable("gui.rpgsetupscreen.next") : Component.translatable("gui.rpgsetupscreen.save");

        this.addRenderableWidget(Button.builder(btnText, b -> {
            b.active = false;
            ClientSettingsCache.save(gender, valW, valH, valC, valY, valSep, valAng, physicsEnabled);

            if (fromFloppy) {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup("", gender, valW, valH, valC, valY, valSep, valAng, physicsEnabled, targetUUID, new net.minecraft.nbt.CompoundTag(), true));
                this.onClose();
                return;
            }

            if (targetUUID.isEmpty()) {
                int myIndex = -1;
                for(int i=0; i<ClientHooks.screenDefs.size(); i++) {
                    if(ClientHooks.screenDefs.get(i).uuid.equals("setup")) { myIndex = i; break; }
                }
                ClientHooks.setPendingSetupData(gender, valW, valH, valC, valY, valSep, valAng, physicsEnabled);
                ClientHooks.openScreen(myIndex + 1);
            } else {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup("", gender, valW, valH, valC, valY, valSep, valAng, physicsEnabled, targetUUID));
                this.onClose();
            }
        }).bounds(cx - 50, cy + 105, 100, 20).build());

        updatePreview();

        for (var w : this.renderables) {
            if (w instanceof AbstractWidget aw) {
                initialYPositions.add(aw.getY());
            }
        }
    }

    private void refreshAllSliderMessages() {
        if (sW != null) sW.setMessage(getSliderMessage("gui.rpgsetupscreen.width", valW));
        if (sH != null) sH.setMessage(getSliderMessage("gui.rpgsetupscreen.height", valH));
        if (sC != null) sC.setMessage(getSliderMessage("gui.rpgsetupscreen.chest", valC));
    }

    private Component getSliderMessage(String key, float value) {
        String msg;
        float baseHeightCm = 160.0f;
        float currentHeightCm = baseHeightCm * valH;

        if (key.equals("gui.rpgsetupscreen.height")) {
            msg = String.format("%.1fcm", currentHeightCm);
        } else if (key.equals("gui.rpgsetupscreen.width")) {
            msg = String.format("%.2fx (Est W: %.1fcm)", value, (60.0f * value * valH));
        } else if (key.equals("gui.rpgsetupscreen.chest")) {
            if (gender == 1) {
                float underBust = currentHeightCm * 0.43f * valW;
                float topBust = underBust + 5.0f + (value * 25.0f);
                float diff = topBust - underBust;
                String cup = getCupSize(diff);
                msg = String.format("B: %.1fcm %s", topBust, cup);
            } else {
                msg = String.format("%.2f", value);
            }
        } else {
            msg = String.format("%.2f", value);
        }
        return Component.translatable(key).append(": " + msg);
    }

    private String getCupSize(float diff) {
        if (diff < 7.5f) return "(AA)";
        if (diff < 10.0f) return "(A)";
        if (diff < 12.5f) return "(B)";
        if (diff < 15.0f) return "(C)";
        if (diff < 17.5f) return "(D)";
        if (diff < 20.0f) return "(E)";
        if (diff < 22.5f) return "(F)";
        if (diff < 25.0f) return "(G)";
        if (diff < 27.5f) return "(H)";
        return "(I+)";
    }

    private void setSlidersVisible(boolean visible) {
        if(sC != null) sC.visible = visible;
        if(sY != null) sY.visible = visible;
        if(sSep != null) sSep.visible = visible;
        if(sAng != null) sAng.visible = visible;
        if(btnPhysics != null) btnPhysics.visible = visible;
    }

    private void updatePreview() {
        if(this.previewPlayer != null) {
            this.previewPlayer.getCapability(RPGCapability.INSTANCE).ifPresent(c -> {
                c.setGender(gender); c.setWidth(valW); c.setHeight(valH);
                c.setChest(valC); c.setChestY(valY); c.setChestSep(valSep); c.setChestAng(valAng);
                c.setPhysicsEnabled(physicsEnabled);
            });
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (this.minecraft.options.keyInventory.matches(btn, 0)) return true;
        float scale = getForcedScale();
        return super.mouseClicked(mx / scale, my / scale, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        float scale = getForcedScale();
        return super.mouseReleased(mx / scale, my / scale, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        float scale = getForcedScale();
        return super.mouseDragged(mx / scale, my / scale, btn, dx / scale, dy / scale);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) return true;
        if (keyCode == 256 && !this.shouldCloseOnEsc()) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (fromFloppy) {
            this.renderBackground(g);
        } else if (targetUUID.isEmpty()) {
            this.renderDirtBackground(g);
        } else {
            g.fill(0, 0, this.width, this.height, 0xAA000000);
        }

        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);
        int vHeight = (int)(this.height / scale);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        LogoRenderer.render(g, vWidth);

        Component titleComp = targetUUID.isEmpty() ? this.title : Component.translatable("gui.rpgsetupscreen.editing_target", targetUUID);
        g.drawCenteredString(this.font, titleComp, vWidth / 2, this.titleY, 0xFFFFFF);

        float t = Mth.clamp((System.currentTimeMillis() - openTime) / (float)ANIM_DURATION, 0f, 1f);

        int widgetIndex = 0;
        for (var r : this.renderables) {
            if (r instanceof AbstractWidget w && widgetIndex < initialYPositions.size()) {
                float delayT = Mth.clamp((System.currentTimeMillis() - openTime - (widgetIndex * 30L)) / (float)ANIM_DURATION, 0f, 1f);
                float itemEase = backOut(delayT);
                int targetY = initialYPositions.get(widgetIndex);
                int startOffsetY = 30;
                w.setY((int) (targetY + startOffsetY * (1 - itemEase)));
                w.setAlpha(itemEase);
                widgetIndex++;
            }
        }

        super.render(g, (int)(mx / scale), (int)(my / scale), pt);

        if (targetUUID.isEmpty()) {
            g.drawCenteredString(this.font, Component.translatable("gui.rpgsetupscreen.warning"), vWidth / 2, vHeight - 30, 0xFF5555);
        }

        int previewX = vWidth / 5;
        int previewY = vHeight / 2 + 50;
        int previewScale = 60;

        if (this.previewPlayer != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, previewX, previewY, previewScale, (float)(previewX) - (float)(mx/scale), (float)(previewY - 120) - (float)(my/scale), this.previewPlayer);
        }

        g.pose().popPose();
    }

    private float backOut(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    @Override public boolean shouldCloseOnEsc() { return !targetUUID.isEmpty() || fromFloppy; }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}