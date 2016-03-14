package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DbShare.DbShare;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 2/21/16.
 */
public class Settings
{
    private static boolean debugEnabled;
    private static String database;
    private static String xAuthAccountsTable;
    private static Map<String, MessageFormat> formats = new HashMap<>();
    private static Map<Integer, List<String>> warningCommands = new HashMap<>();

    private Settings() {}

    public static void read(FileConfiguration config)
    {
        ConfigurationSection section;

        formats.clear();
        warningCommands.clear();
        debugEnabled = config.getBoolean("DebugMode", false);
        database = config.getString("Database");
        xAuthAccountsTable = config.getString("xAuth-AccountsTable");

        section = config.getConfigurationSection("Formats");

        for(String key : section.getKeys(false))
        {
            formats.put(key, new MessageFormat(section.getString(key)));
        }

        section = config.getConfigurationSection("WarningCommands");

        for(String key : section.getKeys(false))
        {
            Integer intKey = parseInt(key);

            if(intKey != null)
            {
                warningCommands.put(intKey, section.getStringList(key));
            }
        }
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static void setDebugEnabled(boolean debugEnabled)
    {
        Settings.debugEnabled = debugEnabled;
    }

    public static HikariDataSource getDataSource()
    {
        return DbShare.getDataSource(database);
    }

    public static String getIpCheckQuery()
    {
        return "SELECT lastloginip FROM `" + xAuthAccountsTable + "` WHERE playername = ?;";
    }

    public static String format(String key, String... args)
    {
        MessageFormat format = formats.get(key);
        return (format == null) ? "Unknown format for: " + key : format.format(args);
    }

    public static List<String> getWarningCommands(int amount)
    {
        return warningCommands.getOrDefault(amount, Collections.emptyList());
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
