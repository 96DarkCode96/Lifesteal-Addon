package eu.darkcode.lifestealaddon.playerdata.entries;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class AbstractPlayerDataEntry implements PlayerDataEntry {

    private final String key;

    public AbstractPlayerDataEntry(String key) {
        this.key = key;
    }

    @Override
    public boolean canLoad(@Nullable JsonObject element) {
        return element == null || element.has(key);
    }

}
