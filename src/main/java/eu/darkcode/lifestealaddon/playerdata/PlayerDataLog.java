package eu.darkcode.lifestealaddon.playerdata;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public final class PlayerDataLog {

    public static PlayerDataLog of(@NotNull String uuid, @NotNull String name, PlayerDataLog.Event eventId, long dateMillis, JsonObject comment) {
        return new PlayerDataLog(uuid.replaceAll("-", ""), name, eventId, dateMillis, comment);
    }

    public static PlayerDataLog of(@NotNull Player player, PlayerDataLog.Event eventId, long dateMillis, JsonObject comment) {
        return new PlayerDataLog(player.getUniqueId().toString().replaceAll("-", ""), player.getName(), eventId, dateMillis, comment);
    }

    public static PlayerDataLog of(@NotNull Player player, PlayerDataLog.Event eventId, JsonObject comment) {
        return of(player, eventId, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog load(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.LOAD_DATA, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog loadFirstJoin(@NotNull Player player) {
        return of(player, Event.LOAD_DATA_FIRST_JOIN, System.currentTimeMillis(), new JsonObject());
    }

    public static PlayerDataLog loadFailed(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.LOAD_DATA_FAILED, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog save(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.SAVE_DATA, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog autoSave(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.AUTO_SAVE_DATA, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog autoSaveFailed(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.AUTO_SAVE_DATA_FAILED, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog saveFailed(@NotNull Player player) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Max-HP", player.getMaxHealth());
        comment.addProperty("HP", player.getHealth());
        return of(player, Event.SAVE_DATA_FAILED, System.currentTimeMillis(), comment);
    }

    public static PlayerDataLog death(@NotNull PlayerDeathEvent event) {
        JsonObject comment = new JsonObject();
        comment.addProperty("Death message", event.getDeathMessage());
        return of(event.getEntity(), Event.PLAYER_DEATH, System.currentTimeMillis(), comment);
    }

    private final String uuid;
    private final String name;
    private final Event event;
    private final long dateMillis;
    private final JsonObject comment;

    private PlayerDataLog(String uuid, String name, Event event, long dateMillis, JsonObject comment) {
        this.uuid = uuid;
        this.name = name;
        this.event = event;
        this.dateMillis = dateMillis;
        this.comment = comment;
    }

    public int getEventId() {
        return event.getId();
    }

    @Getter
    public enum Event {
        LOAD_DATA(0, "LOAD"),
        SAVE_DATA(1, "SAVE"),
        PLAYER_DEATH(2, "DEATH"),
        LOAD_DATA_FAILED(3, "LOAD-FAILED"),
        SAVE_DATA_FAILED(4, "SAVE-FAILED"),
        LOAD_DATA_FIRST_JOIN(5, "LOAD-FIRST-JOIN"),
        AUTO_SAVE_DATA(6, "AUTO-SAVE"),
        AUTO_SAVE_DATA_FAILED(7, "AUTO-SAVE-FAILED");

        private static final Map<Integer, Event> EVENTS = new HashMap<>();

        private final int id;
        private final String name;

        Event(int id, String name) {
            this.id = id;
            this.name = name;
        }

        static{
            for(Event e : Event.values())
                EVENTS.put(e.getId(), e);
        }

        public static Event fromId(int eventId) {
            return EVENTS.get(eventId);
        }
    }
}