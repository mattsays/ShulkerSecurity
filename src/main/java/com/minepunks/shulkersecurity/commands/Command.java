package com.minepunks.shulkersecurity.commands;

import com.minepunks.shulkersecurity.ShulkerSecurity;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class Command implements CommandExecutor, TabExecutor {

    public abstract static class SubCommand extends Command {
        public SubCommand() {
            super(null);
        }

    }

    private final HashMap<String, SubCommand> subCommands;
    protected String name;

    public Command(String name) {
        this.subCommands = new HashMap<>();
        this.name = name;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if(this.handlePermissions(sender, args)) {
            this.handleCommands(sender, args);
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String alias, @NotNull String[] args) {
        return this.handleSuggestions(sender, args);
    }

    protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
        return Optional.empty();
    }

    protected void execute(CommandSender commandSender, String[] arguments) {

    }

    protected Component getDescription() {
        return Component.text("");
    }

    protected abstract boolean hasPermissions(CommandSender commandSender, String[] arguments);

    protected void addSubCommand(String subCommandLabel, SubCommand command) {
        command.name = this.name + " " + subCommandLabel;
        this.subCommands.put(subCommandLabel, command);
    }

    protected void setHelp(boolean enable) {
        if(!enable) {
            this.subCommands.remove("help");
            return;
        }

        Command base = this;

        this.addSubCommand("help", new SubCommand() {
            @Override
            protected Optional<List<String>> getSuggestions(CommandSender commandSender, String[] arguments) {
                return Optional.empty();
            }

            @Override
            protected void execute(CommandSender commandSender, String[] arguments) {
                Audience senderAudience = ShulkerSecurity.getInstance().adventure().sender(commandSender);
                base.displayHelp(senderAudience);
            }

            @Override
            protected boolean hasPermissions(CommandSender commandSender, String[] arguments) {
                return true;
            }
        });
    }

    protected void displayHelp(Audience audience) {
        audience.sendMessage(MineDown.parse("/" + name + " ").append(getDescription()));
        subCommands.forEach((subName, subCommand) -> {
            if(subName.equals("help"))
                return;

            subCommand.displayHelp(audience);
        });
    }

    private void handleCommands(CommandSender commandSender, String[] arguments) {
        if (arguments.length < 1) {
            this.execute(commandSender, arguments);
            return;
        }

        String[] subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        Command subCommand = subCommands.get(arguments[0]);

        if(subCommand == null) {
            subCommand = this;
        }

        if (subCommand != this)
            subCommand.handleCommands(commandSender, subCommandArgs);
        else
            this.execute(commandSender, arguments);
    }

    private boolean handlePermissions(CommandSender commandSender, String[] arguments) {
        if (!this.hasPermissions(commandSender, arguments))
            return false;

        if (arguments.length > 1) {
            String[] subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
            Command subCommand = subCommands.get(arguments[0]);

            if(subCommand == null) {
                subCommand = this;
            }

            return subCommand.handlePermissions(commandSender, subCommandArgs);
        }

        return true;
    }

    private List<String> handleSuggestions(CommandSender commandSender, String[] arguments) {
        if (arguments.length <= 1) {
            List<String> suggestions = this.getSuggestions(commandSender, arguments).orElse(new ArrayList<>());
            suggestions.addAll(this.subCommands.keySet());
            Collections.sort(suggestions);
            return suggestions;
        }

        String[] subCommandArgs = Arrays.copyOfRange(arguments, 1, arguments.length);
        Command subCommand = subCommands.get(arguments[0]);

        if(subCommand == null) {
            subCommand = this;
        }

        if (subCommand != this)
            return subCommand.handleSuggestions(commandSender, subCommandArgs);
        else
            return this.getSuggestions(commandSender, arguments).orElse(new ArrayList<>());
    }

    public void register(JavaPlugin plugin) {
        PluginCommand pluginCommand = plugin.getCommand(this.name);
        pluginCommand.setExecutor(this);
        pluginCommand.setTabCompleter(this);
    }

}
