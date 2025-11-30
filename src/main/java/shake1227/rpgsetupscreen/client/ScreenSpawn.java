package shake1227.rpgsetupscreen.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import java.util.List;

public class ScreenSpawn extends Screen {
    List<RPGCommands.SpawnData.Entry> list;
    int g; float w, h, c, cy, cs, ca;

    public ScreenSpawn(List<RPGCommands.SpawnData.Entry> l, int g, float w, float h, float c, float cy, float cs, float ca) {
        super(Component.translatable("gui.rpgsetupscreen.spawn"));
        this.list = l;
        if(this.list.isEmpty()) this.list.add(new RPGCommands.SpawnData.Entry("Initial Spawn Point", 0,0,0));
        this.g=g; this.w=w; this.h=h; this.c=c;
        this.cy=cy; this.cs=cs; this.ca=ca;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int btnW = 80, btnH = 20, pad = 5;

        int rowSize = 5;
        int totalW = rowSize * btnW + (rowSize-1)*pad;
        int startX = cx - totalW/2;
        int startY = cy - 40;

        for(int i=0; i<list.size(); i++) {
            RPGCommands.SpawnData.Entry e = list.get(i);
            int row = i / rowSize;
            int col = i % rowSize;
            Button b = Button.builder(Component.literal(e.name), btn -> {
                RPGNetwork.CHANNEL.sendToServer(new RPGNetwork.PacketFinishSetup(e.name, g, w, h, c, cy, cs, ca, ""));
                this.minecraft.setScreen(null);
            }).bounds(startX + col*(btnW+pad), startY + row*(btnH+pad), btnW, btnH).build();
            this.addRenderableWidget(b);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        super.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width/2, 40, 0xFFFFFF);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
}