package shake1227.rpgsetupscreen.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScreenData {
    public static class Def {
        public String uuid;
        public String title;
        public boolean isSystem;
        public List<Element> elements = new ArrayList<>();
        public List<String> executeCommands = new ArrayList<>();

        public Def() { this.uuid = UUID.randomUUID().toString(); }
        public Def(String title, boolean isSystem) {
            this();
            this.title = title;
            this.isSystem = isSystem;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("uuid", uuid);
            tag.putString("title", title);
            tag.putBoolean("isSystem", isSystem);

            ListTag cmdList = new ListTag();
            for (String cmd : executeCommands) {
                cmdList.add(StringTag.valueOf(cmd));
            }
            tag.put("commands", cmdList);

            ListTag list = new ListTag();
            for (Element e : elements) list.add(e.save());
            tag.put("elements", list);
            return tag;
        }

        public static Def load(CompoundTag tag) {
            Def d = new Def();
            d.uuid = tag.getString("uuid");
            d.title = tag.getString("title");
            d.isSystem = tag.getBoolean("isSystem");

            if (tag.contains("commands", 9)) { // 9 = List
                ListTag cmdList = tag.getList("commands", 8); // 8 = String
                for (int i = 0; i < cmdList.size(); i++) {
                    d.executeCommands.add(cmdList.getString(i));
                }
            } else if (tag.contains("cmd")) { // 旧データ互換
                String oldCmd = tag.getString("cmd");
                if (!oldCmd.isEmpty()) d.executeCommands.add(oldCmd);
            }

            ListTag list = tag.getList("elements", 10);
            for (int i = 0; i < list.size(); i++) {
                d.elements.add(Element.load(list.getCompound(i)));
            }
            return d;
        }
    }

    public static class Element {
        public String type;
        public int x, y, w, h;
        public String content;
        public int color = 0xFFFFFF;
        public boolean bold;
        public float scale = 1.0f;
        public String varName = "";
        public boolean required = false;

        public Element(String type) { this.type = type; }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("type", type);
            tag.putInt("x", x); tag.putInt("y", y);
            tag.putInt("w", w); tag.putInt("h", h);
            tag.putString("content", content == null ? "" : content);
            tag.putInt("color", color);
            tag.putBoolean("bold", bold);
            tag.putFloat("scale", scale);
            tag.putString("varName", varName);
            tag.putBoolean("required", required);
            return tag;
        }

        public static Element load(CompoundTag tag) {
            Element e = new Element(tag.getString("type"));
            e.x = tag.getInt("x"); e.y = tag.getInt("y");
            e.w = tag.getInt("w"); e.h = tag.getInt("h");
            e.content = tag.getString("content");
            e.color = tag.getInt("color");
            e.bold = tag.getBoolean("bold");
            e.scale = tag.contains("scale") ? tag.getFloat("scale") : 1.0f;
            e.varName = tag.getString("varName");
            e.required = tag.getBoolean("required");
            return e;
        }
    }
}