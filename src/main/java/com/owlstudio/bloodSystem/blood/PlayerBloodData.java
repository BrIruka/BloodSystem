package com.owlstudio.bloodSystem.blood;

import com.owlstudio.bloodSystem.BloodSystem;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class PlayerBloodData {
    private final UUID playerUUID;
    private BloodType bloodType;
    private boolean rhFactor;
    private double quality;
    private double volume;
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
        // Устанавливаем начальный объем равным максимальному из конфига
        this.volume = plugin.getConfig().getDouble("settings.blood.max-volume", 5000.0);
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
        // Получаем количество сердец из объема крови
        double hearts = volume / BloodType.BLOOD_PER_HEART;
        // Конвертируем сердца в HP (1 сердце = 2 HP)
        double health = hearts * 2;

        // Устанавливаем максимальное здоровье игрока на основе объема крови
        double maxHealth = Math.min(Math.max(health, 0), 20.0);
        player.setMaxHealth(maxHealth);

        // Устанавливаем текущее здоровье равным максимальному
        // Это предотвратит восстановление от сытости выше лимита крови
        player.setHealth(maxHealth);
    }

    public void setVolume(double volume) {
        // Берем мин и макс значения из конфига
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 2000.0);
        double maxVolume = plugin.getConfig().getDouble("settings.blood.max-volume", 5000.0);
        this.volume = Math.max(minVolume, Math.min(maxVolume, volume));
    }

    public void addVolume(double amount) {
        double maxVolume = plugin.getConfig().getDouble("settings.blood.max-volume", 5000.0);
        double newVolume = Math.min(this.volume + amount, maxVolume);
        setVolume(newVolume);
    }

    public void removeVolume(double amount) {
        setVolume(volume - amount);
    }

    public void addQuality(double amount) {
        setQuality(quality + amount);
    }

    public void removeQuality(double amount) {
        setQuality(quality - amount);
    }

    // Метод для получения информации о крови
    public String getBloodInfo() {
        return String.format("%s%s (Качество: %.1f%%, Объём: %.0f мл)",
                bloodType.getDisplay(),
                getRhFactor(),
                quality,
                volume);
    }

    // геттер для максимального объема крови
    public double getMaxVolume() {
        return this.volume;
    }
}