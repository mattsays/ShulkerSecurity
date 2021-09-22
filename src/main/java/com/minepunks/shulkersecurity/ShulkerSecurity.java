package com.minepunks.shulkersecurity;

import com.minepunks.shulkersecurity.commands.Backpacks;
import com.minepunks.shulkersecurity.storage.Backpack;
import com.minepunks.shulkersecurity.storage.BackpackManager;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ShulkerSecurity extends JavaPlugin implements Listener {

    private static ShulkerSecurity INSTANCE;
    private BackpackManager backpackManager;
    private BukkitAudiences adventure;
    private FileConfiguration config;

    public static ShulkerSecurity getInstance() {
        return INSTANCE;
    }

    public static Optional<Player> getPlayer(String name) {

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equals(name)) {
                return Optional.of(player);
            }
        }

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if (player.getName().equals(name)) {
                return Optional.of(player.getPlayer());
            }
        }

        return Optional.empty();
    }

    public static List<String> getPlayerNames() {
        List<String> playerNames = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            if(playerNames.contains(player.getName())) {
                continue;
            }
            playerNames.add(player.getName());
        }

        return playerNames;
    }

    public @NotNull BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        this.adventure = BukkitAudiences.create(this);

        File backpacks = new File(this.getDataFolder(), "backpacks.json");

        if (!backpacks.exists()) {
            this.saveResource("backpacks.json", false);
        }

        this.saveDefaultConfig();
        this.config = this.getConfig();
        this.backpackManager = new BackpackManager(this, backpacks, this.config.getInt("autoSaveDelay", 5), this.config.getInt("maxBackpacks", 3));
        Bukkit.getPluginManager().registerEvents(new Backpack.Listener(this.backpackManager), this);
        Bukkit.getPluginManager().registerEvents(this, this);
        new Backpacks(this.backpackManager).register(this);
    }

    @Override
    public void onDisable() {
        if (this.backpackManager != null) {
            this.backpackManager.stop();
        }
        INSTANCE = null;
    }

}
