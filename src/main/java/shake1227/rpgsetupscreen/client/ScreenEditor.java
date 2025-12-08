package shake1227.rpgsetupscreen.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import shake1227.rpgsetupscreen.data.ScreenData;
import shake1227.rpgsetupscreen.network.RPGNetwork;

import java.util.ArrayList;
import java.util.List;

public class ScreenEditor extends Screen {
    private final ScreenData.Def targetScreen;
    private final List<ScreenData.Def> allScreens;

    private ScreenData.Element selectedElement = null;
    private EditBox overlayEditBox;

    private EditBox titleBox;
    private Button cmdButton;
    private EditBox propX, propY, propW, propH, propVar;
    private Button propColorBtn, propRequiredBtn;

    private List<AbstractWidgetWrapper> sidebarWidgets = new ArrayList<>();

    private long lastClickTime = 0;
    private long clickStartTime = 0;
    private boolean isDragging = false;
    private boolean isHolding = false;
    private boolean isResizing = false;
    private int resizeMode = 0;
    private int dragOffsetX, dragOffsetY;
    private int initialX, initialY, initialW, initialH;
    private float initialScale;
    private static final int SNAP_THRESHOLD = 5;
    private boolean snapX = false, snapY = false;
    private int snapLineX = -1, snapLineY = -1;
    private float sidebarProgress = 0.0f;
    private static final int SIDEBAR_WIDTH = 130;
    private static final int HANDLE_SIZE = 6;

    public ScreenEditor(ScreenData.Def target, List<ScreenData.Def> all) {
        super(Component.translatable("gui.rpgsetupscreen.editor.title"));
        this.targetScreen = target;
        this.allScreens = all;
    }

    private float getForcedScale() {
        double currentScale = this.minecraft.getWindow().getGuiScale();
        return (float) (3.0 / currentScale);
    }

    @Override
    protected void init() {
        float scale = getForcedScale();
        int w = (int)(this.width / scale);
        int h = (int)(this.height / scale);
        sidebarWidgets.clear();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.editor.back"), b -> {
            save();
            ClientHooks.requestOpenManager();
            this.minecraft.setScreen(null);
        }).bounds(5, 5, 50, 20).build());

        titleBox = new EditBox(this.font, 60, 5, 120, 20, Component.literal(""));
        titleBox.setValue(targetScreen.title);
        titleBox.setResponder(s -> targetScreen.title = s);
        titleBox.setHint(Component.translatable("gui.rpgsetupscreen.editor.hint.title"));
        this.addRenderableWidget(titleBox);

        cmdButton = Button.builder(Component.translatable("gui.rpgsetupscreen.editor.prop.command"), b -> {
            this.minecraft.setScreen(new CommandListScreen(this, targetScreen.executeCommands));
        }).bounds(185, 5, 100, 20).build();
        this.addRenderableWidget(cmdButton);

        int ly = 35;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.editor.add_text"), b -> {
            addElement("text", w, h);
        }).bounds(5, ly, 60, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.editor.add_input"), b -> {
            addElement("input", w, h);
        }).bounds(5, ly + 25, 60, 20).build());

        int sideX = w;
        int py = 40;

        propX = createSidebarEditBox("0", sideX, py);
        propX.setResponder(s -> tryParseInt(s, v -> { if(selectedElement!=null) selectedElement.x = v; }));

        propY = createSidebarEditBox("0", sideX, py);
        propY.setResponder(s -> tryParseInt(s, v -> { if(selectedElement!=null) selectedElement.y = v; }));

        propW = createSidebarEditBox("100", sideX, py);
        propW.setResponder(s -> tryParseInt(s, v -> { if(selectedElement!=null) selectedElement.w = v; }));

        propH = createSidebarEditBox("20", sideX, py);
        propH.setResponder(s -> tryParseInt(s, v -> { if(selectedElement!=null) selectedElement.h = v; }));

        propVar = createSidebarEditBox("", sideX, py);
        propVar.setResponder(s -> { if(selectedElement!=null) selectedElement.varName = s; });
        propVar.setHint(Component.literal("var_name"));

        propColorBtn = Button.builder(Component.literal(""), b -> {
            if (selectedElement != null && selectedElement.type.equals("text")) {
                Minecraft.getInstance().setScreen(new ColorPickerScreen(this, selectedElement.color, newColor -> {
                    selectedElement.color = newColor;
                }));
            }
        }).bounds(sideX, py, 110, 20).build();
        this.addRenderableWidget(propColorBtn);
        sidebarWidgets.add(new AbstractWidgetWrapper(propColorBtn));

        propRequiredBtn = Button.builder(Component.literal(""), b -> {
            if (selectedElement != null && selectedElement.type.equals("input")) {
                selectedElement.required = !selectedElement.required;
                updateSidebarValues();
            }
        }).bounds(sideX, py, 110, 20).build();
        this.addRenderableWidget(propRequiredBtn);
        sidebarWidgets.add(new AbstractWidgetWrapper(propRequiredBtn));

        Button delBtn = Button.builder(Component.translatable("gui.rpgsetupscreen.editor.delete"), b -> {
            if(selectedElement != null) {
                targetScreen.elements.remove(selectedElement);
                selectedElement = null;
                isDragging = false;
                isHolding = false;
                isResizing = false;
                init();
            }
        }).bounds(sideX, py + 10, 110, 20).build();
        sidebarWidgets.add(new AbstractWidgetWrapper(delBtn));

        overlayEditBox = new EditBox(this.font, 0, 0, 0, 0, Component.literal("Overlay"));
        overlayEditBox.setMaxLength(65535);
        overlayEditBox.setVisible(false);
        this.addRenderableWidget(overlayEditBox);

        updateSidebarValues();
        updateOverlayBox();
    }

    private void updateSidebarValues() { if (selectedElement == null) return; propX.setValue(String.valueOf(selectedElement.x)); propY.setValue(String.valueOf(selectedElement.y)); if (selectedElement.type.equals("input")) { propW.setValue(String.valueOf(selectedElement.w)); propH.setValue(String.valueOf(selectedElement.h)); propVar.setValue(selectedElement.varName); propRequiredBtn.setMessage(Component.literal("Required: " + (selectedElement.required ? "ON" : "OFF"))); } else if (selectedElement.type.equals("text")) { propColorBtn.setMessage(Component.literal(String.format("#%06X", selectedElement.color))); } }
    private EditBox createSidebarEditBox(String defVal, int x, int y) { EditBox box = new EditBox(this.font, x, y, 110, 20, Component.literal("")); box.setValue(defVal); this.addRenderableWidget(box); sidebarWidgets.add(new AbstractWidgetWrapper(box)); return box; }
    private void addElement(String type, int w, int h) { ScreenData.Element e = new ScreenData.Element(type); e.x = w / 2 - 50; e.y = h / 2 - 10; if (type.equals("text")) { e.content = "Text"; e.color = 0xFFFFFF; e.scale = 1.0f; } else { e.content = ""; e.w = 100; e.h = 20; e.required = false; } targetScreen.elements.add(e); selectedElement = e; updateSidebarValues(); updateOverlayBox(); this.setFocused(overlayEditBox); overlayEditBox.setFocused(true); }

    @Override
    public void removed() {
        save();
        super.removed();
    }

    private void save() {
        boolean found = false;
        for (int i = 0; i < allScreens.size(); i++) {
            if (allScreens.get(i).uuid.equals(targetScreen.uuid)) {
                allScreens.set(i, targetScreen);
                found = true;
                break;
            }
        }
        if (!found) {
            allScreens.add(targetScreen);
        }
        RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketSaveScreens(allScreens));
    }

    private void tryParseInt(String s, java.util.function.IntConsumer setter) { try { setter.accept(Integer.parseInt(s)); } catch (NumberFormatException ignored) {} }
    private void updateOverlayBox() { if (selectedElement == null) { overlayEditBox.setVisible(false); overlayEditBox.setResponder(s -> {}); return; } if (!overlayEditBox.isVisible()) { overlayEditBox.setValue(selectedElement.content); } overlayEditBox.setResponder(s -> { if (selectedElement != null) selectedElement.content = s; }); if (selectedElement.type.equals("text")) { int w = Math.max(50, getElementWidth(selectedElement) + 10); int h = getElementHeight(selectedElement); overlayEditBox.setX(selectedElement.x - 4); overlayEditBox.setY(selectedElement.y - 4); overlayEditBox.setWidth(w + 8); overlayEditBox.setHeight(h + 8); } else { overlayEditBox.setX(selectedElement.x); overlayEditBox.setY(selectedElement.y); overlayEditBox.setWidth(selectedElement.w); overlayEditBox.setHeight(selectedElement.h); } }
    private int getElementWidth(ScreenData.Element e) { if (e.type.equals("text")) return (int)(this.font.width(e.content) * e.scale); return e.w; }
    private int getElementHeight(ScreenData.Element e) { if (e.type.equals("text")) return (int)(9 * e.scale); return e.h; }
    private int getHoveredHandle(double mx, double my, ScreenData.Element e) { if (e == null) return 0; int x = e.x; int y = e.y; int w = getElementWidth(e); int h = getElementHeight(e); int hs = HANDLE_SIZE; if (checkHandle(mx, my, x - hs, y - hs)) return 1; if (checkHandle(mx, my, x + w/2 - hs/2, y - hs)) return 2; if (checkHandle(mx, my, x + w, y - hs)) return 3; if (checkHandle(mx, my, x + w, y + h/2 - hs/2)) return 4; if (checkHandle(mx, my, x + w, y + h)) return 5; if (checkHandle(mx, my, x + w/2 - hs/2, y + h)) return 6; if (checkHandle(mx, my, x - hs, y + h)) return 7; if (checkHandle(mx, my, x - hs, y + h/2 - hs/2)) return 8; return 0; }
    private boolean checkHandle(double mx, double my, int x, int y) { return mx >= x && mx <= x + HANDLE_SIZE && my >= y && my <= y + HANDLE_SIZE; }
    @Override public boolean mouseClicked(double mx, double my, int btn) { float s = getForcedScale(); return handleMouseClicked(mx / s, my / s, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { float s = getForcedScale(); return handleMouseReleased(mx / s, my / s, btn); }
    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) { float s = getForcedScale(); return handleMouseDragged(mx / s, my / s, btn, dx / s, dy / s); }
    private boolean handleMouseClicked(double mx, double my, int btn) { if (overlayEditBox.isVisible() && overlayEditBox.isFocused()) { if (!overlayEditBox.isMouseOver(mx, my)) { overlayEditBox.setFocused(false); overlayEditBox.setVisible(false); return true; } } if (super.mouseClicked(mx, my, btn)) return true; if (selectedElement != null) { int handle = getHoveredHandle(mx, my, selectedElement); if (handle != 0) { isResizing = true; resizeMode = handle; initialX = selectedElement.x; initialY = selectedElement.y; if (selectedElement.type.equals("text")) { initialScale = selectedElement.scale; initialW = this.font.width(selectedElement.content); initialH = 9; } else { initialW = selectedElement.w; initialH = selectedElement.h; } dragOffsetX = (int)mx; dragOffsetY = (int)my; return true; } } float scale = getForcedScale(); int vWidth = (int)(this.width / scale); for (int i = targetScreen.elements.size() - 1; i >= 0; i--) { ScreenData.Element e = targetScreen.elements.get(i); int w = getElementWidth(e); int h = getElementHeight(e); if (mx >= e.x && mx <= e.x + w && my >= e.y && my <= e.y + h) { long now = System.currentTimeMillis(); if (selectedElement == e) { if (now - lastClickTime < 300) { updateOverlayBox(); overlayEditBox.setVisible(true); this.setFocused(overlayEditBox); overlayEditBox.setFocused(true); isHolding = false; return true; } } selectedElement = e; updateSidebarValues(); clickStartTime = now; lastClickTime = now; isHolding = true; dragOffsetX = (int)mx - e.x; dragOffsetY = (int)my - e.y; return true; } } if (mx < vWidth - (SIDEBAR_WIDTH * sidebarProgress)) { selectedElement = null; overlayEditBox.setVisible(false); isHolding = false; isResizing = false; return true; } return false; }
    private boolean handleMouseReleased(double mx, double my, int btn) { isHolding = false; isDragging = false; isResizing = false; snapX = false; snapY = false; return super.mouseReleased(mx, my, btn); }
    @Override public void tick() { if (isHolding && selectedElement != null && !isDragging && !isResizing) { if (System.currentTimeMillis() - clickStartTime > 200) { isDragging = true; } } super.tick(); }
    private boolean handleMouseDragged(double mx, double my, int btn, double dx, double dy) { if (selectedElement != null && btn == 0) { if (isResizing) { int diffX = (int)mx - dragOffsetX; int diffY = (int)my - dragOffsetY; if (selectedElement.type.equals("text")) { if (resizeMode >= 3 && resizeMode <= 5) { float newW = (float)initialW * initialScale + diffX; if (newW > 5) selectedElement.scale = newW / (float)initialW; } if (selectedElement.scale < 0.1f) selectedElement.scale = 0.1f; } else { if (resizeMode == 1 || resizeMode == 7 || resizeMode == 8) { int change = Math.min(diffX, initialW - 5); selectedElement.x = initialX + change; selectedElement.w = initialW - change; } if (resizeMode == 1 || resizeMode == 2 || resizeMode == 3) { int change = Math.min(diffY, initialH - 5); selectedElement.y = initialY + change; selectedElement.h = initialH - change; } if (resizeMode == 3 || resizeMode == 4 || resizeMode == 5) { selectedElement.w = Math.max(5, initialW + diffX); } if (resizeMode == 5 || resizeMode == 6 || resizeMode == 7) { selectedElement.h = Math.max(5, initialH + diffY); } } updateSidebarValues(); return true; } if (isDragging) { if (overlayEditBox.isVisible() && overlayEditBox.isFocused()) return false; int newX = (int)mx - dragOffsetX; int newY = (int)my - dragOffsetY; snapX = false; snapY = false; snapLineX = -1; snapLineY = -1; float scale = getForcedScale(); int vWidth = (int)(this.width / scale); int vHeight = (int)(this.height / scale); int selW = getElementWidth(selectedElement); int selH = getElementHeight(selectedElement); int currentCenterX = newX + selW / 2; int currentCenterY = newY + selH / 2; if (Math.abs(currentCenterX - vWidth/2) < SNAP_THRESHOLD) { newX = vWidth/2 - selW/2; snapX = true; snapLineX = vWidth/2; } if (Math.abs(currentCenterY - vHeight/2) < SNAP_THRESHOLD) { newY = vHeight/2 - selH/2; snapY = true; snapLineY = vHeight/2; } for (ScreenData.Element e : targetScreen.elements) { if (e == selectedElement) continue; int eW = getElementWidth(e); int eH = getElementHeight(e); int eCenterX = e.x + eW/2; int eCenterY = e.y + eH/2; if (Math.abs(currentCenterX - eCenterX) < SNAP_THRESHOLD) { newX = eCenterX - selW/2; snapX = true; snapLineX = eCenterX; } if (Math.abs(currentCenterY - eCenterY) < SNAP_THRESHOLD) { newY = eCenterY - selH/2; snapY = true; snapLineY = eCenterY; } } selectedElement.x = newX; selectedElement.y = newY; updateSidebarValues(); if (overlayEditBox.isVisible()) updateOverlayBox(); return true; } } return super.mouseDragged(mx, my, btn, dx, dy); }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) { if (keyCode == GLFW.GLFW_KEY_DELETE && selectedElement != null && !overlayEditBox.isFocused()) { targetScreen.elements.remove(selectedElement); selectedElement = null; overlayEditBox.setVisible(false); init(); return true; } if (keyCode == GLFW.GLFW_KEY_ENTER && overlayEditBox.isFocused()) { overlayEditBox.setFocused(false); overlayEditBox.setVisible(false); return true; } return super.keyPressed(keyCode, scanCode, modifiers); }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) { this.renderDirtBackground(g); float scale = getForcedScale(); int scaledMx = (int)(mx / scale); int scaledMy = (int)(my / scale); g.pose().pushPose(); g.pose().scale(scale, scale, 1.0f); for (ScreenData.Element e : targetScreen.elements) { boolean isSelected = (e == selectedElement); boolean isEditing = isSelected && overlayEditBox.isVisible(); if (e.type.equals("text")) { g.pose().pushPose(); g.pose().translate(e.x, e.y, 0); g.pose().scale(e.scale, e.scale, 1.0f); if (!isEditing) g.drawString(this.font, e.content, 0, 0, e.color, false); g.pose().popPose(); if (isSelected) { int w = (int)(this.font.width(e.content) * e.scale); int h = (int)(9 * e.scale); renderSelection(g, e.x, e.y, w, h); } } else if (e.type.equals("input")) { g.fill(e.x, e.y, e.x + e.w, e.y + e.h, 0xFF000000); int borderColor = isSelected ? 0xFFFF0000 : 0xFFFFFFFF; if(e.required) borderColor = isSelected ? 0xFFFF5555 : 0xFFFFAAAA; renderOutline(g, e.x, e.y, e.w, e.h, borderColor); if (!isEditing && (e.content != null && !e.content.isEmpty())) { g.drawString(this.font, e.content, e.x + 4, e.y + (e.h - 8) / 2, 0xFF888888, false); } if (isSelected) renderSelection(g, e.x, e.y, e.w, e.h); } } int vWidth = (int)(this.width / scale); int vHeight = (int)(this.height / scale); if (isDragging && selectedElement != null) { if (snapX && snapLineX != -1) g.fill(snapLineX, 0, snapLineX + 1, vHeight, 0xFF00FFFF); if (snapY && snapLineY != -1) g.fill(0, snapLineY, vWidth, snapLineY + 1, 0xFF00FFFF); } float targetProgress = (selectedElement != null) ? 1.0f : 0.0f; sidebarProgress = Mth.lerp(0.2f, sidebarProgress, targetProgress); if (sidebarProgress > 0.01f) { int sidebarX = (int)(vWidth - (SIDEBAR_WIDTH * sidebarProgress)); g.fill(sidebarX, 0, vWidth, vHeight, 0xD0000000); g.vLine(sidebarX, 0, vHeight, 0xFFFFFFFF); int currentY = 40; for (AbstractWidgetWrapper wrapper : sidebarWidgets) { boolean visible = true; if (selectedElement != null) { if (wrapper.widget == propW || wrapper.widget == propH || wrapper.widget == propVar || wrapper.widget == propRequiredBtn) { visible = selectedElement.type.equals("input"); } else if (wrapper.widget == propColorBtn) { visible = selectedElement.type.equals("text"); } } else { visible = false; } if (wrapper.widget instanceof Button && ((Button)wrapper.widget).getMessage().getString().equals(Component.translatable("gui.rpgsetupscreen.editor.delete").getString())) { visible = (selectedElement != null); } wrapper.widget.visible = visible && (sidebarProgress > 0.8f); if (visible) { wrapper.widget.setX(sidebarX + 10); wrapper.widget.setY(currentY); int labelY = currentY - 10; if (wrapper.widget == propX) g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.x"), sidebarX + 10, labelY, 0xFFFFFF); if (wrapper.widget == propY) g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.y"), sidebarX + 70, labelY, 0xFFFFFF); if (wrapper.widget == propW) g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.w"), sidebarX + 10, labelY, 0xFFFFFF); if (wrapper.widget == propH) g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.h"), sidebarX + 70, labelY, 0xFFFFFF); if (wrapper.widget == propVar) g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.var"), sidebarX + 10, labelY, 0xFFFFFF); if (wrapper.widget == propColorBtn) { g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.prop.color"), sidebarX + 10, labelY, 0xFFFFFF); if (selectedElement != null) { g.fill(wrapper.widget.getX()+2, wrapper.widget.getY()+2, wrapper.widget.getX()+wrapper.widget.getWidth()-2, wrapper.widget.getY()+wrapper.widget.getHeight()-2, 0xFF000000 | selectedElement.color); } } currentY += 35; } } if (propX.visible) { propX.setWidth(50); propY.setWidth(50); propY.setX(sidebarX + 70); propY.setY(propX.getY()); } if (propW.visible) { propW.setWidth(50); propH.setWidth(50); propH.setX(sidebarX + 70); propH.setY(propW.getY()); } } else { for (AbstractWidgetWrapper w : sidebarWidgets) w.widget.visible = false; } g.drawString(this.font, Component.translatable("gui.rpgsetupscreen.editor.help"), 5, vHeight - 15, 0xFFAAAAAA, false); super.render(g, scaledMx, scaledMy, pt); g.pose().popPose(); }
    private void renderSelection(GuiGraphics g, int x, int y, int w, int h) { renderOutline(g, x-1, y-1, w+2, h+2, 0xFFFF0000); int hs = HANDLE_SIZE; int color = 0xFFFFFFFF; g.fill(x - hs, y - hs, x, y, color); g.fill(x + w/2 - hs/2, y - hs, x + w/2 + hs/2, y, color); g.fill(x + w, y - hs, x + w + hs, y, color); g.fill(x + w, y + h/2 - hs/2, x + w + hs, y + h/2 + hs/2, color); g.fill(x + w, y + h, x + w + hs, y + h + hs, color); g.fill(x + w/2 - hs/2, y + h, x + w/2 + hs/2, y + h + hs, color); g.fill(x - hs, y + h, x, y + h + hs, color); g.fill(x - hs, y + h/2 - hs/2, x, y + h/2 + hs/2, color); }
    private void renderOutline(GuiGraphics g, int x, int y, int w, int h, int color) { g.fill(x, y, x + w, y + 1, color); g.fill(x, y + h - 1, x + w, y + h, color); g.fill(x, y, x + 1, y + h, color); g.fill(x + w - 1, y, x + w, y + h, color); }
    @Override public boolean isPauseScreen() { return false; }
    private static class AbstractWidgetWrapper { AbstractWidget widget; AbstractWidgetWrapper(AbstractWidget w) { this.widget = w; } }
}