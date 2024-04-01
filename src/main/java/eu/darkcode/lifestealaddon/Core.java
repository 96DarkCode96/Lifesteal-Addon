package eu.darkcode.lifestealaddon;

import eu.darkcode.lifestealaddon.config.IPluginConfig;
import eu.darkcode.lifestealaddon.playerdata.PlayerDataLog;
import eu.darkcode.lifestealaddon.playerdata.PlayerDataManager;
import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.worldborder.WorldBorderManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class Core extends JavaPlugin {

    private WorldBorderManager worldBorderManager;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {

        if(!IPluginConfig.initConfigDir(this)){
            Bukkit.getLogger().severe("Failed to initialize config directory!"); // This should never happen but just in case if it does (permission issues, etc...)
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.worldBorderManager = new WorldBorderManager(this);
        try {
            this.playerDataManager = new PlayerDataManager(this);
        } catch (PlayerDataManager.DatabaseNotEnabledException e) {
            Bukkit.getLogger().severe("Failed to create player data manager! (Database not enabled and setup in config!)");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Bukkit.getLogger().info("Lifesteal Addon has been enabled!");
    }

    @Override
    public void onDisable() {
        if(playerDataManager != null){
            Bukkit.getOnlinePlayers().forEach(player -> {
                MessageUtil.kick(player, "&7Restarting...");
                if (playerDataManager.savePlayerData(player.getName(), player.getUniqueId(), playerDataManager.fetch(player))) {
                    playerDataManager.logPlayerData(PlayerDataLog.save(player));
                }else{
                    playerDataManager.logPlayerData(PlayerDataLog.saveFailed(player));
                }
            });
            playerDataManager.close();
        }

        Bukkit.getLogger().info("Lifesteal Addon has been disabled!");
    }
}
