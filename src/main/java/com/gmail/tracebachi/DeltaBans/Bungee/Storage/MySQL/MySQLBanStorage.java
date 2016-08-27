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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class MySQLBanStorage implements BanStorage
{
    private final String SELECT_ALL_BANS_STATEMENT =
        "SELECT * FROM deltabans_bans;";
    private final String ADD_BAN_ENTRY_STATEMENT =
        " INSERT INTO deltabans_bans" +
        " (name, ip, banner, message, duration, createdAt)" +
        " VALUES (?,?,?,?,?,?);";
    private final String DELETE_BAN_ENTRY_NO_NULL_STATEMENT =
        " DELETE FROM deltabans_bans" +
        " WHERE name = ? AND ip = ? AND banner = ? AND message = ?" +
        " LIMIT 1;";
    private final String DELETE_BAN_ENTRY_NAME_NULL_STATEMENT =
        " DELETE FROM deltabans_bans" +
        " WHERE name is NULL AND ip = ? AND banner = ? AND message = ?" +
        " LIMIT 1;";
    private final String DELETE_BAN_ENTRY_IP_NULL_STATEMENT =
        " DELETE FROM deltabans_bans" +
        " WHERE name = ? AND ip is NULL AND banner = ? AND message = ?" +
        " LIMIT 1;";

    private Set<BanEntry> banSet = new HashSet<>();
    private Map<String, BanEntry> nameBanMap = new HashMap<>();
    private Map<String, List<BanEntry>> ipBanMap = new HashMap<>();
    private DeltaBans plugin;

    public MySQLBanStorage(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public synchronized void load() throws SQLException
    {
        try(Connection connection = Settings.getDataSourceConnection())
        {
            List<BanEntry> entriesToRemove = new ArrayList<>(64);

            try(PreparedStatement statement = connection.prepareStatement(
                SELECT_ALL_BANS_STATEMENT))
            {
                try(ResultSet resultSet = statement.executeQuery())
                {
                    if(resultSet.next())
                    {
                        loadFromResultSet(resultSet, entriesToRemove);
                    }
                }
            }

            for(BanEntry entry : entriesToRemove)
            {
                if(entry.hasName() && entry.hasIp())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_NO_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getIp());
                        statement.setString(3, entry.getBanner());
                        statement.setString(4, entry.getMessage());
                        statement.executeUpdate();
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
                else if(entry.hasName())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_IP_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getBanner());
                        statement.setString(3, entry.getMessage());
                        statement.executeUpdate();
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
                else if(entry.hasIp())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_NAME_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getIp());
                        statement.setString(2, entry.getBanner());
                        statement.setString(3, entry.getMessage());
                        statement.executeUpdate();
                    }
                    catch(SQLException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public synchronized void shutdown()
    {
        plugin = null;
    }

    @Override
    public synchronized int getTotalBanCount()
    {
        return banSet.size();
    }

    @Override
    public synchronized AddResult addBanEntry(BanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Ban was null.");

        boolean hasName = entry.hasName();
        boolean hasIp = entry.hasIp();

        // If the entry is a name-only ban
        if(hasName && !hasIp)
        {
            BanEntry foundEntry = nameBanMap.get(entry.getName());

            if(foundEntry != null)
            {
                if(foundEntry.hasIp())
                {
                    return AddResult.NAME_AND_IP_ALREADY_BANNED;
                }
                else
                {
                    return AddResult.NAME_ALREADY_BANNED;
                }
            }

            addToMemory(entry);
            addToDatabase(entry);
            return AddResult.SUCCESS;
        }

        // If the entry is an ip-only ban
        if(!hasName && hasIp)
        {
            List<BanEntry> banEntries = ipBanMap.get(entry.getIp());

            if(banEntries != null)
            {
                for(BanEntry iter : banEntries)
                {
                    if(!iter.hasName())
                    {
                        return AddResult.IP_ALREADY_BANNED;
                    }
                }
            }

            addToMemory(entry);
            addToDatabase(entry);
            return AddResult.SUCCESS;
        }

        // If the entry is a combined name and ip ban

        BanEntry foundEntry = nameBanMap.get(entry.getName());

        if(foundEntry != null)
        {
            if(foundEntry.hasIp())
            {
                return AddResult.NAME_AND_IP_ALREADY_BANNED;
            }
            else
            {
                removeFromMemory(foundEntry);
                removeFromDatabase(foundEntry);
                addToMemory(entry);
                addToDatabase(entry);
                return AddResult.SUCCESS;
            }
        }

        addToMemory(entry);
        addToDatabase(entry);
        return AddResult.SUCCESS;
    }

    @Override
    public synchronized List<BanEntry> removeUsingIp(String ip)
    {
        Preconditions.checkNotNull(ip, "IP was null.");

        List<BanEntry> banEntries = ipBanMap.remove(ip);

        if(banEntries == null) { return Collections.emptyList(); }

        for(BanEntry entry : banEntries)
        {
            removeFromMemory(entry);
            removeFromDatabase(entry);

            if(entry.hasName())
            {
                BanEntry newEntry = new BanEntry(
                    entry.getName(),
                    null,
                    entry.getBanner(),
                    entry.getMessage(),
                    entry.getDuration(),
                    entry.getCreatedAt());

                addToMemory(newEntry);
                addToDatabase(newEntry);
            }
        }

        return Collections.unmodifiableList(banEntries);
    }

    @Override
    public synchronized BanEntry removeUsingName(String name)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        BanEntry entryToRemove = nameBanMap.remove(name);

        if(entryToRemove == null) { return null; }

        removeFromMemory(entryToRemove);
        removeFromDatabase(entryToRemove);

        return entryToRemove;
    }

    @Override
    public synchronized BanEntry getBanEntry(String name, String ip)
    {
        Preconditions.checkNotNull(name != null || ip != null, "Name and IP were both null.");

        BanEntry foundEntry = null;

        if(name != null)
        {
            name = name.toLowerCase();
            foundEntry = nameBanMap.get(name);

            if(foundEntry != null)
            {
                if(foundEntry.isDurationComplete())
                {
                    removeFromMemory(foundEntry);
                    removeFromDatabase(foundEntry);
                }
                else
                {
                    return foundEntry;
                }
            }
        }

        if(ip != null)
        {
            List<BanEntry> banEntries = ipBanMap.get(ip);

            if(banEntries == null) { return null; }

            Iterator<BanEntry> iter = banEntries.iterator();

            while(iter.hasNext())
            {
                BanEntry entry = iter.next();

                if(entry.isDurationComplete())
                {
                    iter.remove();
                    removeFromMemory(entry);
                    removeFromDatabase(entry);
                }
                else
                {
                    foundEntry = entry;
                }
            }

            if(banEntries.isEmpty())
            {
                ipBanMap.remove(ip);
            }
        }

        return foundEntry;
    }

    private synchronized void addToMemory(BanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        banSet.add(entry);

        if(entry.hasName())
        {
            nameBanMap.put(entry.getName(), entry);
        }

        if(entry.hasIp())
        {
            String ip = entry.getIp();
            List<BanEntry> banEntries = ipBanMap.get(ip);

            if(banEntries == null)
            {
                banEntries = new ArrayList<>();
                ipBanMap.put(ip, banEntries);
            }

            banEntries.add(entry);
        }
    }

    private synchronized void removeFromMemory(BanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        if(entry.hasIp())
        {
            String ip = entry.getIp();
            List<BanEntry> banEntries = ipBanMap.get(ip);

            if(banEntries != null)
            {
                // Comparison is based on memory address, which is the desired behavior
                banEntries.remove(entry);

                if(banEntries.isEmpty())
                {
                    ipBanMap.remove(ip);
                }
            }
        }

        if(entry.hasName())
        {
            nameBanMap.remove(entry.getName());
        }

        // Comparison is based on memory address, which is the desired behavior
        banSet.remove(entry);
    }

    private synchronized void addToDatabase(BanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    ADD_BAN_ENTRY_STATEMENT))
                {
                    statement.setString(1, entry.getName());
                    statement.setString(2, entry.getIp());
                    statement.setString(3, entry.getBanner());
                    statement.setString(4, entry.getMessage());

                    if(entry.getDuration() == null)
                    {
                        statement.setNull(5, Types.BIGINT);
                    }
                    else
                    {
                        statement.setLong(5, entry.getDuration());
                    }

                    statement.setTimestamp(6, new Timestamp(entry.getCreatedAt()));

                    if(statement.execute())
                    {
                        plugin.debug("Inserted ban entry into database.");
                    }
                    else
                    {
                        plugin.debug("Failed to insert ban entry into database. " +
                            "Statement#execute() returned false.");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to inserted ban entry into database.");
                e.printStackTrace();
            }
        });
    }

    private synchronized void removeFromDatabase(BanEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                if(entry.hasName() && entry.hasIp())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_NO_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getIp());
                        statement.setString(3, entry.getBanner());
                        statement.setString(4, entry.getMessage());

                        int count = statement.executeUpdate();
                        if(count == 1)
                        {
                            plugin.debug("Deleted ban entry from database.");
                        }
                        else
                        {
                            plugin.debug("Failed to delete ban entry from database." +
                                "Statement#executeUpdate() returned " + count + ".");
                        }
                    }
                }
                else if(entry.hasName())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_IP_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getBanner());
                        statement.setString(3, entry.getMessage());

                        int count = statement.executeUpdate();
                        if(count == 1)
                        {
                            plugin.debug("Deleted ban entry from database.");
                        }
                        else
                        {
                            plugin.debug("Failed to delete ban entry from database." +
                                "Statement#executeUpdate() returned " + count + ".");
                        }
                    }
                }
                else if(entry.hasIp())
                {
                    try(PreparedStatement statement = connection.prepareStatement(
                        DELETE_BAN_ENTRY_NAME_NULL_STATEMENT))
                    {
                        statement.setString(1, entry.getIp());
                        statement.setString(2, entry.getBanner());
                        statement.setString(3, entry.getMessage());

                        int count = statement.executeUpdate();
                        if(count == 1)
                        {
                            plugin.debug("Deleted ban entry from database.");
                        }
                        else
                        {
                            plugin.debug("Failed to delete ban entry from database." +
                                "Statement#executeUpdate() returned " + count + ".");
                        }
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to delete ban entry from database.");
                e.printStackTrace();
            }
        });
    }

    private boolean loadFromResultSet(ResultSet resultSet, List<BanEntry> entriesToRemove)
    {
        BanEntry entry;

        try
        {
            entry = new BanEntry(
                resultSet.getString("name"),
                resultSet.getString("ip"),
                resultSet.getString("banner"),
                resultSet.getString("message"),
                resultSet.getLong("duration"),
                resultSet.getTimestamp("createdAt").getTime());
        }
        catch(SQLException | IllegalArgumentException e)
        {
            e.printStackTrace();
            return false;
        }

        boolean hasName = entry.hasName();
        boolean hasIp = entry.hasIp();

        if(entry.isDurationComplete())
        {
            entriesToRemove.add(entry);
            return false;
        }

        // If the entry is a name-only ban
        if(hasName && !hasIp)
        {
            BanEntry foundEntry = nameBanMap.get(entry.getName());

            if(foundEntry != null)
            {
                entriesToRemove.add(entry);
                return false;
            }

            addToMemory(entry);
            return true;
        }

        // If the entry is an ip-only ban
        if(!hasName && hasIp)
        {
            List<BanEntry> banEntries = ipBanMap.get(entry.getIp());

            if(banEntries != null)
            {
                for(BanEntry iter : banEntries)
                {
                    if(!iter.hasName())
                    {
                        entriesToRemove.add(entry);
                        return false;
                    }
                }
            }

            addToMemory(entry);
            return true;
        }

        // If the entry is a combined name and ip ban

        BanEntry foundEntry = nameBanMap.get(entry.getName());

        if(foundEntry != null)
        {
            if(foundEntry.hasIp())
            {
                entriesToRemove.add(entry);
                return false;
            }
            else
            {
                entriesToRemove.add(foundEntry);
                addToMemory(entry);
                return true;
            }
        }

        addToMemory(entry);
        return true;
    }
}
