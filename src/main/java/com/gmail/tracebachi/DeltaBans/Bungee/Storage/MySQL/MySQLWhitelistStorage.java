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
package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBans;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 8/27/16.
 */
public class MySQLWhitelistStorage implements WhitelistStorage
{
    private final int NORMAL_WHITELIST = 0b01;
    private final int RANGEBAN_WHITELIST = 0b10;

    private final String SELECT_ALL_WHITELIST_STATEMENT =
        "SELECT * FROM deltabans_whitelist;";
    private final String ADD_TO_WHITELIST_STATEMENT =
        " INSERT INTO deltabans_whitelist" +
        " (name, type)" +
        " VALUES (?,?);";
    private final String DELETE_FROM_WHITELIST_STATEMENT =
        " DELETE FROM deltabans_whitelist" +
        " WHERE name = ?";
    private final String UPDATE_IN_WHITELIST_STATEMENT =
        " UPDATE deltabans_whitelist" +
        " SET type = ?" +
        " WHERE name = ?" +
        " LIMIT 1;";

    private Map<String, Integer> whitelistMap = new HashMap<>();
    private DeltaBans plugin;

    public MySQLWhitelistStorage(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void load() throws SQLException
    {
        try(Connection connection = Settings.getDataSource().getConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(
                SELECT_ALL_WHITELIST_STATEMENT))
            {
                try(ResultSet resultSet = statement.executeQuery())
                {
                    while(resultSet.next())
                    {
                        try
                        {
                            String name = resultSet.getString("name");
                            int type = resultSet.getInt("type");

                            whitelistMap.put(name, type);
                        }
                        catch(SQLException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void shutdown()
    {
        plugin = null;
    }

    @Override
    public synchronized int getWhitelistSize()
    {
        return whitelistMap.size();
    }

    @Override
    public synchronized boolean isOnNormalWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        return (whitelistMap.getOrDefault(name, 0) & NORMAL_WHITELIST) != 0;
    }

    @Override
    public boolean addToNormalWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        Integer stored = whitelistMap.get(name);

        if(stored != null)
        {
            if((stored & NORMAL_WHITELIST) != 0)
            {
                return false;
            }
            else
            {
                whitelistMap.put(name, stored | NORMAL_WHITELIST);
                updateInDatabase(name, stored | NORMAL_WHITELIST);
                return true;
            }
        }
        else
        {
            whitelistMap.put(name, NORMAL_WHITELIST);
            addToDatabase(name, NORMAL_WHITELIST);
            return true;
        }
    }

    @Override
    public synchronized boolean removeFromNormalWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        Integer stored = whitelistMap.get(name);

        if(stored == null) { return false; }

        // Clear the flag
        Integer newFlags = stored & ~NORMAL_WHITELIST;

        if(stored.equals(newFlags))
        {
            return false;
        }
        else if(newFlags == 0)
        {
            whitelistMap.remove(name);
            removeFromDatabase(name);
            return true;
        }
        else
        {
            whitelistMap.put(name, newFlags);
            updateInDatabase(name, newFlags);
            return true;
        }
    }

    @Override
    public synchronized boolean isOnRangeBanWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        return (whitelistMap.getOrDefault(name, 0) & RANGEBAN_WHITELIST) != 0;
    }

    @Override
    public boolean addToRangeBanWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        Integer stored = whitelistMap.get(name);

        if(stored != null)
        {
            if((stored & RANGEBAN_WHITELIST) != 0)
            {
                return false;
            }
            else
            {
                whitelistMap.put(name, stored | RANGEBAN_WHITELIST);
                updateInDatabase(name, stored | RANGEBAN_WHITELIST);
                return true;
            }
        }
        else
        {
            whitelistMap.put(name, RANGEBAN_WHITELIST);
            addToDatabase(name, RANGEBAN_WHITELIST);
            return true;
        }
    }

    @Override
    public synchronized boolean removeFromRangeBanWhitelist(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        Integer stored = whitelistMap.get(name);

        if(stored == null) { return false; }

        // Clear the flag
        Integer newFlags = stored & ~RANGEBAN_WHITELIST;

        if(stored.equals(newFlags))
        {
            return false;
        }
        else if(newFlags == 0)
        {
            whitelistMap.remove(name);
            removeFromDatabase(name);
            return true;
        }
        else
        {
            whitelistMap.put(name, newFlags);
            updateInDatabase(name, newFlags);
            return true;
        }
    }

    private synchronized void addToDatabase(String name, Integer type)
    {
        Preconditions.checkNotNull(name, "Name was null.");
        Preconditions.checkNotNull(type, "Type was null.");

        String finalName = name.toLowerCase();

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    ADD_TO_WHITELIST_STATEMENT))
                {
                    statement.setString(1, finalName);
                    statement.setInt(2, type);

                    if(statement.execute())
                    {
                        plugin.debug("Inserted whitelisted name into database.");
                    }
                    else
                    {
                        plugin.debug("Failed to insert whitelisted name into database. " +
                            "Statement#execute() returned false.");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to insert whitelisted name into database.");
                e.printStackTrace();
            }
        });
    }

    private synchronized void removeFromDatabase(String name)
    {
        Preconditions.checkNotNull(name, "Name was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    DELETE_FROM_WHITELIST_STATEMENT))
                {
                    statement.setString(1, name);

                    int count = statement.executeUpdate();
                    if(count == 1)
                    {
                        plugin.debug("Deleted whitelisted name from database.");
                    }
                    else
                    {
                        plugin.debug("Failed to delete whitelisted name from database." +
                            "Statement#executeUpdate() returned " + count + ".");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to delete whitelisted name from database.");
                e.printStackTrace();
            }
        });
    }

    private synchronized void updateInDatabase(String name, Integer type)
    {
        Preconditions.checkNotNull(name, "Name was null.");
        Preconditions.checkNotNull(type, "Type was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    UPDATE_IN_WHITELIST_STATEMENT))
                {
                    statement.setInt(1, type);
                    statement.setString(2, name);

                    int count = statement.executeUpdate();
                    if(count == 1)
                    {
                        plugin.debug("Updated whitelisted name in database.");
                    }
                    else
                    {
                        plugin.debug("Failed to update whitelisted name in database." +
                            "Statement#executeUpdate() returned " + count + ".");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to update whitelisted name in database.");
                e.printStackTrace();
            }
        });
    }
}
