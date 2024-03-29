package eu.darkcode.lifestealaddon.config;

import eu.darkcode.lifestealaddon.Core;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPluginConfig {

    /**
     * Create config dir if it doesn't exist
     * @return true if config dir was created or already exists
     */
    static boolean initConfigDir(@NotNull Core core) {
        return core.getDataFolder().exists() || core.getDataFolder().mkdirs();
    }

    /**
     * Works also for reloading
     * @return true if config was loaded/reloaded
     */
    boolean loadConfig();

    /**
     * Save config
     * @return true if config was saved
     */
    boolean saveConfig();

    /**
     * Reload config
     * @return config or null if was not loaded
     */
    @Nullable YamlConfiguration getConfig();
}