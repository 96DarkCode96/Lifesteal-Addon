package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AirLevelDataEntry extends AbstractPlayerDataEntry {
    AirLevelDataEntry() {
        super("air_level");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        JsonObject data = new JsonObject();
        data.addProperty("remaining", player.getRemainingAir());
        data.addProperty("max", player.getMaximumAir());
        element.add(getKey(), data);
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            Bukkit.getScheduler().callSyncMethod(core, () -> {
                if(element == null) return MethodResult.success();
                else{
                    JsonObject data = element.getAsJsonObject(getKey());
                    player.setMaximumAir(data.get("max").getAsInt());
                    player.setRemainingAir(data.get("remaining").getAsInt());
                    return null;
                }
            });
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
