package eu.darkcode.lifestealaddon.playerdata;

import eu.darkcode.lifestealaddon.utils.MessageUtil;
import eu.darkcode.lifestealaddon.utils.SoundUtil;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public final class PlayerDataLogCommand implements TabExecutor {

    private final PlayerDataManager playerDataManager;
    private final DateTimeFormatter TIME_FORMATTER;

    public PlayerDataLogCommand(PlayerDataManager playerDataManager){
        this.playerDataManager = playerDataManager;
        this.TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("slux.developer")) {
            MessageUtil.send(sender, "&cYou are not allowed to do this!");
            if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO);
            return true;
        }
        if(args.length == 2) {
            Collection<PlayerDataLog> logs;
            if(args[0].equalsIgnoreCase("name")){
                logs = playerDataManager.getLogsByName(args[1]);
                MessageUtil.send(sender, "&8[&cData Log&8] &7Player data logs (Name: " + args[1] + ")");
            } else if (args[0].equalsIgnoreCase("uuid")) {
                logs = playerDataManager.getLogsByUUID(args[1].replaceAll("-", ""));
                MessageUtil.send(sender, "&8[&cData Log&8] &7Player data logs (UUID: " + args[1] + ")");
            }else{
                if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO);
                MessageUtil.send(sender, "&8[&cData Log&8] &cInvalid usage! &8(&c/playerdatalog uuid <uuid> &8| &c/border name <name> &8)");
                return true;
            }
            if(logs == null){
                if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO);
                MessageUtil.send(sender, "&8[&cData Log&8] &8» &cFailed to load logs!");
                return true;
            }
            if(logs.isEmpty()){
                if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_HURT);
                MessageUtil.send(sender, "&8[&cData Log&8] &8» &cNo logs found!");
                return true;
            }
            List<String> messages = new ArrayList<>();
            DateTimeFormatter timeFormatter;
            if(sender instanceof Player player) {
                String locale = player.getLocale();
                String[] s = locale.split("_");
                if(s.length == 2) timeFormatter = TIME_FORMATTER.withLocale(Locale.of(s[0], s[1]));
                else timeFormatter = TIME_FORMATTER;
            } else timeFormatter = TIME_FORMATTER;
            logs.stream().sorted(Comparator.comparingLong(PlayerDataLog::getDateMillis)).forEach(log -> {
                messages.add("&8[&cData Log&8] &8» &7" + timeFormatter.format(Instant.ofEpochMilli(log.getDateMillis())) + " - &c" + log.getEvent().getName());
                log.getComment().entrySet().forEach(entry -> messages.add("&8[&cData Log&8]   &8» &7" + entry.getKey() + ": " + entry.getValue()));
            });
            MessageUtil.send(sender, String.join("\n", messages));
            if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_YES);
            return true;
        }
        if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO);
        MessageUtil.send(sender, "&8[&cData Log&8] &cInvalid usage! &8(&c/playerdatalog uuid <uuid> &8| &c/border name <name> &8)");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("slux.developer"))
            return null;
        if (args.length == 1)
            return List.of("name", "uuid");
        if (args.length == 2 && args[0].equalsIgnoreCase("name"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("uuid"))
            return Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).map(uuid -> uuid.toString().replaceAll("-", "")).collect(Collectors.toList());
        return List.of();
    }
}
