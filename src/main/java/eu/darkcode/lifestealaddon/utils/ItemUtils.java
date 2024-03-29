package eu.darkcode.lifestealaddon.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public final class ItemUtils {

    private ItemUtils() {}

    public static @NotNull String itemStackArrayToBase64(@NotNull ItemStack[] itemStacks) {
        try{
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(arrayOutputStream);

            bukkitObjectOutputStream.writeInt(itemStacks.length);
            for (ItemStack itemStack : itemStacks) bukkitObjectOutputStream.writeObject(itemStack);

            bukkitObjectOutputStream.close();
            return Base64Coder.encodeLines(arrayOutputStream.toByteArray());
        }catch (Throwable e){
            throw new RuntimeException("Failed to encode item stacks", e);
        }
    }

    public static @NotNull ItemStack[] base64ToItemStackArray(@NotNull String base64) {
        try {
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(arrayInputStream);

            ItemStack[] itemStacks = new ItemStack[bukkitObjectInputStream.readInt()];
            for (int i = 0; i < itemStacks.length; i++) itemStacks[i] = (ItemStack) bukkitObjectInputStream.readObject();

            bukkitObjectInputStream.close();
            return itemStacks;
        }catch (Throwable e){
            throw new RuntimeException("Failed to decode item stacks", e);
        }
    }

    public static @NotNull String itemStackToBase64(@NotNull ItemStack itemStack) {
        try{
            ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(arrayOutputStream);

            bukkitObjectOutputStream.writeObject(itemStack);

            bukkitObjectOutputStream.close();
            return Base64Coder.encodeLines(arrayOutputStream.toByteArray());
        }catch (Throwable e){
            throw new RuntimeException("Failed to encode item stack", e);
        }
    }

    public static @NotNull ItemStack base64ToItemStack(@NotNull String base64) {
        try {
            ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
            BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(arrayInputStream);

            ItemStack itemStack = (ItemStack) bukkitObjectInputStream.readObject();

            bukkitObjectInputStream.close();
            return itemStack;
        }catch (Throwable e){
            throw new RuntimeException("Failed to decode item stack", e);
        }
    }

}