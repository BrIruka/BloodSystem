package com.irukamine.bloodSystem.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BloodTabCompleter implements TabCompleter {
    private final List<String> baseCommands = Arrays.asList("help", "info", "set", "add", "remove", "reload");
    private final List<String> setCommands = Arrays.asList("type", "volume", "maxvolume", "quality");
    private final List<String> addRemoveCommands = Arrays.asList("volume", "maxvolume");
    private final List<String> bloodTypes = Arrays.asList("A", "B", "AB", "O");
    private final List<String> rhFactors = Arrays.asList("+", "-");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        boolean isPlayer = sender instanceof Player;
        Player player = isPlayer ? (Player) sender : null;

        switch (args.length) {
            case 1:
                // /blood <help|info|set|add|remove|reload>
                if (sender.hasPermission("bloodsystem.admin")) {
                    completions.add("set");
                    completions.add("add");
                    completions.add("remove");
                }
                if (sender.hasPermission("bloodsystem.reload")) {
                    completions.add("reload");
                }
                if (isPlayer) {
                    completions.add("help");
                    completions.add("info");
                }
                break;

            case 2:
                // /blood set <type|volume|maxvolume|quality>
                if (args[0].equalsIgnoreCase("set") && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(setCommands);
                }
                // /blood add <volume|maxvolume>
                else if (args[0].equalsIgnoreCase("add") && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(addRemoveCommands);
                }
                // /blood remove <volume|maxvolume>
                else if (args[0].equalsIgnoreCase("remove") && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(addRemoveCommands);
                }
                // /blood info <player> - только для игроков
                else if (args[0].equalsIgnoreCase("info") && isPlayer) {
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case 3:
                // /blood set <type|volume|maxvolume|quality> <player>
                if (args[0].equalsIgnoreCase("set") && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                // /blood add <volume|maxvolume> <player>
                else if (args[0].equalsIgnoreCase("add") && (args[1].equalsIgnoreCase("volume") || args[1].equalsIgnoreCase("maxvolume")) && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                // /blood remove <volume|maxvolume> <player>
                else if (args[0].equalsIgnoreCase("remove") && (args[1].equalsIgnoreCase("volume") || args[1].equalsIgnoreCase("maxvolume")) && sender.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case 4:
                // /blood set type <player> <A|B|AB|O>
                if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("type")) {
                    completions.addAll(bloodTypes);
                }
                // /blood add <volume|maxvolume> <player> <value>
                // /blood remove <volume|maxvolume> <player> <value>
                break;

            case 5:
                // /blood set type <player> <A|B|AB|O> <+|->
                if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("type")) {
                    completions.addAll(rhFactors);
                }
                break;
        }

        // Фильтруем результаты по тому, что уже введено
        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }
}