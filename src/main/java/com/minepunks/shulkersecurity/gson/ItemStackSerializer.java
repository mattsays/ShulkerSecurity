package com.minepunks.shulkersecurity.gson;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

public class ItemStackSerializer implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Map<String, Object> map = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());
        if(map == null || map.isEmpty()) {
            return new ItemStack(Material.AIR);
        }

        return ItemStack.deserialize(map);
    }

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        return gson.toJsonTree(src.serialize());
    }
}
