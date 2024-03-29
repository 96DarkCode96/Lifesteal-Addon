package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import eu.darkcode.lifestealaddon.Core;
import eu.darkcode.lifestealaddon.utils.MethodResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlayerDataEntry {

    MethodResult save(@NotNull Core core, @NotNull Player player, @NotNull JsonObject element);

    MethodResult load(@NotNull Core core, @NotNull Player player, @Nullable JsonObject element);

    boolean canLoad(@Nullable JsonObject element);

    @NotNull String getKey();

}