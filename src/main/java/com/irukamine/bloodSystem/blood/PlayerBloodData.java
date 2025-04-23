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
        // Устанавливаем начальный объем и макс. объем равным максимальному из конфига
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

    // Новый геттер для максимального объема
    public double getMaxVolume() {
        return maxVolume;
    }

    // Новый сеттер для максимального объема
    public void setMaxVolume(double maxVolume) {
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 0.0);
        
        // Минимальный объем крови должен быть достаточным для 1 сердца (500 мл)
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
        // Получаем количество сердец из объема крови
        double hearts = volume / BloodType.BLOOD_PER_HEART;
        // Конвертируем сердца в HP (1 сердце = 2 HP)
        double health = hearts * 2;

        // Устанавливаем максимальное здоровье игрока на основе объема крови
        // Минимум 1 сердце (2 HP), иначе возникнет ошибка
        double maxHealth = Math.min(Math.max(health, 2.0), 20.0);
        player.setMaxHealth(maxHealth);

        // Устанавливаем текущее здоровье равным максимальному
        // Это предотвратит восстановление от сытости выше лимита крови
        player.setHealth(maxHealth);
    }

    public void setVolume(double volume) {
        // Используем минимум из конфига и максимум из персональных данных
        double minVolume = plugin.getConfig().getDouble("settings.blood.min-volume", 0.0);
        this.volume = Math.max(minVolume, Math.min(maxVolume, volume));
    }

    public void addVolume(double amount) {
        // Используем персональный максимальный объем
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
        
        // Минимальный объем крови должен быть достаточным для 1 сердца (500 мл)
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