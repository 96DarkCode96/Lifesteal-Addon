package eu.darkcode.lifestealaddon.utils;

import com.google.gson.*;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class PotionEffectsUtils {

    private static final Gson GSON = new GsonBuilder().create();

    public static JsonElement serializeEffects(Collection<PotionEffect> activePotionEffects) {
        JsonArray array = new JsonArray();
        for (PotionEffect activePotionEffect : activePotionEffects) {
            array.add(serializeEffect(activePotionEffect));
        }
        return array;
    }

    public static Collection<PotionEffect> deserializeEffects(JsonElement effects) {
        if(effects == null) return new ArrayList<>();
        if(!effects.isJsonArray()) return new ArrayList<>();
        JsonArray array = effects.getAsJsonArray();
        ArrayList<PotionEffect> effectsList = new ArrayList<>();
        for (JsonElement effect : array) {
            effectsList.add(deserializeEffect(effect));
        }
        return effectsList;
    }

    public static JsonElement serializeEffect(PotionEffect effect) {
        if(effect == null) return JsonNull.INSTANCE;
        return GSON.toJsonTree(effect.serialize(), Map.class);
    }

    public static PotionEffect deserializeEffect(JsonElement effect) {
        if(effect == null) return null;
        Map<String, Object> map = (Map<String, Object>) GSON.fromJson(effect, Map.class);
        map.put("duration", ((Number)map.getOrDefault("duration", 0)).intValue());
        map.put("amplifier", ((Number)map.getOrDefault("amplifier", 0)).intValue());
        return new PotionEffect(map);
    }

}