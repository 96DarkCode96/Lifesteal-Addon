package eu.darkcode.lifestealaddon.worldborder;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

public record WorldBorder(@NotNull WorldBorderManager manager, @NotNull String worldName, double size) implements ConfigurationSerializable {

    public WorldBorder {
        Objects.requireNonNull(worldName, "worldName cannot be null!");
        Objects.requireNonNull(manager, "manager cannot be null!");
        if (size < 1) size = 1; // throw new IllegalArgumentException("Border size cannot be less than 1!");
    }

    public static WorldBorder deserialize(@NotNull WorldBorderManager manager, Object map) {
        try {
            if (map instanceof Map<?, ?> mapData) {
                String worldName = (String) mapData.get("worldName");
                double size = ((Number) mapData.get("size")).doubleValue();
                return new WorldBorder(manager, worldName, size);
            }
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to deserialize WorldBorder!", e);
        }
        throw new IllegalArgumentException("Failed to deserialize WorldBorder!");
    }

    public boolean apply() {
        try {
            World world = Bukkit.getWorld(worldName);
            if (world == null) return false;
            world.getWorldBorder().reset();
            world.getPlayers().forEach(player -> manager.teleportToSafety(player, size));
            world.getWorldBorder().setSize(size);
        } catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to apply world border! (" + worldName + ")", e);
            return false;
        }
        return true;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("worldName", worldName);
        map.put("size", size);
        return map;
    }
}