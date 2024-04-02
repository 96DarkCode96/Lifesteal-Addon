package eu.darkcode.lifestealaddon.playerdata.entries;

import eu.darkcode.lifestealaddon.playerdata.PlayerDataManager;
import lombok.Getter;

import java.util.Collection;
import java.util.List;

@Getter
public final class PlayerDataEntryManager {

    private final Collection<PlayerDataEntry> entries;
    private final PlayerDataManager manager;

    public PlayerDataEntryManager(PlayerDataManager manager) {
        this.manager = manager;
        entries = List.of(new MaxHealthDataEntry(), new LocationDataEntry(manager.getCore().getWorldBorderManager()), new ExpDataEntry(), new InventoryDataEntry(),
                new EffectsDataEntry(), new EnderChestDataEntry(), new RespawnLocationDataEntry(), new DiscoveredRecipesDataEntry());
    }

}