package com.irukamine.bloodSystem.data;

import com.irukamine.bloodSystem.BloodSystem;
import com.irukamine.bloodSystem.blood.PlayerBloodData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {
    private final BloodSystem plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    public DataManager(BloodSystem plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    public void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("playerdata.yml", false);
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить данные игроков!");
            e.printStackTrace();
        }
    }

    public boolean hasPlayerData(UUID playerUUID) {
        return dataConfig.contains("players." + playerUUID.toString());
    }

    public void savePlayerData(PlayerBloodData bloodData) {
        String uuid = bloodData.getPlayerUUID().toString();
        String path = "players." + uuid + ".";

        dataConfig.set(path + "bloodType", bloodData.getBloodType().name());
        dataConfig.set(path + "rhFactor", bloodData.isRhPositive());
        dataConfig.set(path + "quality", bloodData.getQuality());
        dataConfig.set(path + "volume", bloodData.getVolume());
        dataConfig.set(path + "maxVolume", bloodData.getMaxVolume());

        saveData();
    }

    public PlayerBloodData loadPlayerData(UUID playerUUID) {
        String path = "players." + playerUUID.toString() + ".";

        if (!hasPlayerData(playerUUID)) {
            return null;
        }

        PlayerBloodData bloodData = new PlayerBloodData(playerUUID, false, plugin);

        bloodData.setBloodType(dataConfig.getString(path + "bloodType"));
        bloodData.setRhFactor(dataConfig.getBoolean(path + "rhFactor"));
        bloodData.setQuality(dataConfig.getDouble(path + "quality"));
        
        if (dataConfig.contains(path + "maxVolume")) {
            bloodData.setMaxVolume(dataConfig.getDouble(path + "maxVolume"));
        } else {
            bloodData.setMaxVolume(plugin.getConfig().getDouble("settings.blood.max-volume", 5000.0));
        }
        
        bloodData.setVolume(dataConfig.getDouble(path + "volume"));

        return bloodData;
    }
}