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
    private final List<String> addRemoveCommands = Arrays.asList("volume");
    private final List<String> bloodTypes = Arrays.asList("A", "B", "AB", "O");
    private final List<String> rhFactors = Arrays.asList("+", "-");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;

        switch (args.length) {
            case 1:
                // /blood <help|info|set|add|remove|reload>
                if (player.hasPermission("bloodsystem.admin")) {
                    completions.add("set");
                    completions.add("add");
                    completions.add("remove");
                }
                if (player.hasPermission("bloodsystem.reload")) {
                    completions.add("reload");
                }
                completions.add("help");
                completions.add("info");
                break;

            case 2:
                // /blood set <type|volume|maxvolume|quality>
                if (args[0].equalsIgnoreCase("set") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(setCommands);
                }
                // /blood add <volume>
                else if (args[0].equalsIgnoreCase("add") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(addRemoveCommands);
                }
                // /blood remove <volume>
                else if (args[0].equalsIgnoreCase("remove") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(addRemoveCommands);
                }
                // /blood info <player>
                else if (args[0].equalsIgnoreCase("info")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case 3:
                // /blood set <type|volume|maxvolume|quality> <player>
                if (args[0].equalsIgnoreCase("set") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                // /blood add volume <player>
                else if (args[0].equalsIgnoreCase("add") && args[1].equalsIgnoreCase("volume") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                // /blood remove volume <player>
                else if (args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("volume") && player.hasPermission("bloodsystem.admin")) {
                    completions.addAll(getOnlinePlayerNames());
                }
                break;

            case 4:
                // /blood set type <player> <A|B|AB|O>
                if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("type")) {
                    completions.addAll(bloodTypes);
                }
                // /blood add volume <player> <value>
                // Для числовых значений автодополнение не требуется
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