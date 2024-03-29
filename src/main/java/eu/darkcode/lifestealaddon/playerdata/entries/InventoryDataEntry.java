package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.ItemUtils;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class InventoryDataEntry extends AbstractPlayerDataEntry {
    InventoryDataEntry() {
        super("inventory");
    }

    @Override
    public MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element) {
        JsonObject obj = new JsonObject();

        PlayerInventory inventory = player.getInventory();
        obj.addProperty("contents", ItemUtils.itemStackArrayToBase64(inventory.getStorageContents()));
        obj.addProperty("armor", ItemUtils.itemStackArrayToBase64(inventory.getArmorContents()));
        obj.addProperty("offHand", ItemUtils.itemStackToBase64(inventory.getItemInOffHand()));
        obj.addProperty("slot", inventory.getHeldItemSlot());

        element.add(getKey(), obj);
        return MethodResult.success();
    }

    @Override
    public MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element) {
        try{
            if(element == null) return MethodResult.success();
            else {
                JsonObject obj = element.getAsJsonObject(getKey());
                PlayerInventory inventory = player.getInventory();
                inventory.setStorageContents(ItemUtils.base64ToItemStackArray(obj.get("contents").getAsString()));
                inventory.setArmorContents(ItemUtils.base64ToItemStackArray(obj.get("armor").getAsString()));
                inventory.setItemInOffHand(ItemUtils.base64ToItemStack(obj.get("offHand").getAsString()));
                inventory.setHeldItemSlot(obj.get("slot").getAsInt());
            }
        }catch (Throwable e){
            return MethodResult.error(e);
        }
        return MethodResult.success();
    }

}