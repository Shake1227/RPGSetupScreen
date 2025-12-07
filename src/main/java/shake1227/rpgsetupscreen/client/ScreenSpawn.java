package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import shake1227.rpgsetupscreen.network.RPGNetwork;
import shake1227.rpgsetupscreen.setup.RPGCommands;
import java.util.ArrayList;
import java.util.List;

public class ScreenSpawn extends Screen {
    List<RPGCommands.SpawnData.Entry> list;
    int g; float w, h, c, cy, cs, ca;
    boolean physics;

    private long openTime;
    private static final int ANIM_DURATION = 600;
    private final List<Integer> initialYPositions = new ArrayList<>();

    public ScreenSpawn(List<RPGCommands.SpawnData.Entry> l, int g, float w, float h, float c, float cy, float cs, float ca, boolean physics) {
        super(Component.translatable("gui.rpgsetupscreen.spawn"));
        this.list = l;
        if(this.list.isEmpty()) this.list.add(new RPGCommands.SpawnData.Entry("Initial Spawn Point", 0,0,0));
        this.g=g; this.w=w; this.h=h; this.c=c;
        this.cy=cy; this.cs=cs; this.ca=ca;
        this.physics = physics;
    }

    private float getForcedScale() { return 3.0f / (float)this.minecraft.getWindow().getGuiScale(); }

    @Override
    protected void init() {
        this.openTime = System.currentTimeMillis();
        this.initialYPositions.clear();
        float scale = getForcedScale();
        int vWidth = (int)(this.width / scale);
        int vHeight = (int)(this.height / scale);
        int centerX = vWidth / 2;
        int centerY = vHeight / 2;
        int btnW = 80, btnH = 20, pad = 5;
        int maxCols = 5;
        int totalRows = (list.size() + maxCols - 1) / maxCols;
        int startY = centerY - (totalRows * btnH + (Math.max(0, totalRows - 1)) * pad) / 2;

        for (int r = 0; r < totalRows; r++) {
            int buttonsInRow = maxCols;
            if (r == totalRows - 1) { int remainder = list.size() % maxCols; if (remainder != 0) buttonsInRow = remainder; }
            int rowWidth = buttonsInRow * btnW + (buttonsInRow - 1) * pad;
            int rowStartX = centerX - rowWidth / 2;
            int rowY = startY + r * (btnH + pad);

            for (int col = 0; col < buttonsInRow; col++) {
                int index = r * maxCols + col;
                RPGCommands.SpawnData.Entry e = list.get(index);
                Component nameComp = (e.name == null || e.name.isEmpty()) ? Component.literal("Unnamed") : Component.literal(e.name);

                Button b = Button.builder(nameComp, btn -> {
                    ClientHooks.pendingSpawnLocationName = e.name;
                    ClientHooks.pendingSpawnPosition = new Vec3(e.x, e.y, e.z);

                    int myIndex = -1;
                    for(int i=0; i<ClientHooks.screenDefs.size(); i++) {
                        if(ClientHooks.screenDefs.get(i).uuid.equals("spawn")) { myIndex = i; break; }
                    }
                    ClientHooks.openScreen(myIndex + 1);
                }).bounds(rowStartX + col * (btnW + pad), rowY, btnW, btnH).build();
                this.addRenderableWidget(b);
            }
        }

        this.addRenderableWidget(Button.builder(Component.translatable("gui.rpgsetupscreen.back"), b -> {
            int myIndex = -1;
            for(int i=0; i<ClientHooks.screenDefs.size(); i++) {
                if(ClientHooks.screenDefs.get(i).uuid.equals("spawn")) { myIndex = i; break; }
            }
            ClientHooks.openScreen(myIndex - 1);
        }).bounds(centerX - 50, vHeight - 100, 100, 20).build());

        for (var widget : this.renderables) { if (widget instanceof AbstractWidget aw) initialYPositions.add(aw.getY()); }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) { float scale = getForcedScale(); return super.mouseClicked(mx / scale, my / scale, btn); }
    @Override public boolean mouseReleased(double mx, double my, int btn) { float scale = getForcedScale(); return super.mouseReleased(mx / scale, my / scale, btn); }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderDirtBackground(g);
        float scale = getForcedScale(); int vWidth = (int)(this.width / scale);
        g.pose().pushPose(); g.pose().scale(scale, scale, 1.0f);
        LogoRenderer.render(g, vWidth);
        g.drawCenteredString(this.font, this.title, vWidth / 2, 20, 0xFFFFFF);
        float t = Mth.clamp((System.currentTimeMillis() - openTime) / (float)ANIM_DURATION, 0f, 1f);
        int widgetIndex = 0;
        for (var r : this.renderables) { if (r instanceof AbstractWidget w && widgetIndex < initialYPositions.size()) { float delayT = Mth.clamp((System.currentTimeMillis() - openTime - (widgetIndex * 30L)) / (float)ANIM_DURATION, 0f, 1f); float itemEase = backOut(delayT); int targetY = initialYPositions.get(widgetIndex); int startOffsetY = 30; w.setY((int) (targetY + startOffsetY * (1 - itemEase))); w.setAlpha(itemEase); widgetIndex++; } }
        super.render(g, (int)(mx/scale), (int)(my/scale), pt);
        g.pose().popPose();
    }
    private float backOut(float t) { float c1 = 1.70158f; float c3 = c1 + 1; return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2); }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}