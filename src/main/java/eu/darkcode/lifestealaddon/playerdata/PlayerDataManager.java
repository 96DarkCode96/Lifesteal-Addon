package eu.darkcode.lifestealaddon.playerdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.config.IPluginConfig;
import eu.darkcode.lifestealaddon.config.PluginConfig;
import eu.darkcode.lifestealaddon.playerdata.entries.PlayerDataEntryManager;
import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Getter
public final class PlayerDataManager {

    private static final Gson GSON = new GsonBuilder().create();
    private static final long AUTO_SAVE_DELAY = 20 * 60 * 5; // 5 minutes
    private final @NotNull Core core;
    private final IPluginConfig config;
    private final Connection conn;
    private final PlayerDataEntryManager entryManager;
    private final BukkitTask autoSaveTask;
    private final PlayerDataListener listener;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public PlayerDataManager(@NotNull Core core) throws DatabaseNotEnabledException {
        this.core = core;

        this.entryManager = new PlayerDataEntryManager(this);

        this.config = PluginConfig.of(core, "player_data.yml");
        if(!config.loadConfig()) throw new RuntimeException("Failed to load player_data.yml");

        YamlConfiguration yamlConfiguration = config.getConfig();
        assert yamlConfiguration != null;
        yamlConfiguration.addDefaults(
                Map.of("db_enable", false,
                        "db_jdbc", "jdbc:mysql://localhost:3306/sluxrecruitment?useSSL=false&autoReconnect=true",
                        "db_username", "root",
                        "db_password", "")
        );
        yamlConfiguration.options().copyDefaults(true).setHeader(List.of("Slux-Recruitment PlayerData Configuration", ""));
        yamlConfiguration.setComments("db_jdbc", List.of(
                "JDBC connection string, e.g. jdbc:mysql://localhost:3306/sluxrecruitment?useSSL=false&autoReconnect=true",
                "Warning: To make this work 100% you need to keep parameter 'autoReconnect' set to 'true'!"));
        yamlConfiguration.setComments("db_enable", List.of(
                "Set to 'true' to enable database support and whole player data syncing!"));
        config.saveConfig();

        if(!yamlConfiguration.getBoolean("db_enable"))
            throw new DatabaseNotEnabledException();

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(
                    yamlConfiguration.getString("db_jdbc", "jdbc:mysql://localhost:3306/sluxrecruitment?useSSL=false&autoReconnect=true"),
                    yamlConfiguration.getString("db_username"),
                    yamlConfiguration.getString("db_password"));

            SQLActionBuilder.function(PreparedStatement::execute)
                    .sql("CREATE TABLE IF NOT EXISTS `player_data` (`uuid` UUID NOT NULL, `name` VARCHAR(16) NOT NULL, `data` JSON NOT NULL,  PRIMARY KEY (`uuid`))")
                    .execute(conn);

            SQLActionBuilder.function(PreparedStatement::execute)
                    .sql("CREATE TABLE IF NOT EXISTS `player_data_log` (`uuid` UUID NOT NULL, `name` VARCHAR(16) NOT NULL, `eventId` TINYINT UNSIGNED NOT NULL, `date` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, `comment` JSON NOT NULL)")
                    .execute(conn);

        } catch(SQLNonTransientConnectionException e) {
            throw new RuntimeException("Failed to connect to database!", e);
        } catch(Throwable e) {
            throw new RuntimeException(e);
        }

        listener = new PlayerDataListener(this);
        Bukkit.getPluginManager().registerEvents(listener, core);

        PluginCommand cmd = core.getCommand("playerdatalog");
        if(cmd == null) {
            Bukkit.getLogger().warning("Failed to register playerdatalog command!");
        } else {
            cmd.setExecutor(new PlayerDataLogCommand(this));
            cmd.setTabCompleter(new PlayerDataLogCommand(this));
        }

        autoSaveTask = Bukkit.getScheduler().runTaskTimer(core, () -> {
            try {
                Bukkit.getLogger().info("Auto-saving player data...");
                for(Player player : Bukkit.getOnlinePlayers()) {
                    if(listener.getLoadingPlayers().containsKey(player.getUniqueId())) continue;

                    executorService.execute(() -> {
                        try {
                            if(savePlayerData(player.getName(), player.getUniqueId(), fetch(player))) {
                                logPlayerData(PlayerDataLog.autoSave(player));
                                MessageUtil.send(player, "&8[&cServer&8] &7Your player data has been saved! (Auto-Save)");
                            } else {
                                logPlayerData(PlayerDataLog.autoSaveFailed(player));
                            }
                        } catch (Throwable e) {
                            Bukkit.getLogger().log(Level.SEVERE, "Failed to auto-save player data (" + player.getName() + ")", e);
                        }
                    });
                }
            } catch (Throwable e) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to auto-save player data", e);
            }
        }, AUTO_SAVE_DELAY, AUTO_SAVE_DELAY);

    }

    public void removeOldLogsByUUID(@NotNull UUID uuid) {
        removeOldLogsByUUID(uuid.toString().replaceAll("-", ""));
    }

    public void removeOldLogsByUUID(@NotNull String uuid) {
        try {
            SQLActionBuilder.function(PreparedStatement::execute)
                    .sql("DELETE FROM `player_data_log` WHERE `uuid` = ? AND time_to_sec(timediff(current_timestamp(), date)) >= 604800")
                    .prepare((ps) -> ps.setString(1, uuid))
                    .execute(conn);
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to remove old player's data logs for uuid: " + uuid, e);
        }
    }

    public @Nullable Collection<PlayerDataLog> getLogsByName(@NotNull String name) {
        try {
            return SQLActionBuilder.function((ps) -> {
                        ResultSet resultSet = ps.executeQuery();
                        List<PlayerDataLog> logs = new ArrayList<>();
                        while(resultSet.next()) {
                            logs.add(PlayerDataLog.of(
                                    resultSet.getString("uuid"),
                                    resultSet.getString("name"),
                                    PlayerDataLog.Event.fromId(resultSet.getInt("eventId")),
                                    resultSet.getTimestamp("date").toInstant().toEpochMilli(),
                                    GSON.fromJson(resultSet.getString("comment"), JsonObject.class)
                            ));
                        }
                        return logs;
                    })
                    .sql("SELECT * FROM `player_data_log` WHERE `name` = ?")
                    .prepare((ps) -> ps.setString(1, name))
                    .retry(getConn(), 5);
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to grab player's data logs for name: " + name, e);
            return null;
        }
    }

    public @Nullable Collection<PlayerDataLog> getLogsByUUID(@NotNull String uuid) {
        try {
            return SQLActionBuilder.function((ps) -> {
                        ResultSet resultSet = ps.executeQuery();
                        List<PlayerDataLog> logs = new ArrayList<>();
                        while(resultSet.next()) {
                            logs.add(PlayerDataLog.of(
                                    resultSet.getString("uuid"),
                                    resultSet.getString("name"),
                                    PlayerDataLog.Event.fromId(resultSet.getInt("eventId")),
                                    resultSet.getTimestamp("date").toInstant().toEpochMilli(),
                                    GSON.fromJson(resultSet.getString("comment"), JsonObject.class)
                            ));
                        }
                        return logs;
                    })
                    .sql("SELECT * FROM `player_data_log` WHERE `uuid` = ?")
                    .prepare((ps) -> ps.setString(1, uuid))
                    .retry(getConn(), 5);
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to grab player's data logs for uuid: " + uuid, e);
            return null;
        }
    }

    public void logPlayerData(@NotNull PlayerDataLog log) {
        executorService.execute(() -> {
            try {
                SQLActionBuilder.function(PreparedStatement::executeUpdate)
                        .sql("INSERT INTO `player_data_log` (`uuid`, `name`, `eventId`, `comment`) VALUES (?, ?, ?, ?)")
                        .prepare((ps) -> {
                            ps.setString(1, log.getUuid());
                            ps.setString(2, log.getName());
                            ps.setInt(3, log.getEventId());
                            ps.setString(4, GSON.toJson(log.getComment()));
                        })
                        .retry(getConn(), 5);
            } catch(Throwable e) {
                // MAYBE FOR BETTER ERROR HANDLING ADD FAILED DATA STORAGE (AKA SAVES TO FILE IF FAILED TO DATABASE - BETTER FOR ROLLBACK)
                Bukkit.getLogger().log(Level.SEVERE, "Failed to log player data for " + log.getName() + " (" + log.getUuid() + ")", e);
            }
        });
    }

    public boolean savePlayerData(@NotNull String name, @NotNull UUID uuid, @NotNull JsonElement data) {
        try {
            Bukkit.getLogger().info("Saving player data for " + name + " (" + uuid + ")");
            return SQLActionBuilder.function(PreparedStatement::executeUpdate)
                    .sql("INSERT INTO `player_data` (`uuid`, `name`, `data`) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `data` = VALUES(`data`), `name` = VALUES(`name`)")
                    .prepare((ps) -> {
                        ps.setString(1, uuid.toString().replaceAll("-", ""));
                        ps.setString(2, name);
                        ps.setString(3, GSON.toJson(data));
                    }).retry(getConn(), 5) != 0;
        } catch(Throwable e) {
            // MAYBE FOR BETTER ERROR HANDLING ADD FAILED DATA STORAGE (AKA SAVES TO FILE IF FAILED TO DATABASE - BETTER FOR ROLLBACK)
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save player data for " + name + " (" + uuid + ")", e);
            Bukkit.getLogger().info("DATA: " + GSON.toJson(data));
            return false;
        }
    }

    public @NotNull MethodResult getPlayerData(@NotNull String name, @NotNull UUID uuid) {
        try {
            Bukkit.getLogger().info("Loading player data for " + name + " (" + uuid + ")");
            return SQLActionBuilder.function((ps) -> {
                        ResultSet resultSet = ps.executeQuery();
                        if(!resultSet.next()) return MethodResult.success(null);
                        return MethodResult.success(GSON.fromJson(resultSet.getString("data"), JsonObject.class));
                    })
                    .sql("SELECT `data` FROM `player_data` WHERE `uuid` = ?")
                    .prepare((ps) -> ps.setString(1, uuid.toString().replaceAll("-", "")))
                    .retry(getConn(), 5);
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data for " + name + " (" + uuid + ")", e);
            return MethodResult.error(e);
        }
    }

    public @NotNull JsonElement fetch(Player player) {
        JsonObject data = new JsonObject();
        getEntryManager().getEntries().forEach((entry) -> {
            MethodResult result = entry.save(getCore(), player, data);
            if(!result.isSuccess()) {
                if(result.hasError())
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getName() + " (" + player.getUniqueId() + ")", result.getError());
                else
                    Bukkit.getLogger().log(Level.WARNING, "Failed to save player data for " + player.getName() + " (" + player.getUniqueId() + ")");
            }
        });
        return data;
    }

    public void close() {
        try {
            autoSaveTask.cancel();
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to cancel autosave task", e);
        }
        try {
            executorService.shutdown();
            if(!executorService.awaitTermination(5, TimeUnit.SECONDS)){
                executorService.shutdownNow();
            }
        }catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to shutdown executor", e);
        }
        try {
            getConn().close();
        } catch(Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to close SQL connection", e);
        }
    }

    public static class DatabaseNotEnabledException extends Throwable {
    }
}