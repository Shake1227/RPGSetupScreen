package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.awt.Color;
import java.util.function.Consumer;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final Consumer<Integer> onColorSelected;
    private EditBox colorEditBox;

    private float hue = 0.0f;
    private float saturation = 0.0f;
    private float brightness = 1.0f;

    private int pickerX, pickerY, pickerRadius;
    private int sliderX, sliderY, sliderWidth, sliderHeight;
    private int previewX, previewY, previewSize;

    private boolean isDraggingPicker = false;
    private boolean isDraggingSlider = false;

    public ColorPickerScreen(Screen parent, int initialColor, Consumer<Integer> onColorSelected) {
        super(Component.translatable("gui.rpgsetupscreen.colorpicker.title"));
        this.parent = parent;
        this.onColorSelected = onColorSelected;
        setColorFromInt(initialColor);
    }

    @Override
    protected void init() {
        super.init();
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        pickerRadius = 50;
        pickerX = centerX - 80;
        pickerY = centerY - 20;

        sliderWidth = 20;
        sliderHeight = pickerRadius * 2;
        sliderX = pickerX + pickerRadius * 2 + 10;
        sliderY = pickerY - pickerRadius;

        previewSize = 40;
        previewX = sliderX + sliderWidth + 10;
        previewY = sliderY;

        this.colorEditBox = new EditBox(this.font, previewX, previewY + previewSize + 5, 80, 20, Component.translatable("gui.rpgsetupscreen.colorpicker.hex"));
        this.colorEditBox.setMaxLength(7);
        this.colorEditBox.setResponder(this::setColorFromHex);
        updateColorHex();
        this.addRenderableWidget(this.colorEditBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.colorpicker.done"), (button) -> {
            int color = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
            onColorSelected.accept(color & 0xFFFFFF);
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX - 55, centerY + 60, 50, 20).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.colorpicker.cancel"), (button) -> {
            this.minecraft.setScreen(this.parent);
        }).bounds(centerX + 5, centerY + 60, 50, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        drawColorPicker(guiGraphics);
        drawBrightnessSlider(guiGraphics);
        drawPreviewBox(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawColorPicker(GuiGraphics guiGraphics) {
        for (int y = -pickerRadius; y <= pickerRadius; y++) {
            for (int x = -pickerRadius; x <= pickerRadius; x++) {
                double dist = Math.sqrt(x * x + y * y);
                if (dist <= pickerRadius) {
                    double angle = Math.atan2(y, x);
                    float h = (float) ((angle / (2 * Math.PI)) + 0.5);
                    float s = (float) (dist / pickerRadius);
                    int color = Color.HSBtoRGB(h, s, 1.0f);
                    guiGraphics.fill(pickerX + x, pickerY + y, pickerX + x + 1, pickerY + y + 1, color);
                }
            }
        }
        float cursorAngle = (this.hue - 0.5f) * 2 * Mth.PI;
        float cursorDist = this.saturation * pickerRadius;
        int cursorX = pickerX + (int) (Math.cos(cursorAngle) * cursorDist);
        int cursorY = pickerY + (int) (Math.sin(cursorAngle) * cursorDist);
        guiGraphics.renderOutline(cursorX - 2, cursorY - 2, 5, 5, 0xFF000000);
        guiGraphics.renderOutline(cursorX - 1, cursorY - 1, 3, 3, 0xFFFFFFFF);
    }

    private void drawBrightnessSlider(GuiGraphics guiGraphics) {
        int topColor = Color.HSBtoRGB(this.hue, this.saturation, 1.0f);
        int bottomColor = Color.HSBtoRGB(this.hue, this.saturation, 0.0f);
        guiGraphics.fillGradient(sliderX, sliderY, sliderX + sliderWidth, sliderY + sliderHeight, topColor, bottomColor);
        guiGraphics.renderOutline(sliderX, sliderY, sliderWidth, sliderHeight, 0xFF888888);
        int markerY = sliderY + (int)((1.0f - this.brightness) * sliderHeight);
        guiGraphics.fill(sliderX - 2, markerY - 1, sliderX + sliderWidth + 2, markerY + 1, 0xFFFFFFFF);
        guiGraphics.renderOutline(sliderX - 2, markerY - 1, sliderWidth + 4, 2, 0xFF000000);
    }

    private void drawPreviewBox(GuiGraphics guiGraphics) {
        int color = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        guiGraphics.fill(previewX, previewY, previewX + previewSize, previewY + previewSize, 0xFF000000 | color);
        guiGraphics.renderOutline(previewX, previewY, previewSize, previewSize, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isMouseInPicker(mouseX, mouseY)) {
                this.isDraggingPicker = true;
                updateColorFromMouse(mouseX, mouseY);
                return true;
            }
            if (isMouseInSlider(mouseX, mouseY)) {
                this.isDraggingSlider = true;
                updateColorFromMouse(mouseX, mouseY);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingPicker = false;
            this.isDraggingSlider = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && (this.isDraggingPicker || this.isDraggingSlider)) {
            updateColorFromMouse(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isMouseInPicker(double mouseX, double mouseY) {
        return Math.sqrt(Math.pow(mouseX - pickerX, 2) + Math.pow(mouseY - pickerY, 2)) <= pickerRadius;
    }

    private boolean isMouseInSlider(double mouseX, double mouseY) {
        return mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
    }

    private void updateColorFromMouse(double mouseX, double mouseY) {
        if (this.isDraggingPicker) {
            double dx = mouseX - pickerX;
            double dy = mouseY - pickerY;
            double dist = Math.sqrt(dx * dx + dy * dy);
            this.saturation = Mth.clamp((float)(dist / pickerRadius), 0.0f, 1.0f);
            this.hue = (float) ((Math.atan2(dy, dx) / (2 * Math.PI)) + 0.5);
        }
        if (this.isDraggingSlider) {
            this.brightness = Mth.clamp(1.0f - (float)((mouseY - sliderY) / sliderHeight), 0.0f, 1.0f);
        }
        updateColorHex();
    }

    private void updateColorHex() {
        int color = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        String hex = String.format("#%06X", (0xFFFFFF & color));
        if (!this.colorEditBox.getValue().equalsIgnoreCase(hex)) {
            this.colorEditBox.setValue(hex);
        }
    }

    private void setColorFromInt(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if(this.colorEditBox != null) updateColorHex();
    }

    private void setColorFromHex(String hex) {
        if (hex == null) return;
        String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
        if (cleanHex.length() != 6) return;
        try {
            int c = Integer.parseInt(cleanHex, 16);
            setColorFromInt(c);
        } catch (NumberFormatException e) {}
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override public boolean isPauseScreen() { return false; }
}