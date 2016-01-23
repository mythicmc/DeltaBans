package com.gmail.tracebachi.DeltaBans;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 1/14/16.
 */
public interface DeltaBansUtils
{
    Pattern DOT_PATTERN = Pattern.compile("\\.");
    Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    static boolean isIp(String input)
    {
        return IP_PATTERN.matcher(input).matches();
    }

    static boolean isSilent(String[] input)
    {
        boolean flag = false;
        for(String word : input)
        {
            flag |= word.equalsIgnoreCase("-s");
        }
        return flag;
    }

    static String[] filterSilent(String[] input)
    {
        int index = 0;
        String[] result = new String[input.length - 1];

        for(String word : input)
        {
            if(!word.equalsIgnoreCase("-s"))
            {
                result[index] = word;
                index++;
            }
        }
        return result;
    }

    static String formatDuration(Long input)
    {
        if(input == null)
        {
            return "Forever!";
        }

        long inputSeconds = input / 1000;
        long days = (inputSeconds / (24 * 60 * 60));
        long hours = (inputSeconds / (60 * 60)) % 24;
        long minutes = (inputSeconds / (60)) % 60;
        long seconds = inputSeconds % 60;

        return days + " days, " +
            hours + " hours, " +
            minutes + " minutes, " +
            seconds + " seconds";
    }

    static long convertIpToLong(String ip)
    {
        String[] splitIp = DOT_PATTERN.split(ip);
        long result = 0;

        for(int i = 0; i < 4; ++i)
        {
            result <<= 8;
            result |= (255 & Integer.parseInt(splitIp[i]));
        }
        return result;
    }
}
