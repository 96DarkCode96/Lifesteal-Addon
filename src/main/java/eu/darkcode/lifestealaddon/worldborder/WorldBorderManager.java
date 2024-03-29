package eu.darkcode.lifestealaddon.worldborder;

import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.config.IPluginConfig;
import eu.darkcode.lifestealaddon.config.PluginConfig;
import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public final class WorldBorderManager {

    private final @NotNull Core core;
    private final @NotNull Map<String, WorldBorder> borders = new HashMap<>();
    private final @NotNull IPluginConfig config;

    public WorldBorderManager(@NotNull Core core) {
        this.core = core;

        ConfigurationSerialization.registerClass(WorldBorder.class);
        this.config = PluginConfig.of(core, "borders.yml");

        MethodResult methodResult = loadBorders();
        if (!methodResult.isSuccess()) {
            if (methodResult.hasError()) {
                throw new RuntimeException("Failed to load borders", methodResult.getError());
            } else {
                throw new RuntimeException("Failed to load borders");
            }
        }

        Bukkit.getPluginManager().registerEvents(new WorldBorderListener(this), core);

        PluginCommand border = core.getCommand("border");
        if(border == null) {
            Bukkit.getLogger().warning("Failed to register border command! World border can still be edited in borders.yml");
        }else{
            border.setExecutor(new WorldBorderCommand(this));
            border.setTabCompleter(new WorldBorderCommand(this));
        }
    }

    public MethodResult loadBorders() {
        if (!this.config.loadConfig()) return MethodResult.error(new RuntimeException("Failed to load borders.yml"));

        try {
            assert config.getConfig() != null;
            config.getConfig().addDefault("borders", new ArrayList<>());
            config.getConfig().addDefault("spawn", new Location(Bukkit.getWorld("world"), 0, 0, 0));
            config.getConfig().options().copyDefaults(true).setHeader(List.of("Lifesteal Addon World Borders", "", "For better expiration, use the 'border' command!", "",
                    "Warning: Please avoid manually editing the 'borders.yml' file! If you do something wrong, the borders will be reset! (Or maybe some of them!)", ""));
            config.saveConfig();

            List<?> list = config.getConfig().getList("borders", new ArrayList<>());
            list.stream().map(m -> WorldBorder.deserialize(this, m)).forEach(worldBorder -> {
                if(!addBorder(worldBorder))
                    Bukkit.getLogger().warning("Failed to load border: " + worldBorder.worldName());
            });
        } catch (Throwable e) {
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }

    public boolean addBorder(WorldBorder worldBorder) {
        borders.put(worldBorder.worldName(), worldBorder);
        boolean b = saveBorders();
        if(b) {
            worldBorder.apply();
        }else{
            borders.remove(worldBorder.worldName());
        }
        return b;
    }

    public boolean removeBorder(String worldName) {
        if(!borders.containsKey(worldName)) return false;
        WorldBorder remove = borders.remove(worldName);
        boolean b = saveBorders();
        if(b) {
            World world = Bukkit.getWorld(worldName);
            if(world != null) world.getWorldBorder().reset();
        }else{
            borders.put(worldName, remove);
        }
        return b;
    }

    private boolean saveBorders() {
        assert config.getConfig() != null;
        config.getConfig().set("borders", borders.values().stream().map(WorldBorder::serialize).collect(Collectors.toList()));
        return config.saveConfig();
    }

    public void ifExists(String name, Consumer<WorldBorder> o) {
        if(borders.containsKey(name)) o.accept(borders.get(name));
    }

    /**
     *
     * @param player
     * @param size
     * @return false if player is outside
     */
    public boolean teleportToSafety(Player player, double size){
        Location location = player.getLocation();
        World world = location.getWorld();
        if(world == null) throw new IllegalArgumentException("World is null!");

        return checkOutside(location, player, world.getWorldBorder().getCenter(), size);
    }

    /**
     *
     * @param location
     * @param player
     * @return false if player is outside
     */
    public boolean teleportToSafety(Location location, Player player){
        World world = location.getWorld();
        if(world == null) throw new IllegalArgumentException("World is null!");
        org.bukkit.WorldBorder worldBorder = world.getWorldBorder();

        return checkOutside(location, player, worldBorder.getCenter(), worldBorder.getSize());
    }

    /**
     *
     * @param location
     * @param player
     * @param center
     * @param size
     * @return false if player is outside
     */
    private boolean checkOutside(Location location, Player player, Location center, double size) {
        double size2 = size / 2;
        if(location.getX() > center.getX() + size2 || location.getX() < center.getX() - size2 || location.getZ() > center.getZ() + size2 || location.getZ() < center.getZ() - size2) {
            teleportPlayerToSafety(location, player);
            return false;
        }
        return true;
    }

    public void teleportPlayerToSafety(Location location, Player player) {
        YamlConfiguration yamlConfiguration = getConfig().getConfig();
        assert yamlConfiguration != null;
        World world = location.getWorld();
        assert world != null;
        Location spawn = yamlConfiguration.getLocation("spawn", world.getSpawnLocation());

        Bukkit.getScheduler().runTaskLater(getCore(), () -> player.teleport(spawn), 1);

        MessageUtil.send(player, "&8[&cServer&8] &7You have been teleported to the world spawn!\n&8[&cServer&8] &7Please make sure to stay within the world border!");
    }

}