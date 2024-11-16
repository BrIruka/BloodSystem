package com.owlstudio.bloodSystem.placeholders;

import com.owlstudio.bloodSystem.BloodSystem;
import com.owlstudio.bloodSystem.blood.PlayerBloodData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BloodPlaceholders extends PlaceholderExpansion {
    private final BloodSystem plugin;

    public BloodPlaceholders(BloodSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "blood";
    }

    @Override
    public @NotNull String getAuthor() {
        return "OwlStudio";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        PlayerBloodData bloodData = plugin.getPlayerBloodData(player.getUniqueId());
        if (bloodData == null) {
            return "Нет данных";
        }

        switch (params.toLowerCase()) {
            case "type":
                return bloodData.getBloodType().name() + bloodData.getRhFactor();
            case "volume":
                return String.format("%.1f", bloodData.getVolume());
            case "quality":
                return String.format("%.1f", bloodData.getQuality());
            case "type_raw":
                return bloodData.getBloodType().name();
            case "rh":
                return bloodData.getRhFactor();
            case "volume_raw":
                return String.valueOf(bloodData.getVolume());
            case "quality_raw":
                return String.valueOf(bloodData.getQuality());
            default:
                return null;
        }
    }
}