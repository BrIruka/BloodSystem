package com.irukamine.bloodSystem.commands;

import com.irukamine.bloodSystem.BloodSystem;
import com.irukamine.bloodSystem.blood.PlayerBloodData;
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
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getMessage("messages.errors.player-only"));
                return true;
            }
            Player player = (Player) sender;
            showBloodInfo(player, player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "info":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessage("messages.errors.player-only"));
                    return true;
                }
                handleInfoCommand((Player) sender, args);
                break;
            case "help":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(plugin.getMessage("messages.errors.player-only"));
                    return true;
                }
                showHelp((Player) sender);
                break;
            case "set":
                if (sender.hasPermission("bloodsystem.admin")) {
                    handleSetCommand(sender, args);
                } else {
                    sender.sendMessage(plugin.getMessage("messages.errors.no-permission"));
                }
                break;
            case "add":
                if (sender.hasPermission("bloodsystem.admin")) {
                    handleAddCommand(sender, args);
                } else {
                    sender.sendMessage(plugin.getMessage("messages.errors.no-permission"));
                }
                break;
            case "remove":
                if (sender.hasPermission("bloodsystem.admin")) {
                    handleRemoveCommand(sender, args);
                } else {
                    sender.sendMessage(plugin.getMessage("messages.errors.no-permission"));
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
                sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }

        return true;
    }

    private void handleAddCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.player-not-found"));
            return;
        }

        PlayerBloodData bloodData = plugin.getPlayerBloodData(target.getUniqueId());
        if (bloodData == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.no-data"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "volume":
                try {
                    double amount = Double.parseDouble(args[3]);
                    double oldVolume = bloodData.getVolume();
                    
                    bloodData.addVolume(amount);
                    
                    bloodData.updatePlayerHealth(target);
                    plugin.getDataManager().savePlayerData(bloodData);

                    sender.sendMessage(plugin.getMessage("messages.blood.add.volume")
                            .replace("%player%", target.getName())
                            .replace("%amount%", String.format("%.1f", amount))
                            .replace("%oldvolume%", String.format("%.1f", oldVolume))
                            .replace("%volume%", String.format("%.1f", bloodData.getVolume())));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
                }
                break;
            case "maxvolume":
                try {
                    double amount = Double.parseDouble(args[3]);
                    bloodData.addMaxVolume(amount);
                    
                    bloodData.updatePlayerHealth(target);
                    plugin.getDataManager().savePlayerData(bloodData);

                    sender.sendMessage(plugin.getMessage("messages.blood.add.maxvolume")
                            .replace("%player%", target.getName())
                            .replace("%amount%", String.format("%.1f", amount))
                            .replace("%maxvolume%", String.format("%.1f", bloodData.getMaxVolume())));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
                }
                break;
            default:
                sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }
    }

    private void handleRemoveCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.player-not-found"));
            return;
        }

        PlayerBloodData bloodData = plugin.getPlayerBloodData(target.getUniqueId());
        if (bloodData == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.no-data"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "volume":
                try {
                    double amount = Double.parseDouble(args[3]);
                    double oldVolume = bloodData.getVolume();
                    
                    bloodData.removeVolume(amount);
                    
                    bloodData.updatePlayerHealth(target);
                    plugin.getDataManager().savePlayerData(bloodData);

                    sender.sendMessage(plugin.getMessage("messages.blood.remove.volume")
                            .replace("%player%", target.getName())
                            .replace("%amount%", String.format("%.1f", amount))
                            .replace("%oldvolume%", String.format("%.1f", oldVolume))
                            .replace("%volume%", String.format("%.1f", bloodData.getVolume())));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
                }
                break;
            case "maxvolume":
                try {
                    double amount = Double.parseDouble(args[3]);
                    double oldMaxVolume = bloodData.getMaxVolume();
                    
                    double minRequiredVolume = 500.0; // BLOOD_PER_HEART равен 500.0
                    
                    if (oldMaxVolume - amount < minRequiredVolume) {
                        sender.sendMessage(plugin.getMessage("messages.blood_limits.min_required")
                                .replace("%minvolume%", String.valueOf(minRequiredVolume)));
                        sender.sendMessage(plugin.getMessage("messages.blood_limits.max_remove")
                                .replace("%maxamount%", String.format("%.1f", oldMaxVolume - minRequiredVolume)));
                        return;
                    }
                    
                    bloodData.removeMaxVolume(amount);
                    
                    if (bloodData.getVolume() > bloodData.getMaxVolume()) {
                        bloodData.setVolume(bloodData.getMaxVolume());
                    }
                    
                    bloodData.updatePlayerHealth(target);
                    plugin.getDataManager().savePlayerData(bloodData);

                    sender.sendMessage(plugin.getMessage("messages.blood.remove.maxvolume")
                            .replace("%player%", target.getName())
                            .replace("%amount%", String.format("%.1f", amount))
                            .replace("%oldmaxvolume%", String.format("%.1f", oldMaxVolume))
                            .replace("%maxvolume%", String.format("%.1f", bloodData.getMaxVolume())));
                } catch (NumberFormatException e) {
                    sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
                }
                break;
            default:
                sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.player-not-found"));
            return;
        }

        PlayerBloodData bloodData = plugin.getPlayerBloodData(target.getUniqueId());
        if (bloodData == null) {
            sender.sendMessage(plugin.getMessage("messages.errors.no-data"));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "type":
                handleSetType(sender, target, bloodData, args);
                break;
            case "volume":
                handleSetVolume(sender, target, bloodData, args);
                break;
            case "maxvolume":
                handleSetMaxVolume(sender, target, bloodData, args);
                break;
            case "quality":
                handleSetQuality(sender, target, bloodData, args);
                break;
            default:
                sender.sendMessage(plugin.getMessage("messages.errors.invalid-command"));
        }
    }

    private void handleSetMaxVolume(CommandSender sender, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            double maxVolume = Double.parseDouble(args[3]);
            
            double minRequiredVolume = 500.0; // BLOOD_PER_HEART равен 500.0
            
            if (maxVolume < minRequiredVolume) {
                sender.sendMessage(plugin.getMessage("messages.blood_limits.min_required")
                        .replace("%minvolume%", String.valueOf(minRequiredVolume)));
                return;
            }
            
            bloodData.setMaxVolume(maxVolume);
            
            if (bloodData.getVolume() > maxVolume) {
                bloodData.setVolume(maxVolume);
            }
            
            bloodData.updatePlayerHealth(target);
            plugin.getDataManager().savePlayerData(bloodData);

            sender.sendMessage(plugin.getMessage("messages.blood.set.maxvolume")
                    .replace("%player%", target.getName())
                    .replace("%maxvolume%", String.format("%.1f", maxVolume)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
        }
    }

    private void handleSetType(CommandSender sender, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            PlayerBloodData.BloodType type = PlayerBloodData.BloodType.valueOf(args[3].toUpperCase());
            boolean rhFactor = args.length > 4 ? args[4].equals("+") : true;

            bloodData.setBloodType(type);
            bloodData.setRhFactor(rhFactor);

            plugin.getDataManager().savePlayerData(bloodData);

            sender.sendMessage(plugin.getMessage("messages.blood.set.type")
                    .replace("%player%", target.getName())
                    .replace("%bloodtype%", type.name())
                    .replace("%rhfactor%", rhFactor ? "+" : "-"));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid_type"));
        }
    }

    private void handleSetVolume(CommandSender sender, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            double volume = Double.parseDouble(args[3]);
            bloodData.setVolume(volume);

            bloodData.updatePlayerHealth(target);
            plugin.getDataManager().savePlayerData(bloodData);

            sender.sendMessage(plugin.getMessage("messages.blood.set.volume")
                    .replace("%player%", target.getName())
                    .replace("%volume%", String.format("%.1f", volume)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
        }
    }

    private void handleSetQuality(CommandSender sender, Player target, PlayerBloodData bloodData, String[] args) {
        try {
            double quality = Double.parseDouble(args[3]);
            bloodData.setQuality(quality);

            plugin.getDataManager().savePlayerData(bloodData);

            sender.sendMessage(plugin.getMessage("messages.blood.set.quality")
                    .replace("%player%", target.getName())
                    .replace("%quality%", String.format("%.1f", quality)));
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMessage("messages.errors.invalid_value"));
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
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.set_maxvolume"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.add_volume"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.add_maxvolume"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.remove_volume"));
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.remove_maxvolume"));
        }

        if (player.hasPermission("bloodsystem.reload")) {
            player.sendMessage(plugin.getMessage("messages.help.commands.admin.reload"));
        }
    }

    private void handleInfoCommand(Player player, String[] args) {
        if (args.length > 1) {
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

        sender.sendMessage(prefix + plugin.getMessage("messages.blood.info")
                .replace("%bloodtype%", bloodData.getBloodType().name())
                .replace("%rhfactor%", bloodData.getRhFactor()));

        sender.sendMessage(prefix + plugin.getMessage("messages.blood.volume")
                .replace("%volume%", String.format("%.1f", bloodData.getVolume())) + 
                "§7/§f" + String.format("%.1f", bloodData.getMaxVolume()));

        sender.sendMessage(prefix + plugin.getMessage("messages.blood.quality")
                .replace("%quality%", String.format("%.1f", bloodData.getQuality())));
    }
}