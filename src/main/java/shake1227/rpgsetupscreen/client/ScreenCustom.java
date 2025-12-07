package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import shake1227.rpgsetupscreen.data.ScreenData;

import java.util.ArrayList;
import java.util.List;

public class ScreenCustom extends Screen {
    private final ScreenData.Def def;
    private final int myIndex;
    private final List<EditBox> inputs = new ArrayList<>();
    private Button nextButton;

    private long openTime;
    private static final int ANIM_DURATION = 600;
    private final List<Integer> initialYPositions = new ArrayList<>();

    public ScreenCustom(ScreenData.Def def, int index) {
        super(Component.literal(def.title));
        this.def = def;
        this.myIndex = index;
    }

    private float getForcedScale() {
        return 3.0f / (float)this.minecraft.getWindow().getGuiScale();
    }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        this.initialYPositions.clear();
        inputs.clear();

        float scale = getForcedScale();
        int w = (int)(this.width / scale);
        int h = (int)(this.height / scale);

        for (ScreenData.Element e : def.elements) {
            if (e.type.equals("input")) {
                EditBox box = new EditBox(this.font, e.x, e.y, e.w, e.h, Component.literal(""));
                if (e.content != null && !e.content.isEmpty()) {
                    box.setHint(Component.literal(e.content));
                }

                if (ClientHooks.accumulatedInputs.contains(e.varName)) {
                    box.setValue(ClientHooks.accumulatedInputs.getString(e.varName));
                }

                box.setResponder(val -> {
                    if (!e.varName.isEmpty()) {
                        ClientHooks.accumulatedInputs.putString(e.varName, val);
                    }
                    checkRequiredFields();
                });
                this.addRenderableWidget(box);
                inputs.add(box);
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.back"), b -> {
            ClientHooks.openScreen(myIndex - 1);
        }).bounds(10, h - 30, 80, 20).build());

        this.addRenderableWidget(nextButton = Button.builder(Component.translatable("gui.rpgsetupscreen.next"), b -> {
            b.active = false;
            ClientHooks.openScreen(myIndex + 1);
        }).bounds(w - 110, h - 30, 100, 20).build());

        checkRequiredFields();

        for (var widget : this.renderables) {
            if (widget instanceof AbstractWidget aw) {
                initialYPositions.add(aw.getY());
            }
        }
    }

    private void checkRequiredFields() {
        boolean allValid = true;
        int inputIdx = 0;
        for (ScreenData.Element e : def.elements) {
            if (e.type.equals("input")) {
                if (e.required) {
                    EditBox box = inputs.get(inputIdx);
                    if (box.getValue().trim().isEmpty()) {
                        allValid = false;
                    }
                }
                inputIdx++;
            }
        }
        if (nextButton != null) {
            nextButton.active = allValid;
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) { float s = getForcedScale(); return super.mouseClicked(mx/s, my/s, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { float s = getForcedScale(); return super.mouseReleased(mx/s, my/s, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { float s = getForcedScale(); return super.mouseDragged(mx/s, my/s, btn, dx/s, dy/s); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (this.minecraft.options.keyInventory.matches(keyCode, scanCode)) return true; return super.keyPressed(keyCode, scanCode, modifiers); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderDirtBackground(g);

        float scale = getForcedScale();
        int scaledMx = (int)(mx / scale);
        int scaledMy = (int)(my / scale);
        int vWidth = (int)(this.width / scale);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        LogoRenderer.render(g, vWidth);
        g.drawCenteredString(this.font, this.title, vWidth / 2, 20, 0xFFFFFF);

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

        int elementIndex = 0;
        int inputIdx = 0;
        for (ScreenData.Element e : def.elements) {
            float delayT = Mth.clamp((System.currentTimeMillis() - openTime - (elementIndex * 30L)) / (float)ANIM_DURATION, 0f, 1f);
            float itemEase = backOut(delayT);
            int startOffsetY = 30;
            int currentY = (int)(e.y + startOffsetY * (1 - itemEase));
            int alpha = (int)(itemEase * 255);
            int colorWithAlpha = (e.color & 0xFFFFFF) | (alpha << 24);

            if (e.type.equals("text")) {
                g.pose().pushPose();
                g.pose().translate(e.x, currentY, 0);
                g.pose().scale(e.scale, e.scale, 1.0f);
                g.drawString(this.font, e.content, 0, 0, colorWithAlpha, false);
                g.pose().popPose();
            } else if (e.type.equals("input")) {
                if (e.required && inputs.get(inputIdx).getValue().trim().isEmpty()) {
                    AbstractWidget w = inputs.get(inputIdx);
                    renderOutline(g, w.getX() - 1, w.getY() - 1, w.getWidth() + 2, w.getHeight() + 2, 0xFFFF5555);
                }
                inputIdx++;
            }
            elementIndex++;
        }

        super.render(g, scaledMx, scaledMy, pt);
        g.pose().popPose();
    }

    private void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    private float backOut(float t) { float c1 = 1.70158f; float c3 = c1 + 1; return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}