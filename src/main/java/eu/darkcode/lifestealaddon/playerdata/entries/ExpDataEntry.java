package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ExpDataEntry extends AbstractPlayerDataEntry {
    ExpDataEntry() {
        super("exp");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        JsonObject obj = new JsonObject();
        obj.addProperty("exp", player.getExp());
        obj.addProperty("level", player.getLevel());
        element.add(getKey(), obj);
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            if(element == null) return MethodResult.success();
            else {
                JsonObject json = element.get(getKey()).getAsJsonObject();
                player.setExp(json.get("exp").getAsFloat());
                player.setLevel(json.get("level").getAsInt());
            }
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
