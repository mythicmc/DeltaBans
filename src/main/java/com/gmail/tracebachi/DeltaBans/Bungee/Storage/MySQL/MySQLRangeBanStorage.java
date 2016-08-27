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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class MySQLRangeBanStorage implements RangeBanStorage
{
    private final String SELECT_ALL_BANS_STATEMENT =
        "SELECT * FROM deltabans_rangebans;";
    private final String ADD_BAN_ENTRY_STATEMENT =
        " INSERT INTO deltabans_rangebans" +
        " (ip_start, ip_end, banner, message, createdAt)" +
        " VALUES (?,?,?,?,?);";
    private final String DELETE_BAN_ENTRY_STATEMENT =
        " DELETE FROM deltabans_rangebans" +
        " WHERE ip_start = ? AND ip_end = ? AND banner = ? AND message = ?" +
        " LIMIT 1;";

    private List<RangeBanEntry> rangeBanList = new ArrayList<>();
    private DeltaBans plugin;

    public MySQLRangeBanStorage(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void load() throws SQLException
    {
        try(Connection connection = Settings.getDataSource().getConnection())
        {
            try(PreparedStatement statement = connection.prepareStatement(
                SELECT_ALL_BANS_STATEMENT))
            {
                try(ResultSet resultSet = statement.executeQuery())
                {
                    if(resultSet.next())
                    {
                        loadFromResultSet(resultSet);
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
    public synchronized int getTotalRangeBanCount()
    {
        return rangeBanList.size();
    }

    @Override
    public synchronized void add(RangeBanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        rangeBanList.add(entry);
        addToDatabase(entry);
    }

    @Override
    public synchronized RangeBanEntry getIpRangeBan(String ip)
    {
        return getIpRangeBan(DeltaBansUtils.convertIpToLong(ip));
    }

    @Override
    public synchronized RangeBanEntry getIpRangeBan(long ipAsLong)
    {
        for(RangeBanEntry entry : rangeBanList)
        {
            if(ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
            {
                return entry;
            }
        }
        return null;
    }

    @Override
    public synchronized int removeIpRangeBan(String ip)
    {
        return removeIpRangeBan(DeltaBansUtils.convertIpToLong(ip));
    }

    @Override
    public synchronized int removeIpRangeBan(long ipAsLong)
    {
        int count = 0;
        ListIterator<RangeBanEntry> iterator = rangeBanList.listIterator();

        while(iterator.hasNext())
        {
            RangeBanEntry entry = iterator.next();

            if(ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
            {
                iterator.remove();
                removeFromDatabase(entry);
                count++;
            }
        }
        return count;
    }

    private synchronized void addToDatabase(RangeBanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    ADD_BAN_ENTRY_STATEMENT))
                {
                    statement.setString(1, entry.getStartAddress());
                    statement.setString(2, entry.getEndAddress());
                    statement.setString(3, entry.getBanner());
                    statement.setString(4, entry.getMessage());
                    statement.setTimestamp(5, new Timestamp(entry.getCreatedAt()));

                    if(statement.execute())
                    {
                        plugin.debug("Inserted range ban entry into database.");
                    }
                    else
                    {
                        plugin.debug("Failed to insert range ban entry into database. " +
                            "Statement#execute() returned false.");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to insert range ban entry into database.");
                e.printStackTrace();
            }
        });
    }

    private synchronized void removeFromDatabase(RangeBanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    DELETE_BAN_ENTRY_STATEMENT))
                {
                    statement.setString(1, entry.getStartAddress());
                    statement.setString(2, entry.getEndAddress());
                    statement.setString(3, entry.getBanner());
                    statement.setString(4, entry.getMessage());

                    int count = statement.executeUpdate();
                    if(count == 1)
                    {
                        plugin.debug("Deleted range ban entry from database.");
                    }
                    else
                    {
                        plugin.debug("Failed to delete range ban entry from database." +
                            "Statement#executeUpdate() returned " + count + ".");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to delete range ban entry from database.");
                e.printStackTrace();
            }
        });
    }

    private void loadFromResultSet(ResultSet resultSet)
    {
        RangeBanEntry entry;

        try
        {
            entry = new RangeBanEntry(
                resultSet.getString("banner"),
                resultSet.getString("message"),
                resultSet.getString("ip_start"),
                resultSet.getString("ip_end"),
                resultSet.getTimestamp("createdAt").getTime());
        }
        catch(SQLException | NullPointerException e)
        {
            e.printStackTrace();
            return;
        }

        rangeBanList.add(entry);
    }
}
