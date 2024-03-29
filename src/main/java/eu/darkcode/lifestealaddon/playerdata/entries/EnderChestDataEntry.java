package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.ItemUtils;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class EnderChestDataEntry extends AbstractPlayerDataEntry {
    EnderChestDataEntry() {
        super("ender_chest");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        JsonObject obj = new JsonObject();

        Inventory inventory = player.getEnderChest();
        obj.addProperty("contents", ItemUtils.itemStackArrayToBase64(inventory.getStorageContents()));

        element.add(getKey(), obj);
        return MethodResult.success();
    }

    public MethodResult pre_load(@NotNull Core core, @NotNull Player player) {
        player.getEnderChest().clear();
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            if(element == null) return MethodResult.success();
            else {
                JsonObject obj = element.getAsJsonObject(getKey());
                Inventory inventory = player.getEnderChest();
                inventory.setStorageContents(ItemUtils.base64ToItemStackArray(obj.get("contents").getAsString()));
            }
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }

}