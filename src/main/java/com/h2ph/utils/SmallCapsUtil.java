
package com.h2ph.utils;

public class SmallCapsUtil {
    private static final String REGULAR = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡxʏᴢ";

    public static String toSmallCaps(String text) {
        if (text == null)
            return "";
        char[] chars = text.toLowerCase().toCharArray();
        StringBuilder result = new StringBuilder();
        for (char c : chars) {
            int index = REGULAR.indexOf(c);
            if (index != -1) {
                result.append(SMALL_CAPS.charAt(index));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
