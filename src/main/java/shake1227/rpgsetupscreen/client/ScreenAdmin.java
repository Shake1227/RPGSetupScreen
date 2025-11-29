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

    public ScreenAdmin(List<RPGCommands.SpawnData.Entry> l) { super(Component.literal("Spawn Manager")); this.list=l; }

    @Override
    protected void init() {
        int cx = this.width / 2;
        nameInput = new EditBox(font, cx-100, 30, 150, 20, Component.literal("Name"));
        addRenderableWidget(nameInput);

        addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            if(!nameInput.getValue().isEmpty()) RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminAction(0, nameInput.getValue()));
        }).bounds(cx+60, 30, 40, 20).build());

        int y = 60;
        for(var e : list) {
            addRenderableWidget(Button.builder(Component.literal(e.name + " ("+ (int)e.x +","+ (int)e.y +","+ (int)e.z +")"), b->{}).bounds(cx-100, y, 150, 20).build());
            addRenderableWidget(Button.builder(Component.literal("X"), b->{
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketAdminAction(1, e.name));
            }).bounds(cx+60, y, 20, 20).build());
            y+=25;
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
    }
}