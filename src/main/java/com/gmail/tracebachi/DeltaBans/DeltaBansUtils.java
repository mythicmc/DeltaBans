package com.gmail.tracebachi.DeltaBans;

import com.google.common.base.Preconditions;
import java.util.regex.Pattern;

public interface DeltaBansUtils {
    Pattern DOT_PATTERN = Pattern.compile("\\.");
    Pattern IP_PATTERN = Pattern.compile("([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])");

    static boolean isIp(String input) {
        return IP_PATTERN.matcher(input).matches();
    }

    static boolean isSilent(String[] input) {
        return hasFlag(input, "-s");
    }

    static String[] filterSilent(String[] input) {
        return filterFlag(input, "-s");
    }

    static boolean hasFlag(String[] input, String flagName) {
        boolean flag = false;
        String[] var3 = input;
        int var4 = input.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            String word = var3[var5];
            flag |= word.equalsIgnoreCase(flagName);
        }

        return flag;
    }

    static String[] filterFlag(String[] input, String flagName) {
        int index = 0;
        String[] result = new String[input.length - 1];
        String[] var4 = input;
        int var5 = input.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String word = var4[var6];
            if (!word.equalsIgnoreCase(flagName)) {
                result[index] = word;
                ++index;
            }
        }

        return result;
    }

    static String formatDuration(Long input) {
        if (input == null) {
            return "Forever!";
        } else {
            long inputSeconds = input / 1000L;
            long days = inputSeconds / 86400L;
            long hours = inputSeconds / 3600L % 24L;
            long minutes = inputSeconds / 60L % 60L;
            long seconds = inputSeconds % 60L;
            return days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
        }
    }

    static long convertIpToLong(String ip) {
        Preconditions.checkNotNull(ip, "ip");
        String[] splitIp = DOT_PATTERN.split(ip);
        long result = 0L;

        for(int i = 0; i < 4; ++i) {
            result <<= 8;
            result |= (long)(255 & Integer.parseInt(splitIp[i]));
        }

        return result;
    }
}
