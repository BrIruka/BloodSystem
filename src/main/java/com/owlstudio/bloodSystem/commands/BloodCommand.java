package com.owlstudio.bloodSystem.commands;

import com.owlstudio.bloodSystem.BloodSystem;
import com.owlstudio.bloodSystem.blood.PlayerBloodData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BloodCommand implements CommandExecutor {
    private final BloodSystem plugin;

    public BloodCommand(BloodSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("messages.errors.player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Базовая команда /blood
        if (args.length == 0) {
            showBloodInfo(player, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                handleInfoCommand(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            case "set":
                if (player.hasPermission("bloodsystem.admin")) {
                    handleSetCommand(player, args);
                } else {
                    player.sendMessage(plugin.getMessage("messages.errors.no-permission"));
                }
                break;
            case "reload":
                if (sender.hasPermission("bloodsystem.reload")) {
                    plugin.reloadPlugin();
                    sender.sendMessage(plugin.getMessage("messages.prefix") + " " +
                            plugin.getMessage("messages.reload.success"));
                } else {
                    sender.sendMessage(plugin.getMessage("messages.errors.no-permission"));
                }
                break;
            default:
                player.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }

        return true;
    }

    private void handleSetCommand(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            player.sendMessage(plugin.getMessage("messages.errors.player-not-found"));
            return;
        }

        PlayerBloodData bloodData = plugin.getPlayerBloodData(target.getUniqueId());
        if (bloodData == null) {
            player.sendMessage(plugin.getMessage("messages.errors.no-data"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "type":
                handleSetType(player, target, bloodData, args);
                break;
            case "volume":
                handleSetVolume(player, target, bloodData, args);
                break;
            case "quality":
                handleSetQuality(player, target, bloodData, args);
                break;
            default:
                player.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }
    }

    private void handleSetType(Player player, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            PlayerBloodData.BloodType type = PlayerBloodData.BloodType.valueOf(args[3].toUpperCase());
            boolean rhFactor = args.length > 4 ? args[4].equals("+") : true;

            bloodData.setBloodType(type);
            bloodData.setRhFactor(rhFactor);

            // Сохраняем изменения
            plugin.getDataManager().savePlayerData(bloodData);

            player.sendMessage(plugin.getMessage("messages.blood.set.type")
                    .replace("%player%", target.getName())
                    .replace("%bloodtype%", type.name())
                    .replace("%rhfactor%", rhFactor ? "+" : "-"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(plugin.getMessage("messages.errors.invalid_type"));
        }
    }

    private void handleSetVolume(Player player, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            double volume = Double.parseDouble(args[3]);
            bloodData.setVolume(volume);

            // Обновляем здоровье и сохраняем изменения
            bloodData.updatePlayerHealth(target);
            plugin.getDataManager().savePlayerData(bloodData);

            player.sendMessage(plugin.getMessage("messages.blood.set.volume")
                    .replace("%player%", target.getName())
                    .replace("%volume%", String.format("%.1f", volume)));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
        }
    }

    private void handleSetQuality(Player player, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            double quality = Double.parseDouble(args[3]);
            bloodData.setQuality(quality);

            // Сохраняем изменения
            plugin.getDataManager().savePlayerData(bloodData);

            player.sendMessage(plugin.getMessage("messages.blood.set.quality")
                    .replace("%player%", target.getName())
                    .replace("%quality%", String.format("%.1f", quality)));
        } catch (NumberFormatException e) {
            player.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
        }
    }

    private void showHelp(Player player) {
        String prefix = plugin.getMessage("messages.prefix") + " ";
        player.sendMessage(prefix + plugin.getMessage("messages.help.title"));
        player.sendMessage(plugin.getMessage("messages.help.commands.blood"));
        player.sendMessage(plugin.getMessage("messages.help.commands.info"));
        player.sendMessage(plugin.getMessage("messages.help.commands.help"));

        if (player.hasPermission("bloodsystem.admin")) {
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.title"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.set_type"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.set_volume"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.set_quality"));
        }

        if (player.hasPermission("bloodsystem.reload")) {
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.reload"));
        }
    }

    private void handleInfoCommand(Player player, String[] args) {
        if (args.length > 1) {
            // Проверка прав на просмотр информации других игроков
            if (!player.hasPermission("bloodsystem.info.others")) {
                player.sendMessage(plugin.getMessage("messages.errors.no-permission"));
                return;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(plugin.getMessage("messages.errors.player-not-found"));
                return;
            }
            showBloodInfo(player, target);
        } else {
            showBloodInfo(player, player);
        }
    }

    private void showBloodInfo(Player sender, Player target) {
        PlayerBloodData bloodData = plugin.getPlayerBloodData(target.getUniqueId());

        String prefix = plugin.getMessage("messages.prefix") + " ";

        // Отправляем информацию о крови
        sender.sendMessage(prefix + plugin.getMessage("messages.blood.info")
                .replace("%bloodtype%", bloodData.getBloodType().name())
                .replace("%rhfactor%", bloodData.getRhFactor()));

        sender.sendMessage(prefix + plugin.getMessage("messages.blood.volume")
                .replace("%volume%", String.format("%.1f", bloodData.getVolume())));

        sender.sendMessage(prefix + plugin.getMessage("messages.blood.quality")
                .replace("%quality%", String.format("%.1f", bloodData.getQuality())));
    }
}