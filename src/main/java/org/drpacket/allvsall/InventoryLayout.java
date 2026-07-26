package org.drpacket.allvsall;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryLayout {
    private static final int ARMOR_SIZE = 4;
    private static final int CONTENT_SIZE = 36;

    private String[] armorContents = new String[ARMOR_SIZE];
    private String[] contents = new String[CONTENT_SIZE];
    private String offhand = "AIR:0";

    public ItemStack[] getArmorContents() {
        return decodeItems(armorContents);
    }

    public void setArmorContents(ItemStack[] armorContents) {
        this.armorContents = armorContents == null ? new String[ARMOR_SIZE] : encodeItems(armorContents, ARMOR_SIZE);
    }

    public ItemStack[] getContents() {
        return decodeItems(contents);
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents == null ? new String[CONTENT_SIZE] : encodeItems(contents, CONTENT_SIZE);
    }

    public ItemStack getOffhand() {
        return decodeItem(offhand);
    }

    public void setOffhand(ItemStack offhand) {
        this.offhand = encodeItem(offhand);
    }

    public String serialize() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("armor", serializeItems(armorContents));
        config.set("contents", serializeItems(contents));
        config.set("offhand", offhand);
        return config.saveToString();
    }

    public static InventoryLayout deserialize(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return new InventoryLayout();
        }

        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(serialized);
        } catch (Exception exception) {
            return new InventoryLayout();
        }

        InventoryLayout layout = new InventoryLayout();
        layout.armorContents = decodeSerializedItems(config.getList("armor"));
        layout.contents = decodeSerializedItems(config.getList("contents"));
        layout.offhand = config.getString("offhand", "AIR:0");
        return layout;
    }

    private static java.util.List<?> serializeItems(String[] items) {
        java.util.List<Object> serialized = new java.util.ArrayList<>();
        for (String item : items) {
            serialized.add(item == null ? "AIR:0" : item);
        }
        return serialized;
    }

    private static String[] encodeItems(ItemStack[] items, int size) {
        String[] encoded = new String[size];
        for (int index = 0; index < size; index++) {
            encoded[index] = index < items.length ? encodeItem(items[index]) : "AIR:0";
        }
        return encoded;
    }

    private static String encodeItem(ItemStack item) {
        if (item == null || item.getType() == null || item.getType().isAir()) {
            return "AIR:0";
        }
        return item.getType().name() + ":" + Math.max(1, item.getAmount());
    }

    private static ItemStack[] decodeItems(String[] encoded) {
        ItemStack[] items = new ItemStack[encoded == null ? 0 : encoded.length];
        for (int index = 0; index < items.length; index++) {
            items[index] = decodeItem(encoded[index]);
        }
        return items;
    }

    private static ItemStack decodeItem(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }
        String[] parts = encoded.split(":", 2);
        Material material = Material.matchMaterial(parts[0]);
        if (material == null || material == Material.AIR) {
            return null;
        }
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new ItemStack(material, Math.max(1, amount));
    }

    private static String[] decodeSerializedItems(List<?> rawItems) {
        String[] encoded = new String[rawItems == null ? 0 : rawItems.size()];
        for (int index = 0; index < encoded.length; index++) {
            Object raw = rawItems.get(index);
            encoded[index] = raw == null ? "AIR:0" : String.valueOf(raw);
        }
        return encoded;
    }
}
