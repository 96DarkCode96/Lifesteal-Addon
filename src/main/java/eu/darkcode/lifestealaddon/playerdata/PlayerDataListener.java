package eu.darkcode.lifestealaddon.playerdata;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.playerdata.entries.PlayerDataEntry;
import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import eu.darkcode.lifestealaddon.utils.SoundUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

@Getter
public final class PlayerDataListener implements Listener {
    private final @NotNull PlayerDataManager playerDataManager;
    private final Map<UUID, BukkitTask> loadingPlayers = new HashMap<>();

    public PlayerDataListener(@NotNull PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // NOTIFY PLAYER OF LOADING
        SoundUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT);

        // CLEAR PLAYER DATA LOADED FROM DEFAULT MC FILE !!!

        //PlayerDataEntryManager.entries.forEach(entry -> entry.pre_load(playerDataManager.getCore(), player));
        // REMOVED CAUSE SERVER IS RUNNING AND PLAYERS ALREADY HAVE SOME ITEMS IN MINECRAFT DATA FILE (THIS WOULD REMOVE ALL ITEMS FROM THEM)

        player.setCanPickupItems(false); // Prevent player from picking up items cause of inventory overriding
        player.addPotionEffect(PotionEffectType.DARKNESS.createEffect(-1, 0));
        player.sendTitle("§7Loading...", "§k§7# §r§8This may take a while §k§7#", 0, 1_728_000, 0);

        // START ASYNC LOADING OF DATA
        loadingPlayers.put(player.getUniqueId(), Bukkit.getScheduler().runTaskAsynchronously(playerDataManager.getCore(), () -> {
            // LOAD PLAYER DATA
            MethodResult playerData1 = playerDataManager.getPlayerData(player.getName(), player.getUniqueId());

            if(!playerData1.isSuccess()) {
                // REMOVE FROM LOADING LIST
                loadingPlayers.remove(player.getUniqueId());

                player.setCanPickupItems(true);

                // KICK PLAYER
                Bukkit.getScheduler().callSyncMethod(playerDataManager.getCore(), () -> {
                    MessageUtil.kick(player, "&8[&cServer&8] &7Failed to load your data!");
                    playerDataManager.logPlayerData(PlayerDataLog.loadFailed(player));
                    return null;
                });

                // LOG FAILURE
                if(playerData1.hasError())
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data! (" + player.getName() + ")", playerData1.getError());
                else
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data! (" + player.getName() + ")");
                return;
            }

            JsonObject playerData = (JsonObject) playerData1.getResult();

            List<PlayerDataEntry> dataEntries = playerDataManager.getEntryManager().getEntries().stream()
                    .filter(entry -> entry.canLoad(playerData))
                    .toList();

            for(PlayerDataEntry entry : dataEntries) {
                MethodResult load = entry.load(playerDataManager.getCore(), player, playerData);
                if(load.isSuccess())
                    continue;

                // REMOVE FROM LOADING LIST
                loadingPlayers.remove(player.getUniqueId());

                player.setCanPickupItems(true);

                // KICK PLAYER
                Bukkit.getScheduler().callSyncMethod(playerDataManager.getCore(), () -> {
                    MessageUtil.kick(player, "&8[&cServer&8] &7Failed to load your data!");
                    playerDataManager.logPlayerData(PlayerDataLog.loadFailed(player));
                    return null;
                });

                // LOG FAILURE
                if(load.hasError())
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data! (" + player.getName() + ") (Entry: " + entry.getClass().getName() + ")", load.getError());
                else
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to load player data! (" + player.getName() + ") (Entry: " + entry.getClass().getName() + ")");
                return;
            }

            // NOTIFY PLAYER OF SUCCESS
            SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_YES);
            MessageUtil.send(player, "&8[&cServer&8] &7Successfully loaded your data!");

            // REMOVE FROM LOADING LIST
            loadingPlayers.remove(player.getUniqueId());

            player.setCanPickupItems(true);

            player.resetTitle();

            Bukkit.getScheduler().callSyncMethod(playerDataManager.getCore(), () -> {
                player.removePotionEffect(PotionEffectType.DARKNESS);
                return null;
            });

            if(playerData == null)
                playerDataManager.logPlayerData(PlayerDataLog.loadFirstJoin(player));
            else
                playerDataManager.logPlayerData(PlayerDataLog.load(player));

            playerDataManager.removeOldLogsByUUID(player.getUniqueId());
        }));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(loadingPlayers.containsKey(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(AsyncPlayerChatEvent event) {
        if(loadingPlayers.containsKey(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(EntityDamageEvent event) {
        if(event.getEntityType() == EntityType.PLAYER && loadingPlayers.containsKey(event.getEntity().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if(loadingPlayers.containsKey(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if(loadingPlayers.containsKey(event.getPlayer().getUniqueId()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopLoading(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerKick(PlayerKickEvent event) {
        stopLoading(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        playerDataManager.logPlayerData(PlayerDataLog.death(event));
    }

    private void stopLoading(Player player) {
        BukkitTask remove = loadingPlayers.remove(player.getUniqueId());
        if(remove != null) {
            remove.cancel();
        } else {
            if (playerDataManager.savePlayerData(player.getName(), player.getUniqueId(), playerDataManager.fetch(player))) {
                playerDataManager.logPlayerData(PlayerDataLog.save(player));
            }else {
                playerDataManager.logPlayerData(PlayerDataLog.saveFailed(player));
            }
        }
    }

}