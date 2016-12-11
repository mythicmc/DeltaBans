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
package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DbShare.Spigot.DbShare;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
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
    private static boolean debugEnabled;
    private static String databaseName;
    private static String accountsTableName;
    private static String ipColumnName;
    private static String playerColumnName;
    private static Map<String, MessageFormat> formats = new HashMap<>();
    private static Map<Integer, List<String>> warningCommands = new HashMap<>();

    private Settings() {}

    public static void read(FileConfiguration config)
    {
        ConfigurationSection section;

        formats.clear();
        warningCommands.clear();

        debugEnabled = config.getBoolean("DebugMode", false);
        databaseName = config.getString("Database.Name");
        accountsTableName = config.getString("Database.Table");
        ipColumnName = config.getString("Database.IpColumn");
        playerColumnName = config.getString("Database.PlayerColumn");

        section = config.getConfigurationSection("Formats");

        for(String key : section.getKeys(false))
        {
            String translated = ChatColor.translateAlternateColorCodes('&', section.getString(key));

            formats.put(key, new MessageFormat(translated));
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

    public static String getAccountsTableName()
    {
        return accountsTableName;
    }

    public static String getIpColumnName()
    {
        return ipColumnName;
    }

    public static String getPlayerColumnName()
    {
        return playerColumnName;
    }

    public static HikariDataSource getDataSource()
    {
        return DbShare.getDataSource(databaseName);
    }

    public static Connection getDataSourceConnection() throws SQLException
    {
        return DbShare.getDataSource(databaseName).getConnection();
    }

    public static String getIpLookupQuery()
    {
        return
            "SELECT `" + ipColumnName + "` " +
            "FROM `" + accountsTableName + "` " +
            "WHERE `" + playerColumnName + "` = ?;";
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
