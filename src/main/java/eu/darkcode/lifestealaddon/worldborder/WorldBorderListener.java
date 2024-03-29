package eu.darkcode.lifestealaddon.worldborder;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.world.WorldLoadEvent;

public record WorldBorderListener(WorldBorderManager worldBorderManager) implements Listener {

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        worldBorderManager.ifExists(event.getWorld().getName(), (border) -> {
            if (!border.apply())
                Bukkit.getLogger().warning("Failed to apply world border for " + event.getWorld().getName());
        });
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if(!event.getEntityType().equals(EntityType.PLAYER))
            return;
        Player player = (Player) event.getEntity();
        if (!event.getCause().equals(EntityDamageEvent.DamageCause.WORLD_BORDER))
            return;
        worldBorderManager.teleportPlayerToSafety(player.getLocation(), player);
        event.setCancelled(true);
    }

}
