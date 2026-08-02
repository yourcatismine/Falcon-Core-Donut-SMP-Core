package com.falconcore.survival.utils;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import java.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemSerializationManager {

    /**
     * Converts an array of ItemStacks to a Base64 encoded string.
     *
     * @param items The array of ItemStacks to serialize.
     * @return A Base64 string representing the items.
     */
    public static String itemStackArrayToBase64(ItemStack[] items) {
        if (items == null)
            return "";
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

            dataOutput.writeInt(items.length);

            for (int i = 0; i < items.length; i++) {
                dataOutput.writeObject(items[i]);
            }

            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to save item stacks.", e);
        }
    }

    /**
     * Converts a Base64 string back into an array of ItemStacks.
     *
     * @param data The Base64 string to deserialize.
     * @return An array of ItemStacks.
     * @throws IOException            If the data is corrupted.
     * @throws ClassNotFoundException If there is a class loading issue during
     *                                deserialization.
     */
    public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
        if (data == null || data.isEmpty())
            return new ItemStack[0];

        try {
            // Replace all whitespaces (including newlines) to support decoding older Base64Coder.encodeLines
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data.replaceAll("\\s+", "")));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            dataInput.close();
            return items;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unable to decode class type.", e);
        }
    }

    public static String itemStackListToBase64(java.util.List<ItemStack> items) {
        if (items == null)
            return "";
        return itemStackArrayToBase64(items.toArray(new ItemStack[0]));
    }

    public static java.util.List<ItemStack> itemStackListFromBase64(String data) throws IOException {
        ItemStack[] array = itemStackArrayFromBase64(data);
        return java.util.Arrays.asList(array);
    }
}
