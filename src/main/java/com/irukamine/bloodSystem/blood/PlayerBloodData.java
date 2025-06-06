package com.irukamine.bloodSystem.blood;

import com.irukamine.bloodSystem.BloodSystem;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class PlayerBloodData {
    private final UUID playerUUID;
    private BloodType bloodType;
    private boolean rhFactor;
    private double quality;
    private double volume;
    private double maxVolume;
    private final BloodSystem plugin;

    public enum BloodType {
        A("A"), B("B"), AB("AB"), O("O");

        private final String display;

        // Константа для конвертации крови в HP
        private static final double BLOOD_PER_HEART = 500.0; // 500 мл на одно сердце

        BloodType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }

        public static BloodType random() {
            return values()[new Random().nextInt(values().length)];
        }

        public static BloodType fromString(String type) {
            try {
                return valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return O; // Группа O по умолчанию
            }
        }
    }

    public PlayerBloodData(UUID playerUUID, boolean randomize, BloodSystem plugin) {
        this.playerUUID = playerUUID;
        this.plugin = plugin;
        if (randomize) {
            randomizeBloodData();
        }
    }

    // Метод для создания случайных данных
    private void randomizeBloodData() {
        this.bloodType = BloodType.random();
        this.rhFactor = new Random().nextBoolean();
        this.quality = 100.0;
        double configMaxVolume = plugin.getConfig().getDouble("settings.blood.max-volume", 5000.0);
        this.volume = configMaxVolume;
        this.maxVolume = configMaxVolume;
    }

    // Геттеры
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public boolean isRhPositive() {
        return rhFactor;
    }

    public String getRhFactor() {
        return rhFactor ? "+" : "-";
    }

    public double getQuality() {
        return quality;
    }

    public double getVolume() {
        return volume;
    }

    public double getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(double maxVolume) {
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 0.0);
        
        double minRequiredVolume = BloodType.BLOOD_PER_HEART;
        minVolume = Math.max(minVolume, minRequiredVolume);
        
        this.maxVolume = Math.max(minVolume, maxVolume);
    }

    // Сеттеры
    public void setBloodType(BloodType bloodType) {
        this.bloodType = bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = BloodType.fromString(bloodType);
    }

    public void setRhFactor(boolean rhFactor) {
        this.rhFactor = rhFactor;
    }

    public void setQuality(double quality) {
        this.quality = Math.max(0, Math.min(100, quality));
    }

    // Метод для обновления здоровья игрока на основе объема крови
    public void updatePlayerHealth(Player player) {
        if (player == null || !player.isOnline() || player.isDead()) {
            return;
        }

        try {
            double hearts = volume / BloodType.BLOOD_PER_HEART;
            
            double maxHealth = hearts * 2;
            
            maxHealth = Math.max(maxHealth, 2.0);
            
            player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            
            double currentHealthPercent = player.getHealth() / player.getMaxHealth();
            
            if (currentHealthPercent < 0.1 || player.getHealth() <= 0) {
                player.setHealth(maxHealth);
            } else {
                double newHealth = maxHealth * currentHealthPercent;
                player.setHealth(Math.min(maxHealth, newHealth));
            }
        } catch (Exception e) {
        }
    }

    public void setVolume(double volume) {
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 0.0);
        this.volume = Math.max(minVolume, Math.min(maxVolume, volume));
    }

    public void addVolume(double amount) {
        double newVolume = Math.min(this.volume + amount, maxVolume);
        setVolume(newVolume);
    }

    public void removeVolume(double amount) {
        setVolume(volume - amount);
    }

    // Метод для увеличения максимального объема крови
    public void addMaxVolume(double amount) {
        setMaxVolume(maxVolume + amount);
    }

    // Метод для уменьшения максимального объема крови
    public void removeMaxVolume(double amount) {
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 0.0);
        
        double minRequiredVolume = BloodType.BLOOD_PER_HEART;
        minVolume = Math.max(minVolume, minRequiredVolume);
        
        setMaxVolume(Math.max(maxVolume - amount, minVolume));
    }

    public void addQuality(double amount) {
        setQuality(quality + amount);
    }

    public void removeQuality(double amount) {
        setQuality(quality - amount);
    }

    // Метод для получения информации о крови
    public String getBloodInfo() {
        return String.format("%s%s (Качество: %.1f%%, Объём: %.0f/%.0f мл)",
                bloodType.getDisplay(),
                getRhFactor(),
                quality,
                volume,
                maxVolume);
    }
}