package eu.darkcode.lifestealaddon.config;

import eu.darkcode.lifestealaddon.Core;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public abstract class APluginConfig implements IPluginConfig {

    private final @NotNull File configFile;
    private @Nullable YamlConfiguration config;

    public APluginConfig(@NotNull Core core, @NotNull String configName) {
        this.configFile = new File(core.getDataFolder(), configName);
    }

    @Override
    public boolean loadConfig() {
        if (!configFile.exists()) {
            try {
                if(!configFile.createNewFile()) {
                    Bukkit.getLogger().severe("Failed to create config file! (" + configFile.getAbsolutePath() + ")");
                    return false;
                }
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to create config file! (" + configFile.getAbsolutePath() + ")", e);
                return false;
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        return true;
    }

    @Override
    public boolean saveConfig() {
        try {
            if (config == null) return false;
            config.save(configFile);
            return true;
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save config file! (" + configFile.getAbsolutePath() + ")", e);
            return false;
        }
    }

    @Override
    public @Nullable YamlConfiguration getConfig() {
        return config;
    }
}