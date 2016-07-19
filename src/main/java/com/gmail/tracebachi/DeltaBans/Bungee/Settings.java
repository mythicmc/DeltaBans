/*
 * This file is part of DeltaBans.
 *
 * DeltaBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaBans.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Bungee;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 2/21/16.
 */
public class Settings
{
    private static boolean debugEnabled;
    private static boolean whitelistEnabled;
    private static int minutesPerBanSave;
    private static int minutesPerWarningCleanup;
    private static int warningDuration;
    private static Map<String, MessageFormat> formats = new HashMap<>();

    private Settings() {}

    public static void read(Configuration config)
    {
        Configuration section = config.getSection("Formats");

        formats.clear();
        minutesPerBanSave = config.getInt("MinutesPerBanSave");
        minutesPerWarningCleanup = config.getInt("MinutesPerWarningCleanup");
        warningDuration = config.getInt("WarningDuration");
        debugEnabled = config.getBoolean("DebugMode", false);
        whitelistEnabled = config.getBoolean("Whitelist", false);

        for(String key : section.getKeys())
        {
            String translated = ChatColor.translateAlternateColorCodes('&', section.getString(key));

            formats.put(key, new MessageFormat(translated));
        }
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static boolean isWhitelistEnabled()
    {
        return whitelistEnabled;
    }

    public static void setWhitelistEnabled(boolean whitelistEnabled)
    {
        Settings.whitelistEnabled = whitelistEnabled;
    }

    public static int getMinutesPerBanSave()
    {
        return minutesPerBanSave;
    }

    public static int getMinutesPerWarningCleanup()
    {
        return minutesPerWarningCleanup;
    }

    public static int getWarningDuration()
    {
        return warningDuration;
    }

    public static String format(String key, String... args)
    {
        MessageFormat format = formats.get(key);
        return (format == null) ? "Unknown format for: " + key : format.format(args);
    }

    private static Integer parseInt(String src)
    {
        try
        {
            return Integer.parseInt(src);
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }
}
