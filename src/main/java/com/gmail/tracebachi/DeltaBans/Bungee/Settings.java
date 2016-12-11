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

import com.gmail.tracebachi.DbShare.Bungee.DbShare;
import com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 2/21/16.
 */
public class Settings
{
    private static boolean debugEnabled;
    private static boolean startWithWhitelistEnabled;
    private static int warningDurationInMinutes;
    private static String dbShareDataSourceName;

    private Settings() {}

    public static void read(Configuration config)
    {
        debugEnabled = config.getBoolean("Debug", false);
        startWithWhitelistEnabled = config.getBoolean("StartWithWhitelistEnabled", false);
        warningDurationInMinutes = config.getInt("WarningDuration", 10080);
        dbShareDataSourceName = config.getString("DbShareDataSourceName", "<missing>");

        Configuration section = config.getSection("Formats");
        for(String key : section.getKeys())
        {
            String translated = ChatColor.translateAlternateColorCodes(
                '&',
                section.getString(key));
            ChatMessageHelper.instance().updateFormat(
                "DeltaBans." + key,
                translated);
        }
    }

    public static boolean isDebugEnabled()
    {
        return debugEnabled;
    }

    public static boolean shouldStartWithWhitelistEnabled()
    {
        return startWithWhitelistEnabled;
    }

    public static int getWarningDurationInMinutes()
    {
        return warningDurationInMinutes;
    }

    public static HikariDataSource getDataSource()
    {
        return DbShare.getDataSource(dbShareDataSourceName);
    }

    public static Connection getDataSourceConnection() throws SQLException
    {
        return DbShare.getDataSource(dbShareDataSourceName).getConnection();
    }
}
