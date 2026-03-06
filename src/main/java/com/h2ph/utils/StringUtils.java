package com.h2ph.utils;

import java.util.HashMap;
import java.util.Map;

public class StringUtils {

    private static final Map<Character, Character> smallCapsMap = new HashMap<>();

    static {
        char[] normal = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ".toCharArray();

        for (int i = 0; i < normal.length; i++) {
            if (i < small.length) {
                smallCapsMap.put(normal[i], small[i]);
                smallCapsMap.put(Character.toUpperCase(normal[i]), small[i]);
            }
        }
    }

    public static String toSmallCaps(String input) {
        if (input == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            sb.append(smallCapsMap.getOrDefault(c, c));
        }
        return sb.toString();
    }
}
