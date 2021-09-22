package com.minepunks.shulkersecurity.storage;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class BackpackHolder implements InventoryHolder {

    private final Backpack backpack;
    private final Inventory inventory;

    public BackpackHolder(Backpack backpack) {
        this.backpack = backpack;
        this.inventory = Bukkit.createInventory(this, 54, "Backpack #" + backpack.nth);

        for (int i = 0; i < backpack.contents.length; i++) {
            ItemStack itemStack = backpack.contents[i];
            if(itemStack == null) continue;
            inventory.setItem(i, itemStack);
        }
    }

    public void update() {
        this.backpack.contents = this.inventory.getContents();
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }
}
