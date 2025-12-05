package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import java.util.List;

public class ScreenAdmin extends Screen {
    List<RPGCommands.SpawnData.Entry> list;
    EditBox nameInput;

    public ScreenAdmin(List<RPGCommands.SpawnData.Entry> l) {
        super(Component.literal("RPG Setup Admin"));
        this.list = l;
    }

    private float getForcedScale() {
        return 3.0f / (float)this.minecraft.getWindow().getGuiScale();
    }

    @Override
    protected void init() {
        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);
        int vHeight = (int)(this.height / scale);
        int cx = vWidth / 2;
        int cy = vHeight / 2 - 30;

        nameInput = new EditBox(this.font, cx - 100, cy - 20, 200, 20, Component.literal("Name"));
        this.addRenderableWidget(nameInput);

        this.addRenderableWidget(Button.builder(Component.literal("Add Current Location"), b -> {
            String n = nameInput.getValue();
            if(!n.isEmpty()) {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminAction(0, n));
                nameInput.setValue("");
            }
        }).bounds(cx - 100, cy + 5, 200, 20).build());

        int startY = cy + 40;
        if(list != null) {
            for(int i=0; i<list.size(); i++) {
                RPGCommands.SpawnData.Entry e = list.get(i);
                this.addRenderableWidget(Button.builder(Component.literal("Remove: " + e.name), b -> {
                    RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminAction(1, e.name));
                }).bounds(cx - 100, startY + i*25, 200, 20).build());
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float scale = getForcedScale();
        return super.mouseClicked(mx / scale, my / scale, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        float scale = getForcedScale();
        return super.mouseReleased(mx / scale, my / scale, btn);
    }

    @Override
    public boolean charTyped(char code, int modifiers) {
        return super.charTyped(code, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(super.keyPressed(keyCode, scanCode, modifiers)) return true;
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);

        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        super.render(g, (int)(mx/scale), (int)(my/scale), pt);
        g.drawCenteredString(this.font, this.title, vWidth / 2, 20, 0xFFFFFF);

        g.pose().popPose();
    }
}