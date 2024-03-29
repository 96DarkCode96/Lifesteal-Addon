package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SaturationDataEntry extends AbstractPlayerDataEntry {
    SaturationDataEntry() {
        super("saturation");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        element.addProperty(getKey(), player.getSaturation());
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            if(element == null) return MethodResult.success();
            else player.setSaturation(element.get(getKey()).getAsInt());
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
