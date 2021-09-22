package com.minepunks.shulkersecurity.storage;

import com.google.gson.*;
import com.minepunks.shulkersecurity.ShulkerSecurity;
import com.minepunks.shulkersecurity.gson.ItemStackSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class BackpackManager {

    private final Gson gson;

    private final Map<UUID, Backpack> backpacks;
    private final File backpackFile;

    private final int maxBackpacks;
    private final BukkitTask updateTask;

    public BackpackManager(ShulkerSecurity shulkerSecurity, File backpacksFile, int autoSaveDelay, int maxBackpacks) {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithoutExposeAnnotation()
                .registerTypeAdapter(ItemStack.class, new ItemStackSerializer())
                .create();
        this.backpackFile = backpacksFile;
        this.backpacks = new HashMap<>();
        this.loadBackpacks();

        this.maxBackpacks = maxBackpacks;
        this.updateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(shulkerSecurity, this::saveBackpacks, 0L, autoSaveDelay * 60 * 20L);
    }

    public int getLastNth(Player player) {
        int lastNth = 0;

        for (Backpack backpack : this.backpacks.values()) {
            if(!backpack.owner.equals(player.getUniqueId())) {
                continue;
            }
            lastNth = Math.max(lastNth, backpack.nth);
        }

        return lastNth;
    }

    public int getRemainingMaxBackpacks(Player player) {
        return this.maxBackpacks - this.getLastNth(player);
    }

    private void loadBackpacks() {
        try(FileReader fileReader = new FileReader(this.backpackFile)) {
            JsonArray jsonArray = JsonParser.parseReader(fileReader).getAsJsonArray();

            jsonArray.forEach(jsonElement -> {
                Backpack backpack = this.gson.fromJson(jsonElement, Backpack.class);
                this.backpacks.put(backpack.getID(), backpack);
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveBackpacks() {
        try(FileWriter fileWriter = new FileWriter(this.backpackFile)) {
            JsonArray jsonArray = new JsonArray();

            this.backpacks.forEach((id, backpack) -> {
                if(backpack.hasHolder()) {
                    backpack.getHolder().update();
                }
                JsonElement jsonBackpack = this.gson.toJsonTree(backpack, Backpack.class);
                jsonArray.add(jsonBackpack);
            });

            this.gson.toJson(jsonArray, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.updateTask.cancel();
        this.saveBackpacks();
    }

    public Optional<Backpack> getBackpack(UUID id) {
        return Optional.ofNullable(this.backpacks.get(id));
    }

    public Optional<Backpack> getBackpack(Player player, int nth) {
        return this.backpacks.values().stream()
                .filter(backpack -> backpack.owner.equals(player.getUniqueId()) && backpack.nth == nth)
                .findFirst();
    }

    public boolean createBackpack(Player player) {
        int lastNth = this.getLastNth(player);

        if(lastNth >= this.maxBackpacks) {
            return false;
        }

        Backpack backpack = new Backpack(player.getUniqueId(),  lastNth + 1);
        this.backpacks.put(backpack.getID(), backpack);
        this.saveBackpacks();
        return true;
    }


}
