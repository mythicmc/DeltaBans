/*
 * DeltaBans - Ban and warning plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans;

import com.google.common.base.Preconditions;

import java.util.regex.Pattern;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public interface DeltaBansUtils
{
  Pattern DOT_PATTERN = Pattern.compile("\\.");
  Pattern IP_PATTERN = Pattern.compile(
    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])");

  static boolean isIp(String input)
  {
    return IP_PATTERN.matcher(input).matches();
  }

  static boolean isSilent(String[] input)
  {
    return hasFlag(input, "-s");
  }

  static String[] filterSilent(String[] input)
  {
    return filterFlag(input, "-s");
  }

  static boolean hasFlag(String[] input, String flagName)
  {
    boolean flag = false;
    for (String word : input)
    {
      flag |= word.equalsIgnoreCase(flagName);
    }
    return flag;
  }

  static String[] filterFlag(String[] input, String flagName)
  {
    int index = 0;
    String[] result = new String[input.length - 1];

    for (String word : input)
    {
      if (!word.equalsIgnoreCase(flagName))
      {
        result[index] = word;
        index++;
      }
    }
    return result;
  }

  static String formatDuration(Long input)
  {
    if (input == null)
    {
      return "Forever!";
    }

    long inputSeconds = input / 1000;
    long days = (inputSeconds / (24 * 60 * 60));
    long hours = (inputSeconds / (60 * 60)) % 24;
    long minutes = (inputSeconds / (60)) % 60;
    long seconds = inputSeconds % 60;

    return days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds";
  }

  static long convertIpToLong(String ip)
  {
    Preconditions.checkNotNull(ip, "ip");

    String[] splitIp = DOT_PATTERN.split(ip);
    long result = 0;

    for (int i = 0; i < 4; ++i)
    {
      result <<= 8;
      result |= (255 & Integer.parseInt(splitIp[i]));
    }
    return result;
  }
}
