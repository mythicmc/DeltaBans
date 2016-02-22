package com.gmail.tracebachi.DeltaBans.Spigot;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 2/21/16.
 */
public class Settings
{
    private String database;
    private String xAuthAccountsTable;
    private Map<String, MessageFormat> formats;
    private Map<Integer, List<String>> warningCommands;

    public void read(JavaPlugin plugin)
    {
        ConfigurationSection section;
        FileConfiguration config = plugin.getConfig();

        database = config.getString("Database");
        xAuthAccountsTable = config.getString("xAuth-AccountsTable");
        formats = new HashMap<>();
        warningCommands = new HashMap<>();

        section = config.getConfigurationSection("Formats");

        for(String key : section.getKeys(false))
        {
            formats.put(key, new MessageFormat(section.getString(key)));
        }

        section = config.getConfigurationSection("WarningCommands");

        for(String key : section.getKeys(false))
        {
            Integer intKey = parseInt(key);

            if(intKey == null)
            {
                plugin.getLogger().severe(key + " is not a valid amount of warnings. Ignoring section.");
            }
            else
            {
                warningCommands.put(intKey, section.getStringList(key));
            }
        }
    }

    public String getDatabase()
    {
        return database;
    }

    public String getIpCheckQuery()
    {
        return "SELECT lastloginip FROM `" + xAuthAccountsTable + "` WHERE playername = ?;";
    }

    public String format(String key, String... args)
    {
        MessageFormat format = formats.get(key);

        if(format == null)
        {
            return "{key: " + key + "} not found.";
        }

        return format.format(args);
    }

    public List<String> getWarningCommands(int amount)
    {
        return warningCommands.getOrDefault(amount, Collections.emptyList());
    }

    private Integer parseInt(String src)
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
