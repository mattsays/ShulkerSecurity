package com.minepunks.shulkersecurity.storage;


import com.google.gson.annotations.Expose;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class Backpack {

    public static class Listener implements org.bukkit.event.Listener {

        private final BackpackManager backpackManager;

        public Listener(BackpackManager backpackManager) {
            this.backpackManager = backpackManager;
        }

        @EventHandler
        public void onInteract(PlayerInteractEvent event) {
            if(event.isBlockInHand() && event.getPlayer().isSneaking())
                return;

            if(event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            if(!event.hasBlock()) {
                return;
            }

            Block block = event.getClickedBlock();

            if(!(block.getState() instanceof ShulkerBox)) {
                return;
            }

            ShulkerBox shulkerBox = (ShulkerBox) block.getState();

            if(!isCustomShulkerbox(shulkerBox)) {
                return;
            }

            event.setCancelled(true);

            UUID backpackId = UUID.fromString(shulkerBox.getCustomName());

            Optional<Backpack> optBackpack = this.backpackManager.getBackpack(backpackId);

            optBackpack.ifPresent(backpack -> {
                Player player = event.getPlayer();

                if(!backpack.canOpen(player) && !player.hasPermission("com.minepunks.shulkersecurit.bypass")) {
                    player.sendMessage(ChatColor.RED + "You cannot open others' backpack!");
                    return;
                }

                player.openInventory(backpack.getHolder().getInventory());
            });

            if(!optBackpack.isPresent()) {
                event.getPlayer().sendMessage(ChatColor.RED + "Couldn't open custom backpack. There was an internal error");
            }
        }

        @EventHandler
        public void onShulkerInventoryChange(InventoryMoveItemEvent event) {

            ShulkerBox shulkerBox;
            boolean isSource = false;

            if(event.getDestination().getHolder() instanceof ShulkerBox) {
                shulkerBox = (ShulkerBox) event.getDestination().getHolder();
            } else if(event.getSource().getHolder() instanceof ShulkerBox) {
                isSource = true;
                shulkerBox = (ShulkerBox) event.getSource().getHolder();
            } else {
                return;
            }

            if(!isCustomShulkerbox(shulkerBox)) {
                return;
            }

            event.setCancelled(true);

            UUID backpackId = UUID.fromString(shulkerBox.getCustomName());

            boolean finalIsSource = isSource;
            this.backpackManager.getBackpack(backpackId).ifPresent(backpack -> {

                Inventory backpackInv = backpack.holder.getInventory();

                if(finalIsSource) {
                    ItemStack firstItem = this.getFirstFreeItem(backpackInv);

                    if(firstItem == null) {
                        return;
                    }

                    Optional<ItemStack> itemsNotRemoved = backpackInv.removeItem(firstItem).values().stream().findFirst();

                    int amount = firstItem.getAmount();
                    if(itemsNotRemoved.isPresent()) {
                        amount = firstItem.getAmount() - itemsNotRemoved.get().getAmount();
                    }

                    Optional<ItemStack> itemsNotStored = event.getDestination().addItem(new ItemStack(firstItem.getType(), amount)).values().stream().findFirst();
                    itemsNotStored.ifPresent(backpackInv::addItem);

                } else {
                    event.getSource().removeItem(event.getItem());
                    Optional<ItemStack> itemsNotStored = backpackInv.addItem(event.getItem()).values().stream().findFirst();
                    itemsNotStored.ifPresent(itemStack -> event.getSource().addItem(itemStack));
                }
            });
        }

        private ItemStack getFirstFreeItem(Inventory inventory) {
            if(!inventory.iterator().hasNext()) {
                return null;
            }

            return inventory.iterator().next();
        }
    }

    @Expose
    protected int nth;

    @Expose
    private UUID id;

    @Expose
    protected UUID owner;

    @Expose
    protected ItemStack[] contents;

    private BackpackHolder holder;

    public Backpack(UUID owner, int nth) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.contents = new ItemStack[54];
        this.nth = nth;
    }

    public static boolean isCustomShulkerbox(ShulkerBox shulkerBox) {
        return shulkerBox.getCustomName() != null && shulkerBox.getCustomName().matches("[0-9a-f]{8}\\b-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-\\b[0-9a-f]{12}");
    }

    public UUID getID() {
        return id;
    }

    public boolean canOpen(Player player) {
        if(player.hasPermission("com.minepunks.shulkersecurit.bypass")) {
            return true;
        }

        return player.getUniqueId().equals(this.owner);
    }

    public BackpackHolder getHolder() {
        if(this.holder == null) {
            this.holder = new BackpackHolder(this);
        }

        return this.holder;
    }

    public boolean hasHolder() {
        return this.holder != null;
    }

    public void assign(ShulkerBox shulkerBox) {
        shulkerBox.setCustomName(this.id.toString());
        shulkerBox.getSnapshotInventory().clear();
        shulkerBox.getSnapshotInventory().setItem(0, new ItemStack(Material.GRASS));
        shulkerBox.update(true, true);
    }
}
