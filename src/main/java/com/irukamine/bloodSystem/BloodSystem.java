package com.irukamine.bloodSystem;

import com.irukamine.bloodSystem.blood.PlayerBloodData;
import com.irukamine.bloodSystem.commands.BloodCommand;
import com.irukamine.bloodSystem.commands.BloodTabCompleter;
import com.irukamine.bloodSystem.data.DataManager;
import com.irukamine.bloodSystem.placeholders.BloodPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.EventPriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodSystem extends JavaPlugin implements Listener {
    private FileConfiguration langConfig;
    private final Map<UUID, PlayerBloodData> playerBloodMap = new HashMap<>();
    private DataManager dataManager;
    private int regenerationTask = 0;
    private int healthRegenerationTask = 0;

    @Override
    public void onEnable() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        // Сохраняем конфиг по умолчанию
        saveDefaultConfig();

        // Загружаем языковой файл
        loadLanguage();

        // Инициализируем менеджер данных
        dataManager = new DataManager(this);

        // Регистрируем команды и автодополнение
        getCommand("blood").setExecutor(new BloodCommand(this));
        getCommand("blood").setTabCompleter(new BloodTabCompleter());

        // Запускаем обе задачи регенерации
        startRegenerationTask();
        startHealthRegenerationTask();

        // Регистрируем слушатели событий
        getServer().getPluginManager().registerEvents(this, this);

        // Регистрируем плейсхолдеры PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BloodPlaceholders(this).register();
            getLogger().info("PlaceholderAPI найден - плейсхолдеры зарегистрированы!");
        }

        // Красивый вывод в консоль при запуске
        console.sendMessage("");
        console.sendMessage("§8[§cBloodSystem§8] §7==========================================");
        console.sendMessage("§8[§cBloodSystem§8] §fVersion: §c" + getDescription().getVersion());
        console.sendMessage("§8[§cBloodSystem§8] §fAuthor: §2IrukaMine");
        console.sendMessage("§8[§cBloodSystem§8] §7==========================================");
        console.sendMessage("");
    }

    @Override
    public void onDisable() {
        // Сохраняем данные всех игроков перед выключением
        for (PlayerBloodData bloodData : playerBloodMap.values()) {
            dataManager.savePlayerData(bloodData);
        }
        playerBloodMap.clear();

        getLogger().info("BloodSystem выключен!");

        // Отменяем задачи при выключении плагина
        if (regenerationTask != 0) {
            Bukkit.getScheduler().cancelTask(regenerationTask);
        }
        if (healthRegenerationTask != 0) {
            Bukkit.getScheduler().cancelTask(healthRegenerationTask);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());

        double bloodLoss = event.getFinalDamage() * getConfig().getDouble("settings.blood.blood-loss-per-damage", 100);
        
        double currentHealth = player.getHealth();
        double finalDamage = event.getFinalDamage();
        
        if (currentHealth - finalDamage <= 0) {
            return;
        } else {
            bloodData.removeVolume(bloodLoss);
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && !player.isDead()) {
                    bloodData.updatePlayerHealth(player);
                }
            }, 1L);
        }
    }

    public void setBloodVolume(Player target, double volume) {
        PlayerBloodData bloodData = getPlayerBloodData(target.getUniqueId());
        bloodData.setVolume(volume);
        bloodData.updatePlayerHealth(target);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playerBloodMap.containsKey(uuid)) {
            PlayerBloodData bloodData = dataManager.loadPlayerData(uuid);

            if (bloodData == null) {
                bloodData = new PlayerBloodData(uuid, true, this);
                dataManager.savePlayerData(bloodData);
            }

            playerBloodMap.put(uuid, bloodData);
        }

        PlayerBloodData bloodData = playerBloodMap.get(uuid);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            bloodData.updatePlayerHealth(player);
        }, 1L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        PlayerBloodData bloodData = playerBloodMap.get(playerUUID);
        if (bloodData != null) {
            dataManager.savePlayerData(bloodData);
            playerBloodMap.remove(playerUUID);
        }
    }

    public void reloadPlugin() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        for (PlayerBloodData bloodData : playerBloodMap.values()) {
            dataManager.savePlayerData(bloodData);
        }
        playerBloodMap.clear();
        reloadConfig();
        loadLanguage();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerBloodData bloodData = dataManager.loadPlayerData(uuid);
            if (bloodData == null) {
                bloodData = new PlayerBloodData(uuid, true, this);
            }
            playerBloodMap.put(uuid, bloodData);
            bloodData.updatePlayerHealth(player);
        }
        startRegenerationTask();
        startHealthRegenerationTask();
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new BloodPlaceholders(this).register();
        }

    }

    private void loadLanguage() {
        // Создаем папку lang, если её нет
        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();

            // Сохраняем все языковые файлы только если папка была создана
            String[] languages = {"en", "ru", "uk", "pl", "zh", "fr", "es"};
            for (String lang : languages) {
                saveResource("lang/" + lang + ".yml", false);
            }
        }

        // Получаем язык из конфига
        String language = getConfig().getString("settings.language", "en");

        // Загружаем файл языка
        File langFile = new File(getDataFolder() + "/lang/" + language + ".yml");
        if (!langFile.exists()) {
            langFile = new File(getDataFolder() + "/lang/en.yml");

            // Если нет даже английского файла, создаем его
            if (!langFile.exists()) {
                saveResource("lang/en.yml", false);
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    // Метод для получения сообщений из языкового файла
    public String getMessage(String path) {
        String message = langConfig.getString(path);
        if (message == null) {
            return "Message not found: " + path;
        }
        return message.replace('&', '§');
    }

    // Геттер для получения данных о крови игрока
    public PlayerBloodData getPlayerBloodData(UUID playerUUID) {
        return playerBloodMap.get(playerUUID);
    }

    // Геттер для DataManager
    public DataManager getDataManager() {
        return dataManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
        
        if (bloodData != null) {
            double respawnBloodVolume = getConfig().getDouble("settings.blood.death-blood-loss", 500.0);
            bloodData.setVolume(respawnBloodVolume);
            
            dataManager.savePlayerData(bloodData);
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        Bukkit.getScheduler().runTaskLater(this, () -> {
            PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
            if (bloodData != null && player.isOnline()) {
                bloodData.updatePlayerHealth(player);
                
                dataManager.savePlayerData(bloodData);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        
        Player player = (Player) event.getEntity();
        PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
        
        if (bloodData == null) return;
        
        // Получаем максимально возможное здоровье на основе объема крови
        double maxAllowedHealth = Math.min((bloodData.getVolume() / 500.0) * 2.0, 20.0);
        
        // Если игрок пытается восстановить больше максимально допустимого
        if (player.getHealth() + event.getAmount() > maxAllowedHealth) {
            double allowedAmount = maxAllowedHealth - player.getHealth();
            if (allowedAmount <= 0) {
                event.setCancelled(true);
            } else {
                event.setAmount(allowedAmount);
            }
        }
    }

    private void startRegenerationTask() {
        if (regenerationTask != 0) {
            Bukkit.getScheduler().cancelTask(regenerationTask);
        }

        double regenPerMinute = getConfig().getDouble("settings.blood.regeneration-rate", 300);
        double regenPerSecond = regenPerMinute / 60.0;

        regenerationTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
                if (bloodData != null && player.isOnline() && !player.isDead()) {
                    bloodData.addVolume(regenPerSecond);
                    bloodData.updatePlayerHealth(player);
                }
            }
        }, 20L, 20L); //20 тиков = 1 секунда
    }

    private void startHealthRegenerationTask() {
        if (healthRegenerationTask != 0) {
            Bukkit.getScheduler().cancelTask(healthRegenerationTask);
        }

        healthRegenerationTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
                if (bloodData == null || player.isDead()) continue;

                if (player.getHealth() < player.getMaxHealth()) {
                    double currentHealth = player.getHealth();
                    double maxHealth = player.getMaxHealth();
                    
                    double healAmount = 1.0;
                    
                    if (currentHealth + healAmount <= maxHealth) {
                        player.setHealth(currentHealth + healAmount);
                    }
                }
            }
        }, 20L, 20L).getTaskId(); // 20 тиков = 1 секунда
    }
}