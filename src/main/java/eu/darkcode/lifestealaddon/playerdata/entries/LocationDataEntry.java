package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.LocationUtils;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import eu.darkcode.lifestealaddon.worldborder.WorldBorderManager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public final class LocationDataEntry extends AbstractPlayerDataEntry {
    private final WorldBorderManager worldBorderManager;

    LocationDataEntry(@NotNull WorldBorderManager worldBorderManager) {
        super("location");
        this.worldBorderManager = worldBorderManager;
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        element.add(getKey(), LocationUtils.encodeLocation(player.getLocation()));
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            if(element == null)
                worldBorderManager.teleportToSafety(player.getLocation(), player);
            else {
                Location location = LocationUtils.decodeLocation(element.getAsJsonObject(getKey()));

                if(worldBorderManager.teleportToSafety(location, player)){
                    Bukkit.getScheduler().runTaskLater(worldBorderManager.getCore(), () -> player.teleport(location), 1);
                }
            }
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
