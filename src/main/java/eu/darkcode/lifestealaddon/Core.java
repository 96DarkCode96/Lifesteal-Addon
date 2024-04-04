package eu.darkcode.lifestealaddon;

import eu.darkcode.lifestealaddon.config.IPluginConfig;
import eu.darkcode.lifestealaddon.playerdata.PlayerDataManager;
import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.worldborder.WorldBorderManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Core extends JavaPlugin {

    private WorldBorderManager worldBorderManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {

        if(!IPluginConfig.initConfigDir(this)) {
            Bukkit.getLogger().severe("Failed to initialize config directory!"); // This should never happen but just in case if it does (permission issues, etc...)
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.worldBorderManager = new WorldBorderManager(this);
        try {
            this.playerDataManager = new PlayerDataManager(this);
        } catch(PlayerDataManager.DatabaseNotEnabledException e) {
            Bukkit.getLogger().severe("Failed to create player data manager! (Database not enabled and setup in config!)");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getLogger().info("Lifesteal Addon has been enabled!");
    }

    @Override
    public void onDisable() {
        if(playerDataManager != null) {
            for(Player player : Bukkit.getOnlinePlayers()) {
                playerDataManager.getListener().stopLoading(player);
                MessageUtil.kick(player, "Data saved correctly!");
            }
            playerDataManager.close();
        } else {
            Bukkit.getLogger().info("Data of players could not be saved! (PlayerDataManager not initialized!)");
        }

        Bukkit.getLogger().info("Lifesteal Addon has been disabled!");
    }
}
