package shake1227.rpgsetupscreen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CommandListScreen extends Screen {
    private final Screen parent;
    private final List<String> commands;
    private CommandList list;
    private EditBox input;

    public CommandListScreen(Screen parent, List<String> commands) {
        super(Component.literal("Command Editor"));
        this.parent = parent;
        this.commands = commands;
    }

    @Override
    protected void init() {
        this.list = new CommandList(this.minecraft, this.width, this.height, 40, this.height - 60, 24);
        for (String cmd : commands) {
            this.list.addEntryToPublic(new CommandEntry(cmd));
        }
        this.addRenderableWidget(this.list);

        this.input = new EditBox(this.font, 20, this.height - 50, this.width - 150, 20, Component.literal(""));
        this.input.setMaxLength(256);
        this.addRenderableWidget(this.input);

        this.addRenderableWidget(Button.builder(Component.literal("Add"), b -> {
            String val = input.getValue();
            if (!val.isEmpty()) {
                commands.add(val);
                list.addEntryToPublic(new CommandEntry(val));
                input.setValue("");
            }
        }).bounds(this.width - 120, this.height - 50, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> {
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 100, this.height - 25, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        this.renderBackground(g);
        this.list.render(g, mx, my, pt);
        g.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(g, mx, my, pt);
    }

    @Override public boolean isPauseScreen() { return false; }

    class CommandList extends ObjectSelectionList<CommandEntry> {
        public CommandList(Minecraft mc, int w, int h, int y0, int y1, int itemH) {
            super(mc, w, h, y0, y1, itemH);
        }

        public void addEntryToPublic(CommandEntry entry) {
            this.addEntry(entry);
        }

        @Override public int getRowWidth() { return 300; }
        @Override protected int getScrollbarPosition() { return this.width / 2 + 160; }
    }

    class CommandEntry extends ObjectSelectionList.Entry<CommandEntry> {
        private final String cmd;
        private final Button deleteBtn;

        public CommandEntry(String cmd) {
            this.cmd = cmd;
            this.deleteBtn = Button.builder(Component.literal("X"), b -> {
                commands.remove(cmd);
                CommandListScreen.this.init(CommandListScreen.this.minecraft, CommandListScreen.this.width, CommandListScreen.this.height);
            }).bounds(0, 0, 20, 20).build();
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mx, int my, boolean hovered, float pt) {
            g.drawString(font, cmd, left + 5, top + 6, 0xFFFFFF, false);
            deleteBtn.setX(left + width - 25);
            deleteBtn.setY(top);
            deleteBtn.render(g, mx, my, pt);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int btn) {
            if (deleteBtn.mouseClicked(mx, my, btn)) return true;
            return false;
        }

        @Override public Component getNarration() { return Component.literal(cmd); }
    }
}