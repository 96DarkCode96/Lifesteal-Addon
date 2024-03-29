package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class MaxHealthDataEntry extends AbstractPlayerDataEntry {
    MaxHealthDataEntry() {
        super("max_health");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if(attribute == null) element.addProperty(getKey(), 20.0D);
        else element.addProperty(getKey(), attribute.getBaseValue());
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if(attribute == null) throw new NullPointerException("Attribute of player (GENERIC_MAX_HEALTH) is null");
            else{
                if(element == null) return MethodResult.success();
                else attribute.setBaseValue(element.get(getKey()).getAsDouble());
            }
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
