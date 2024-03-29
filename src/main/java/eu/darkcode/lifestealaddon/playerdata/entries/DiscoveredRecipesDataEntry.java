package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DiscoveredRecipesDataEntry extends AbstractPlayerDataEntry {
    DiscoveredRecipesDataEntry() {
        super("discovered_recipes");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        JsonArray obj = new JsonArray();
        for (NamespacedKey discoveredRecipe : player.getDiscoveredRecipes())
            obj.add(discoveredRecipe.toString());
        element.add(getKey(), obj);
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            Bukkit.getScheduler().callSyncMethod(core, () -> {
                if(element == null) return MethodResult.success();
                else {
                    JsonArray array = element.getAsJsonArray(getKey());
                    for (JsonElement jsonElement : array) {
                        NamespacedKey recipe = NamespacedKey.fromString(jsonElement.getAsString());
                        if(recipe == null) continue; // MAYBE ERROR HANDLING ?
                        player.discoverRecipe(recipe);
                    }
                }
                return null;
            });
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }
}
