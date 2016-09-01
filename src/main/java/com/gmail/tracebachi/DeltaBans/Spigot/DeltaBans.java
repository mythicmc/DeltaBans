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

import com.gmail.tracebachi.DeltaBans.Spigot.Commands.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class DeltaBans extends JavaPlugin
{
    private BanCommand banCommand;
    private BannedCommand bannedCommand;
    private KickCommand kickCommand;
    private NameBanCommand nameBanCommand;
    private RangeBanCommand rangeBanCommand;
    private RangeUnbanCommand rangeUnbanCommand;
    private RangeWhitelistCommand rangeWhitelistCommand;
    private TempBanCommand tempBanCommand;
    private TempNameBanCommand tempNameBanCommand;
    private UnbanCommand unbanCommand;
    private UnwarnCommand unwarnCommand;
    private WarnCommand warnCommand;
    private WhitelistCommand whitelistCommand;

    @Override
    public void onLoad()
    {
        saveDefaultConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        Settings.read(getConfig());

        banCommand = new BanCommand(this);
        banCommand.register();

        bannedCommand = new BannedCommand(this);
        bannedCommand.register();

        kickCommand = new KickCommand(this);
        kickCommand.register();

        nameBanCommand = new NameBanCommand(this);
        nameBanCommand.register();

        rangeBanCommand = new RangeBanCommand(this);
        rangeBanCommand.register();

        rangeUnbanCommand = new RangeUnbanCommand(this);
        rangeUnbanCommand.register();

        rangeWhitelistCommand = new RangeWhitelistCommand(this);
        rangeWhitelistCommand.register();

        tempBanCommand = new TempBanCommand(this);
        tempBanCommand.register();

        tempNameBanCommand = new TempNameBanCommand(this);
        tempNameBanCommand.register();

        unbanCommand = new UnbanCommand(this);
        unbanCommand.register();

        unwarnCommand = new UnwarnCommand(this);
        unwarnCommand.register();

        warnCommand = new WarnCommand(this);
        warnCommand.register();

        whitelistCommand = new WhitelistCommand(this);
        whitelistCommand.register();
    }

    @Override
    public void onDisable()
    {
        whitelistCommand.shutdown();
        whitelistCommand = null;

        warnCommand.shutdown();
        warnCommand = null;

        unwarnCommand.shutdown();
        unwarnCommand = null;

        unbanCommand.shutdown();
        unbanCommand = null;

        tempNameBanCommand.shutdown();
        tempNameBanCommand = null;

        tempBanCommand.shutdown();
        tempBanCommand = null;

        rangeUnbanCommand.shutdown();
        rangeUnbanCommand = null;

        rangeWhitelistCommand.shutdown();
        rangeWhitelistCommand = null;

        rangeBanCommand.shutdown();
        rangeBanCommand = null;

        nameBanCommand.shutdown();
        nameBanCommand = null;

        kickCommand.shutdown();
        kickCommand = null;

        bannedCommand.shutdown();
        bannedCommand = null;

        banCommand.shutdown();
        banCommand = null;
    }

    public String getIpOfPlayer(String playerName)
    {
        try(Connection connection = Settings.getDataSourceConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(Settings.getIpLookupQuery()))
            {
                statement.setString(1, playerName);

                try(ResultSet resultSet = statement.executeQuery())
                {
                    return (resultSet.next()) ?
                        resultSet.getString(Settings.getIpColumnName()) :
                        null;
                }
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            throw new IllegalArgumentException("Failed to access the accounts table.");
        }
    }

    public void info(String message)
    {
        getLogger().info(message);
    }

    public void severe(String message)
    {
        getLogger().severe(message);
    }

    public void debug(String message)
    {
        if(Settings.isDebugEnabled())
        {
            getLogger().info("[Debug] " + message);
        }
    }

    private void testConnection() throws SQLException
    {
        try(Connection connection = Settings.getDataSource().getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                statement.execute("SELECT 1;");
            }
        }
    }
}
