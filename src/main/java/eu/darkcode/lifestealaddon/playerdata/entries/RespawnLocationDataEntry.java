package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.LocationUtils;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RespawnLocationDataEntry extends AbstractPlayerDataEntry {
    RespawnLocationDataEntry() {
        super("bed_location");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        Location location = player.getRespawnLocation();
        if (location == null)
            return MethodResult.success();
        element.add(getKey(), LocationUtils.encodeLocation(location));
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            Bukkit.getScheduler().callSyncMethod(core, () -> {
                if (element == null) return MethodResult.success();
                else {
                    JsonObject obj = element.getAsJsonObject(getKey());
                    Location location = new Location(
                            Bukkit.getWorld(obj.get("world").getAsString()),
                            obj.get("x").getAsDouble(),
                            obj.get("y").getAsDouble(),
                            obj.get("z").getAsDouble(),
                            obj.get("yaw").getAsFloat(),
                            obj.get("pitch").getAsFloat());
                    player.setRespawnLocation(location);
                }
                return null;
            });
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
