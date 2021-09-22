package com.minepunks.shulkersecurity.commands;

import com.minepunks.shulkersecurity.ShulkerSecurity;
import com.minepunks.shulkersecurity.storage.Backpack;
import com.minepunks.shulkersecurity.storage.BackpackManager;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Backpacks extends Command {

    private final BackpackManager backpackManager;

    public Backpacks(BackpackManager backpackManager) {
        super("backpacks");
        this.backpackManager = backpackManager;

        this.setHelp(true);

        this.addSubCommand("create", new SubCommand() {
            @Override
            protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
                return commandSender.hasPermission("com.minepunks.backpacks.create");
            }

            @Override
            protected Component getDescription() {
                return MineDown.parse("- Use this command to create a backpack");
            }

            @Override
            protected void execute(CommandSender commandSender, String[] arguments) {
                if (!(commandSender instanceof Player)) {
                    Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
                    senderAudience.sendMessage(MineDown.parse("[You must be a player to run this command.](color=red)"));
                    return;
                }

                Player player = (Player) commandSender;
                Audience playerAudience = ShulkerSecurity.getInstance().adventure().player(player);

                if (!backpackManager.createBackpack(player)) {
                    playerAudience.sendMessage(MineDown.parse("[You reached max backpacks amount!](color=red)"));
                } else {
                    playerAudience.sendMessage(MineDown.parse("[Backpack created successfully! You can create other " + backpackManager.getRemainingMaxBackpacks(player) + " backpacks](color=green)"));
                }
            }
        });
        this.addSubCommand("open", new SubCommand() {
            @Override
            protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
                return commandSender.hasPermission("com.minepunks.backpacks.open");
            }

            @Override
            protected Component getDescription() {
                return MineDown.parse("<backpack nth> - Use this command to open a backpack");
            }

            private void executeAdmin(Player player, Audience playerAudience, String[] arguments) {
                if(!player.hasPermission("com.minepunks.backpacks.open.others")) {
                    playerAudience.sendMessage(MineDown.parse("[You don't have permissions to view other's backpacks](color=red)"));
                    return;
                }

                String playerName = arguments[0];

                Optional<Player> optOtherPlayer = ShulkerSecurity.getPlayer(playerName);

                if(!optOtherPlayer.isPresent()) {
                    playerAudience.sendMessage(MineDown.parse("[Player not found](color=red)"));
                    return;
                }

                Player otherPlayer = optOtherPlayer.get();

                int nth = 1;

                if(arguments.length == 2) {
                    String nthString = arguments[1];
                    try {
                        nth = Integer.parseInt(nthString);
                    } catch (Exception e) {
                        playerAudience.sendMessage(MineDown.parse("[Invalid backpack number](color=red)"));
                        return;
                    }
                } else if(arguments.length > 2) {
                    playerAudience.sendMessage(MineDown.parse("[Invalid syntax. Use /backpacks open <player> <nth default 1>](color=red)"));
                    return;
                }

                Optional<Backpack> optBackpack = backpackManager.getBackpack(otherPlayer, nth);
                optBackpack.ifPresent(backpack -> player.openInventory(backpack.getHolder().getInventory()));

                if(!optBackpack.isPresent()) {
                    playerAudience.sendMessage(MineDown.parse("[" + otherPlayer.getDisplayName() + "'s backpack #" + nth + " not found.](color=red)"));
                } else {
                    playerAudience.sendMessage(MineDown.parse("[" + otherPlayer.getDisplayName() + "'s backpack #" + nth + " opened.](color=green)"));
                }
            }

            private void executeUser(Player player, Audience playerAudience, String[] arguments) {
                int nth = 1;

                if(arguments.length == 1) {

                    String nthString = arguments[0];
                    try {
                        nth = Integer.parseInt(nthString);
                    } catch (Exception e) {
                        playerAudience.sendMessage(MineDown.parse("[Invalid backpack number](color=red)"));
                        return;
                    }

                }

                Optional<Backpack> optBackpack = backpackManager.getBackpack(player, nth);
                optBackpack.ifPresent(backpack -> player.openInventory(backpack.getHolder().getInventory()));

                if(!optBackpack.isPresent()) {
                    if(nth == 1) {
                        playerAudience.sendMessage(MineDown.parse("[You don't have any backpacks.](color=red)"));
                    } else {
                        playerAudience.sendMessage(MineDown.parse("[You don't have a backpack with specified number.](color=red)"));
                    }
                } else {
                    playerAudience.sendMessage(MineDown.parse("[Backpack #" + nth + " opened.](color=green)"));
                }
            }

            @Override
            protected void execute(CommandSender commandSender, String[] arguments) {
                if (!(commandSender instanceof Player)) {
                    Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
                    senderAudience.sendMessage(MineDown.parse("[You must be a player to run this command.](color=red)"));
                    return;
                }

                Player player = (Player) commandSender;
                Audience playerAudience = ShulkerSecurity.getInstance().adventure().player(player);

                if(arguments.length <= 1) {
                    this.executeUser(player, playerAudience, arguments);
                } else if(arguments.length == 2) {
                    this.executeAdmin(player, playerAudience, arguments);
                } else {
                    boolean hasAdminPerm = player.hasPermission("com.minepunks.backpacks.open.others");
                    playerAudience.sendMessage(MineDown.parse("[Invalid syntax. Use /backpacks open <nth default 1>" + (hasAdminPerm ? " or /backpacks open <player> <nth default 1>" : "") + "](color=red)"));
                }

            }

            @Override
            protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {

                if(!(commandSender instanceof Player)) {
                    return Optional.empty();
                }

                Player player = (Player) commandSender;

                if(arguments.length == 2) {

                    if(player.hasPermission("com.minepunks.backpacks.open.others")) {
                        String playerName = arguments[0];

                        Optional<Player> otherPlayer = ShulkerSecurity.getPlayer(playerName);

                        if(otherPlayer.isPresent()) {
                            int lastNth = backpackManager.getLastNth(otherPlayer.get());

                            if(lastNth > 0) {
                                List<String> suggestions = new ArrayList<>();
                                for (int i = lastNth; i > 0; i--) {
                                    if(arguments[1] == null || arguments[1].isEmpty() || arguments[1].startsWith(i + "")) {
                                        suggestions.add((lastNth - i + 1) + "");
                                    }
                                }
                                return Optional.of(suggestions);
                            }
                        }
                    }
                } else if(arguments.length == 1) {
                    List<String> suggestions = new ArrayList<>();
                    if(player.hasPermission("com.minepunks.backpacks.open.others")) {
                        if(arguments[0] == null || arguments[0].isEmpty()) {
                            suggestions.addAll(ShulkerSecurity.getPlayerNames());
                        } else {
                            ShulkerSecurity.getPlayerNames().forEach(playerName -> {
                                if(playerName.startsWith(arguments[0])) {
                                    suggestions.add(playerName);
                                }
                            });
                        }
                    }

                    int lastNth = backpackManager.getLastNth(player);

                    if(lastNth > 0) {
                        for (int i = lastNth; i > 0; i--) {
                            if(arguments[0] == null || arguments[0].isEmpty() || arguments[0].startsWith(i + "")) {
                                suggestions.add((lastNth - i + 1) + "");
                            }
                        }
                    }

                    return Optional.of(suggestions);
                }

                return Optional.empty();
            }
        });
        this.addSubCommand("assign", new SubCommand() {
            @Override
            protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
                return commandSender.hasPermission("com.minepunks.backpacks.assign");
            }

            @Override
            protected Component getDescription() {
                return MineDown.parse("<backpack nth> - Use this command to assign a backpack to a specific shulkerbox");
            }

            @Override
            protected void execute(CommandSender commandSender, String[] arguments) {
                if(!(commandSender instanceof Player)) {
                    Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
                    senderAudience.sendMessage(MineDown.parse("[You must be a player to run this command.](color=red)"));
                    return;
                }


                Player player = (Player) commandSender;
                Audience playerAudience = ShulkerSecurity.getInstance().adventure().player(player);

                ShulkerBox shulkerBox;

                Block targetBlock = player.getTargetBlock(null, 100);


                if(!(targetBlock.getState() instanceof ShulkerBox)) {
                    playerAudience.sendMessage(MineDown.parse("[You are not looking at a shulkerbox.](color=red)"));
                    return;
                } else {
                    shulkerBox = (ShulkerBox) targetBlock.getState();
                }

                int nth = 1;

                if(arguments.length == 1) {

                    String nthString = arguments[0];
                    try {
                        nth = Integer.parseInt(nthString);
                    } catch (Exception e) {
                        playerAudience.sendMessage(MineDown.parse("[Invalid backpack number](color=red)"));
                        return;
                    }

                }

                Optional<Backpack> optBackpack = backpackManager.getBackpack(player, nth);

                if(!optBackpack.isPresent()) {
                    playerAudience.sendMessage(MineDown.parse("[Backpack #" + nth + " not found.](color=red)"));
                    return;
                }

                optBackpack.get().assign(shulkerBox);
                playerAudience.sendMessage(MineDown.parse("[Backpack #" + nth + " assigned.](color=green)"));
            }
        });
        this.addSubCommand("list", new SubCommand() {
            @Override
            protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
                return commandSender.hasPermission("com.minepunks.backpacks.list");
            }

            @Override
            protected Component getDescription() {
                return MineDown.parse("- Use this command to list backpacks");
            }

            @Override
            protected void execute(CommandSender commandSender, String[] arguments) {
                if(!(commandSender instanceof Player)) {
                    Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
                    senderAudience.sendMessage(MineDown.parse("[You must be a player to run this command.](color=red)"));
                    return;
                }


                Player player = (Player) commandSender;
                Audience playerAudience = ShulkerSecurity.getInstance().adventure().player(player);

                int lastNth = backpackManager.getLastNth(player);

                playerAudience.sendMessage(MineDown.parse("You have [" + lastNth + "](color=green) backpacks"));
            }
        });
    }

    @Override
    protected Component getDescription() {
        return MineDown.parse("Base command.");
    }

    @Override
    protected void execute(CommandSender commandSender, String[] arguments) {
        if(!(commandSender instanceof Player)) {
            Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
            senderAudience.sendMessage(MineDown.parse("[You must be a player to run this command.](color=red)"));
            return;
        }

        Player player = (Player) commandSender;
        Audience playerAudience = ShulkerSecurity.getInstance().adventure().player(player);

        playerAudience.sendMessage(MineDown.parse("Invalid command. Type /backpacks help for info"));
    }

    @Override
    protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
        return commandSender.hasPermission("com.minepunks.backpacks.base");
    }
}