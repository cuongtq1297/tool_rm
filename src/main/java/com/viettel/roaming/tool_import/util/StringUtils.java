package com.viettel.roaming.tool_import.util;

public class StringUtils {
    public static boolean isBlank(String str) {
        if (str == null) return true;
        if (str.trim().equals("")) return true;
        return false;
    }

    public static String join(String delimiter, String[] strs) {
        if (strs == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strs.length; i++) {
            if (i == strs.length -1) {
                sb.append(strs[i]);
            } else {
                sb.append(strs[i]).append(delimiter);
            }
        }
        return sb.toString();
    }
}
