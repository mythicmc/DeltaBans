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
package com.yahoo.tracebachi.DeltaBans.Spigot;

import com.yahoo.tracebachi.DeltaBans.Spigot.Commands.*;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisPlugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class DeltaBansPlugin extends JavaPlugin
{
    private boolean debug;
    private String username;
    private String password;
    private String url;
    private String accountTable;

    private BanCommand banCommand;
    private UnbanCommand unbanCommand;
    private TempBanCommand tempBanCommand;
    private NameBanCommand nameBanCommand;
    private CheckBanCommand checkBanCommand;
    private WarnCommand warnCommand;
    private PardonWarnCommand pardonWarnCommand;
    private CheckWarnCommand checkWarnCommand;
    private SaveBansCommand saveBansCommand;
    private DeltaBansListener deltaBansListener;

    private Connection connection;

    @Override
    public void onLoad()
    {
        saveDefaultConfig();
    }

    @Override
    public void onEnable()
    {
        reloadConfig();
        debug = getConfig().getBoolean("DebugMode", false);
        username = getConfig().getString("Database.Username");
        password = getConfig().getString("Database.Password");
        url = getConfig().getString("Database.URL");
        accountTable = getConfig().getString("xAuth-AccountsTable");

        DeltaRedisPlugin plugin = (DeltaRedisPlugin) getServer().getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = plugin.getDeltaRedisApi();

        try
        {
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + url, username, password);
        }
        catch(SQLException e)
        {
            severe("Failed to connect to database containing xAuth account table.");
            severe("Shutting down.");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        deltaBansListener = new DeltaBansListener(this);
        getServer().getPluginManager().registerEvents(deltaBansListener, this);

        banCommand = new BanCommand(deltaRedisApi, this);
        getCommand("ban").setExecutor(banCommand);
        getCommand("ban").setTabCompleter(banCommand);

        tempBanCommand = new TempBanCommand(deltaRedisApi, this);
        getCommand("tempban").setExecutor(tempBanCommand);
        getCommand("tempban").setTabCompleter(tempBanCommand);

        nameBanCommand = new NameBanCommand(deltaRedisApi);
        getCommand("nameban").setExecutor(nameBanCommand);
        getCommand("nameban").setTabCompleter(nameBanCommand);

        unbanCommand = new UnbanCommand(deltaRedisApi);
        getCommand("unban").setExecutor(unbanCommand);

        checkBanCommand = new CheckBanCommand(deltaRedisApi);
        getCommand("checkban").setExecutor(checkBanCommand);

        warnCommand = new WarnCommand(deltaBansListener, deltaRedisApi);
        getCommand("warn").setExecutor(warnCommand);
        getCommand("warn").setTabCompleter(warnCommand);

        pardonWarnCommand = new PardonWarnCommand(deltaRedisApi);
        getCommand("pardon").setExecutor(pardonWarnCommand);
        getCommand("pardonall").setExecutor(pardonWarnCommand);
        getCommand("pardon").setTabCompleter(pardonWarnCommand);
        getCommand("pardonall").setTabCompleter(pardonWarnCommand);

        checkWarnCommand = new CheckWarnCommand(deltaRedisApi);
        getCommand("checkwarn").setExecutor(checkWarnCommand);
        getCommand("checkwarn").setTabCompleter(checkWarnCommand);

        saveBansCommand = new SaveBansCommand(deltaRedisApi);
        getCommand("savebans").setExecutor(saveBansCommand);
    }

    @Override
    public void onDisable()
    {
        deltaBansListener = null;

        if(saveBansCommand != null)
        {
            saveBansCommand.shutdown();
            saveBansCommand = null;
        }

        if(checkWarnCommand != null)
        {
            checkWarnCommand.shutdown();
            checkWarnCommand = null;
        }

        if(pardonWarnCommand != null)
        {
            pardonWarnCommand.shutdown();
            pardonWarnCommand = null;
        }

        if(warnCommand != null)
        {
            warnCommand.shutdown();
            warnCommand = null;
        }

        if(checkBanCommand != null)
        {
            checkBanCommand.shutdown();
            checkBanCommand = null;
        }

        if(nameBanCommand != null)
        {
            nameBanCommand.shutdown();
            nameBanCommand = null;
        }

        if(tempBanCommand != null)
        {
            tempBanCommand.shutdown();
            tempBanCommand = null;
        }

        if(unbanCommand != null)
        {
            unbanCommand.shutdown();
            unbanCommand = null;
        }

        if(banCommand != null)
        {
            banCommand.shutdown();
            banCommand = null;
        }

        try
        {
            connection.close();
        }
        catch(SQLException e)
        {
            e.printStackTrace();
        }
    }

    public String getIpOfPlayer(String playerName) throws IllegalArgumentException
    {
        String sql = "SELECT lastloginip FROM " + accountTable + " WHERE playername = ?;";

        try
        {
            updateConnection();
            try(PreparedStatement statement = connection.prepareStatement(sql))
            {
                statement.setString(1, playerName);
                try(ResultSet resultSet = statement.executeQuery())
                {
                    if(resultSet.next())
                    {
                        return resultSet.getString("lastloginip");
                    }
                    else
                    {
                        throw new IllegalArgumentException("There is no player by the name (" +
                            playerName + ") in the xAuth account table.");
                    }
                }
            }
        }
        catch(SQLException ex)
        {
            ex.printStackTrace();
            throw new IllegalArgumentException("Failed to access the xAuth accounts table.");
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
        if(debug)
        {
            getLogger().info("[Debug] " + message);
        }
    }

    private void updateConnection() throws SQLException
    {
        if(!connection.isValid(1))
        {
            connection.close();
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + url, username, password);
        }
    }
}
