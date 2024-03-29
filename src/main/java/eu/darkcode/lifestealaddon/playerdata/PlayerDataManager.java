package eu.darkcode.lifestealaddon.playerdata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.config.IPluginConfig;
import eu.darkcode.lifestealaddon.config.PluginConfig;
import eu.darkcode.lifestealaddon.playerdata.entries.PlayerDataEntryManager;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@Getter
public final class PlayerDataManager {

    private static final Gson GSON = new GsonBuilder().create();
    private final @NotNull Core core;
    private final IPluginConfig config;
    private final Connection conn;
    private final PlayerDataEntryManager entryManager;
    public PlayerDataManager(@NotNull Core core) throws DatabaseNotEnabledException{
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
        } catch(SQLNonTransientConnectionException e) {
            throw new RuntimeException("Failed to connect to database!", e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        Bukkit.getPluginManager().registerEvents(new PlayerDataListener(this), core);
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
                    })
                    .retry(getConn(), 5) == 1;
        } catch (Throwable e) {
            // MAYBE FOR BETTER ERROR HANDLING ADD FAILED DATA STORAGE (AKA SAVES TO FILE IF FAILED TO DATABASE - BETTER FOR ROLLBACK)
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save player data for " + name + " (" + uuid + ")", e);
            return false;
        }
    }

    public @Nullable JsonObject getPlayerData(@NotNull String name, @NotNull UUID uuid) {
        try {
            Bukkit.getLogger().info("Loading player data for " + name + " (" + uuid + ")");
            return SQLActionBuilder.function((ps) -> {
                        ResultSet resultSet = ps.executeQuery();
                        if(!resultSet.next()) return null;
                        return GSON.fromJson(resultSet.getString("data"), JsonObject.class);
                    })
                    .sql("SELECT `data` FROM `player_data` WHERE `uuid` = ?")
                    .prepare((ps) -> ps.setString(1, uuid.toString().replaceAll("-", "")))
                    .retry(getConn(), 5);
        } catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data for " + name + " (" + uuid + ")", e);
            return null;
        }
    }

    public @NotNull JsonElement fetch(Player player) {
        JsonObject data = new JsonObject();
        getEntryManager().getEntries().forEach((entry) -> {
            MethodResult result = entry.save(getCore(), player, data);
            if (!result.isSuccess()) {
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
            getConn().close();
        } catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to close SQL connection", e);
        }
    }

    public static class DatabaseNotEnabledException extends Throwable {}
}