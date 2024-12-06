package com.owlstudio.bloodSystem;

import com.owlstudio.bloodSystem.blood.PlayerBloodData;
import com.owlstudio.bloodSystem.commands.BloodCommand;
import com.owlstudio.bloodSystem.commands.BloodTabCompleter;
import com.owlstudio.bloodSystem.data.DataManager;
import com.owlstudio.bloodSystem.placeholders.BloodPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodSystem extends JavaPlugin implements Listener {
    private FileConfiguration langConfig;
    private final Map<UUID, PlayerBloodData> playerBloodMap = new HashMap<>();
    private DataManager dataManager;
    private int regenerationTask;

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

        // Запускаем задачу регенерации крови
        startRegenerationTask();

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
        console.sendMessage("§8[§cBloodSystem§8] §fAuthor: §6IrukaMine");
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

        // Останавливаем задачу регенерации
        Bukkit.getScheduler().cancelTask(regenerationTask);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());

        // Вычисляем потерю крови (100 мл за 1 урон)
        double bloodLoss = event.getFinalDamage() * getConfig().getDouble("settings.blood.blood-loss-per-damage", 100);
        bloodData.removeVolume(bloodLoss);

        // Обновляем здоровье
        bloodData.updatePlayerHealth(player);
    }

    // Обновим метод в BloodCommand для команды set volume
    public void setBloodVolume(Player target, double volume) {
        PlayerBloodData bloodData = getPlayerBloodData(target.getUniqueId());
        bloodData.setVolume(volume);
        bloodData.updatePlayerHealth(target);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Проверяем, есть ли уже данные об игроке
        if (!playerBloodMap.containsKey(uuid)) {
            PlayerBloodData bloodData = dataManager.loadPlayerData(uuid);

            // Если данных нет в файле, создаем новые
            if (bloodData == null) {
                bloodData = new PlayerBloodData(uuid, true, this);
                dataManager.savePlayerData(bloodData);
            }

            playerBloodMap.put(uuid, bloodData);
        }

        // Обновляем здоровье
        PlayerBloodData bloodData = playerBloodMap.get(uuid);
        bloodData.updatePlayerHealth(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        // Сохраняем данные при выходе игрока
        PlayerBloodData bloodData = playerBloodMap.get(playerUUID);
        if (bloodData != null) {
            dataManager.savePlayerData(bloodData);
            playerBloodMap.remove(playerUUID);
        }
    }

    public void reloadPlugin() {
        ConsoleCommandSender console = Bukkit.getConsoleSender();

        // Сохраняем все данные
        for (PlayerBloodData bloodData : playerBloodMap.values()) {
            dataManager.savePlayerData(bloodData);
        }
        // Очищаем карту данных
        playerBloodMap.clear();
        // Перезагружаем конфиг
        reloadConfig();
        // Перезагружаем языковые файлы
        loadLanguage();
        // Перезагружаем данные всех онлайн игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerBloodData bloodData = dataManager.loadPlayerData(uuid);
            if (bloodData == null) {
                bloodData = new PlayerBloodData(uuid, true, this);
            }
            playerBloodMap.put(uuid, bloodData);
            bloodData.updatePlayerHealth(player);
        }
        // Перезапускаем задачу регенерации
        startRegenerationTask();
        // Перерегистрируем плейсхолдеры
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
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
            // Отменяем восстановление от сытости
            event.setCancelled(true);
        }
    }

    private void startRegenerationTask() {
        // Останавливаем существующую задачу, если она есть
        if (regenerationTask != 0) {
            Bukkit.getScheduler().cancelTask(regenerationTask);
        }

        // Получаем значение регенерации из конфига (мл/секунду)
        double regenPerMinute = getConfig().getDouble("settings.blood.regeneration-rate", 300);
        // Конвертируем в регенерацию в секунду
        double regenPerSecond = regenPerMinute / 60.0;

        regenerationTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerBloodData bloodData = getPlayerBloodData(player.getUniqueId());
                if (bloodData != null) {
                    bloodData.addVolume(regenPerSecond);
                    bloodData.updatePlayerHealth(player);
                }
            }
        }, 20L, 20L); //20 тиков = 1 секунда
    }
}
