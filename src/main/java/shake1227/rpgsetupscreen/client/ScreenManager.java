package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shake1227.rpgsetupscreen.data.ScreenData;
import shake1227.rpgsetupscreen.network.RPGNetwork;

import java.util.Collections;
import java.util.List;

public class ScreenManager extends Screen {
    private List<ScreenData.Def> screens;
    private final int MAX_SCREENS = 8;

    public ScreenManager(List<ScreenData.Def> screens) {
        super(Component.translatable("gui.rpgsetupscreen.manager.title"));
        this.screens = screens;
    }

    private float getForcedScale() {
        double currentScale = this.minecraft.getWindow().getGuiScale();
        return (float) (3.0 / currentScale);
    }

    @Override
    protected void init() {
        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);
        int vHeight = (int)(this.height / scale);

        int centerX = vWidth / 2;
        int centerY = vHeight / 2;

        int cols = 4;
        int rows = 2;
        int slotW = 100;
        int slotH = 70;
        int gap = 15;

        int totalW = cols * slotW + (cols - 1) * gap;
        int totalH = rows * slotH + (rows - 1) * gap;

        int startX = centerX - totalW / 2;
        int startY = centerY - totalH / 2 - 40;

        for (int i = 0; i < MAX_SCREENS; i++) {
            final int idx = i;
            int c = i % cols;
            int r = i / cols;
            int x = startX + c * (slotW + gap);
            int y = startY + r * (slotH + gap);

            if (i < screens.size()) {
                ScreenData.Def s = screens.get(i);
                this.addRenderableWidget(new ScreenPreviewWidget(x, y, slotW, slotH, s, idx));
            } else {
                this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.manager.add"), b -> {
                    if (screens.size() > 0 && screens.get(screens.size()-1).uuid.equals("spawn")) {
                        screens.add(screens.size() - 1, new ScreenData.Def("New Screen", false));
                    } else {
                        screens.add(new ScreenData.Def("New Screen", false));
                    }
                    this.rebuildWidgets();
                }).bounds(x, y, slotW, slotH).build());
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.manager.close"), b -> this.onClose())
                .bounds(centerX - 40, vHeight - 30, 80, 20).build());
    }

    @Override
    public void removed() {
        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketSaveScreens(screens));
        super.removed();
    }

    private void saveAndReload() {
        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketSaveScreens(screens));
        this.rebuildWidgets();
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) { float s = getForcedScale(); return super.mouseClicked(mx/s, my/s, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { float s = getForcedScale(); return super.mouseReleased(mx/s, my/s, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { float s = getForcedScale(); return super.mouseDragged(mx/s, my/s, btn, dx/s, dy/s); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderDirtBackground(g);
        float scale = getForcedScale();
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);
        int vWidth = (int)(this.width / scale);
        g.drawCenteredString(this.font, this.title, vWidth / 2, 10, 0xFFFFFF);
        super.render(g, (int)(mx/scale), (int)(my/scale), pt);
        g.pose().popPose();
    }

    @Override public boolean isPauseScreen() { return false; }

    class ScreenPreviewWidget extends AbstractWidget {
        private final ScreenData.Def def;
        private final int index;

        public ScreenPreviewWidget(int x, int y, int w, int h, ScreenData.Def def, int index) {
            super(x, y, w, h, Component.literal(def.title));
            this.def = def;
            this.index = index;
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            int borderColor = isHovered ? 0xFFFFFFFF : 0xFF888888;
            if (def.isSystem && isHovered) borderColor = 0xFFAAAAAA;

            g.fill(getX(), getY(), getX() + width, getY() + height, 0xFF000000);
            renderOutline(g, getX(), getY(), width, height, borderColor);

            int previewX = getX() + 2;
            int previewY = getY() + 14;
            int previewW = width - 4;
            int previewH = height - 16;

            float contentScale = 0.15f;

            float guiScale = getForcedScale();
            g.enableScissor((int)(previewX * guiScale), (int)(previewY * guiScale),
                    (int)((previewX + previewW) * guiScale), (int)((previewY + previewH) * guiScale));

            g.pose().pushPose();
            g.pose().translate(previewX, previewY, 0);
            g.pose().scale(contentScale, contentScale, 1.0f);

            g.fill(0, 0, (int)(previewW/contentScale), (int)(previewH/contentScale), 0xFF202020);

            if (def.uuid.equals("setup")) {
                int cx = (int)(previewW / contentScale / 2);
                int cy = (int)(previewH / contentScale / 2);
                g.fill(cx - 20, cy - 20, cx + 20, cy + 40, 0xFF888888); // Body
                g.fill(cx - 15, cy - 50, cx + 15, cy - 25, 0xFFCCCCCC); // Head
                g.fill(cx - 50, cy, cx - 30, cy + 5, 0xFF666666);
                g.fill(cx + 30, cy, cx + 50, cy + 5, 0xFF666666);
            } else if (def.uuid.equals("spawn")) {
                int cx = (int)(previewW / contentScale / 2);
                for(int k=-1; k<=1; k++) {
                    g.fill(cx - 50, 100 + k*30 - 10, cx + 50, 100 + k*30 + 10, 0xFF555555);
                }
                g.fill(cx - 5, 40, cx + 5, 50, 0xFFFF0000);
            } else {
                for (ScreenData.Element e : def.elements) {
                    if (e.type.equals("text")) {
                        g.drawString(font, e.content, e.x, e.y, e.color, false);
                    } else if (e.type.equals("input")) {
                        g.fill(e.x, e.y, e.x + e.w, e.y + e.h, 0xFF000000);
                        renderOutline(g, e.x, e.y, e.w, e.h, 0xFFFFFFFF);
                    }
                }
            }

            g.pose().popPose();
            g.disableScissor();

            g.fill(getX(), getY(), getX() + width, getY() + 12, 0xAA000000);
            String t = def.title.length() > 13 ? def.title.substring(0, 12) + "." : def.title;
            g.drawString(font, t, getX() + 4, getY() + 2, 0xFFFFFF, false);

            if (!def.isSystem) {
                int delX = getX() + width - 12;
                int delY = getY() + 1;
                boolean hoverDel = mx >= delX && mx < delX + 10 && my >= delY && my < delY + 10;
                g.fill(delX, delY, delX + 10, delY + 10, hoverDel ? 0xFFFF0000 : 0xFFAA0000);
                g.drawCenteredString(font, "x", delX + 5, delY + 1, 0xFFFFFF);
            }

            if (index > 0 && !def.isSystem && !screens.get(index-1).isSystem) {
                int upX = getX() + width - 24;
                int upY = getY() + height - 12;
                boolean hoverUp = mx >= upX && mx < upX + 10 && my >= upY && my < upY + 10;
                g.fill(upX, upY, upX + 10, upY + 10, hoverUp ? 0xFFAAAAAA : 0xFF666666);
                g.drawCenteredString(font, "<", upX + 5, upY + 1, 0xFFFFFF);
            }
            if (index < screens.size() - 2 && !def.isSystem) {
                int downX = getX() + width - 12;
                int downY = getY() + height - 12;
                boolean hoverDown = mx >= downX && mx < downX + 10 && my >= downY && my < downY + 10;
                g.fill(downX, downY, downX + 10, downY + 10, hoverDown ? 0xFFAAAAAA : 0xFF666666);
                g.drawCenteredString(font, ">", downX + 5, downY + 1, 0xFFFFFF);
            }
        }

        private void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
            g.fill(x, y, x + w, y + 1, color);
            g.fill(x, y + h - 1, x + w, y + h, color);
            g.fill(x, y, x + 1, y + h, color);
            g.fill(x + w - 1, y, x + w, y + h, color);
        }

        @Override
        public void onClick(double mx, double my) {
            if (def.isSystem) return;

            int delX = getX() + width - 12;
            int delY = getY() + 1;
            if (mx >= delX && mx < delX + 10 && my >= delY && my < delY + 10) {
                screens.remove(index);
                saveAndReload();
                return;
            }

            int upX = getX() + width - 24;
            int upY = getY() + height - 12;
            if (mx >= upX && mx < upX + 10 && my >= upY && my < upY + 10) {
                if(index > 0 && !screens.get(index-1).isSystem) {
                    Collections.swap(screens, index, index - 1);
                    saveAndReload();
                }
                return;
            }
            int downX = getX() + width - 12;
            int downY = getY() + height - 12;
            if (mx >= downX && mx < downX + 10 && my >= downY && my < downY + 10) {
                if(index < screens.size() - 2) {
                    Collections.swap(screens, index, index + 1);
                    saveAndReload();
                }
                return;
            }

            Minecraft.getInstance().setScreen(new ScreenEditor(def, screens));
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) {}
    }
}