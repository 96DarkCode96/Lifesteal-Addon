package eu.darkcode.lifestealaddon.utils;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public final class LocationUtils {

    private LocationUtils() {}

    public static JsonObject encodeLocation(@NotNull Location location) {
        JsonObject obj = new JsonObject();

        World world = location.getWorld();
        if(world == null) throw new IllegalArgumentException("World is null!");

        obj.addProperty("world", world.getName());
        obj.addProperty("x", location.getX());
        obj.addProperty("y", location.getY());
        obj.addProperty("z", location.getZ());
        obj.addProperty("yaw", location.getYaw());
        obj.addProperty("pitch", location.getPitch());
        return obj;
    }

    public static Location decodeLocation(JsonObject obj) {
        return new Location(
                Bukkit.getWorld(obj.get("world").getAsString()),
                obj.get("x").getAsDouble(),
                obj.get("y").getAsDouble(),
                obj.get("z").getAsDouble(),
                obj.get("yaw").getAsFloat(),
                obj.get("pitch").getAsFloat());
    }
}