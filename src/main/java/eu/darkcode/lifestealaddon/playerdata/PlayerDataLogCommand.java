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
import java.util.stream.Stream;

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

        if(args.length == 2 || (args.length == 4 && args[2].equalsIgnoreCase("|"))) {
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
                MessageUtil.send(sender, "&8[&cData Log&8] &cFailed to load logs!");
                return true;
            }
            Stream<PlayerDataLog> stream = logs.stream();
            if(args.length == 4)
                stream = stream.filter((log) -> log.getEvent().getName().equalsIgnoreCase(args[3]));

            List<PlayerDataLog> list = stream.toList();
            if(list.isEmpty()){
                if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_HURT);
                MessageUtil.send(sender, "&8[&cData Log&8] &cNo logs found!");
                return true;
            }
            List<String> messages = new ArrayList<>();
            list.stream().sorted(Comparator.comparingLong(PlayerDataLog::getDateMillis)).forEach(log -> {
                messages.add("&8[&cData Log&8] &8» &7" + TIME_FORMATTER.format(Instant.ofEpochMilli(log.getDateMillis())) + " - &c[" + log.getEvent().getName() + "]");
                log.getComment().entrySet().forEach(entry -> messages.add("&8[&cData Log&8]   &8» &7" + entry.getKey() + ": " + entry.getValue()));
            });
            MessageUtil.send(sender, String.join("\n", messages));
            if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_YES);
            return true;
        }
        if(sender instanceof Player player) SoundUtil.playSound(player, Sound.ENTITY_VILLAGER_NO);
        MessageUtil.send(sender, "&8[&cData Log&8] &cInvalid usage! &8(&c/playerdatalog uuid <uuid> &8| &c/playerdatalog name <name> &8)");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("slux.developer"))
            return null;
        if (args.length == 1)
            return List.of("name", "uuid");
        if (args.length == 2 && args[0].equalsIgnoreCase("name"))
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("uuid"))
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getUniqueId)
                    .map(uuid -> uuid.toString().replaceAll("-", ""))
                    .filter(uuid -> uuid.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        if(args.length == 3)
            return List.of("|");
        if(args.length == 4 && args[2].equalsIgnoreCase("|"))
            return Arrays.stream(PlayerDataLog.Event
                    .values())
                    .map(PlayerDataLog.Event::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[3].toLowerCase()))
                    .collect(Collectors.toList());
        return List.of();
    }
}
