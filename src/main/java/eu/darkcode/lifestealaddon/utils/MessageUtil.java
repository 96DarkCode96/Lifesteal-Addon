package eu.darkcode.lifestealaddon.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class MessageUtil {

    private MessageUtil() {}


    public static void send(CommandSender sender, String msg) {
        sender.sendMessage(reformat(msg));
    }

    private static String reformat(String msg) {
        msg = msg.replaceAll("&", "§");
        return msg;
    }

    public static void kick(Player player, String msg) {
        player.kickPlayer(reformat(msg));
    }
}