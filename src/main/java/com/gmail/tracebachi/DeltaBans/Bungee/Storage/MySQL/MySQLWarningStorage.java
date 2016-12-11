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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class MySQLWarningStorage implements WarningStorage
{
    private final String SELECT_ALL_WARNINGS_STATEMENT =
        "SELECT * FROM deltabans_warnings;";
    private final String ADD_WARNING_STATEMENT =
        " INSERT INTO deltabans_warnings" +
        " (name, warner, message, createdAt)" +
        " VALUES (?,?,?,?);";
    private final String DELETE_WARNING_STATEMENT =
        " DELETE FROM deltabans_warnings" +
        " WHERE name = ? AND warner = ? AND message = ?" +
        " LIMIT 1;";

    private long warningDuration;
    private HashMap<String, List<WarningEntry>> warningsMap = new HashMap<>();
    private DeltaBans plugin;

    public MySQLWarningStorage(DeltaBans plugin)
    {
        this.plugin = plugin;
        this.warningDuration = Settings.getWarningDurationInMinutes() * 60 * 1000;
    }

    @Override
    public void load() throws SQLException
    {
        try(Connection connection = Settings.getDataSource().getConnection())
        {
            List<WarningEntry> entriesToRemove = new ArrayList<>(64);

            try(PreparedStatement statement = connection.prepareStatement(
                SELECT_ALL_WARNINGS_STATEMENT))
            {
                try(ResultSet resultSet = statement.executeQuery())
                {
                    while(resultSet.next())
                    {
                        loadFromResultSet(resultSet, entriesToRemove);
                    }
                }
            }

            try(PreparedStatement statement = connection.prepareStatement(
                DELETE_WARNING_STATEMENT))
            {
                for(WarningEntry entry : entriesToRemove)
                {
                    try
                    {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getWarner());
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
    public void shutdown()
    {
        plugin = null;
    }

    @Override
    public synchronized int getTotalWarningCount()
    {
        int count = 0;

        for(List<WarningEntry> list : warningsMap.values())
        {
            count += list.size();
        }

        return count;
    }

    @Override
    public synchronized int addWarning(WarningEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        String name = entry.getName();
        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings == null)
        {
            warnings = new ArrayList<>();
            warningsMap.put(name, warnings);
        }

        warnings.add(entry);
        addToDatabase(entry);

        return warnings.size();
    }

    @Override
    public synchronized int removeWarning(String name, int amount)
    {
        name = Preconditions.checkNotNull(name, "Name was null.").toLowerCase();

        int count = 0;
        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings != null)
        {
            for(int i = amount; i > 0 && warnings.size() > 0; i--)
            {
                WarningEntry warningEntry = warnings.get(warnings.size() - 1);
                warnings.remove(warnings.size() - 1);
                removeFromDatabase(warningEntry);

                count++;
            }

            if(warnings.isEmpty())
            {
                warningsMap.remove(name);
            }
        }

        return count;
    }

    @Override
    public synchronized List<WarningEntry> getWarnings(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        name = name.toLowerCase();

        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings == null)
        {
            return Collections.emptyList();
        }
        else if(warnings.size() == 0)
        {
            warningsMap.remove(name);
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableList(warnings);
        }
    }

    @Override
    public void removeExpired()
    {
        Iterator<Map.Entry<String, List<WarningEntry>>> iterator = warningsMap.entrySet().iterator();
        long oldestTime = System.currentTimeMillis() - warningDuration;

        while(iterator.hasNext())
        {
            Map.Entry<String, List<WarningEntry>> entry = iterator.next();
            ListIterator<WarningEntry> listIterator = entry.getValue().listIterator();

            while(listIterator.hasNext())
            {
                WarningEntry warningEntry = listIterator.next();

                if(warningEntry.getCreatedAt() < oldestTime)
                {
                    listIterator.remove();
                    removeFromDatabase(warningEntry);
                }
            }

            if(entry.getValue().size() == 0)
            {
                iterator.remove();
            }
        }
    }

    private synchronized void addToDatabase(WarningEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    ADD_WARNING_STATEMENT))
                {
                    statement.setString(1, entry.getName());
                    statement.setString(2, entry.getWarner());
                    statement.setString(3, entry.getMessage());
                    statement.setTimestamp(4, new Timestamp(entry.getCreatedAt()));

                    if(statement.execute())
                    {
                        plugin.debug("Inserted warning into database.");
                    }
                    else
                    {
                        plugin.debug("Failed to insert warning into database. " +
                            "Statement#execute() returned false.");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to insert warning into database.");
                e.printStackTrace();
            }
        });
    }

    private synchronized void removeFromDatabase(WarningEntry entry)
    {
        Preconditions.checkNotNull(entry, "Entry was null.");

        plugin.getProxy().getScheduler().runAsync(plugin, () ->
        {
            try(Connection connection = Settings.getDataSourceConnection())
            {
                try(PreparedStatement statement = connection.prepareStatement(
                    DELETE_WARNING_STATEMENT))
                {
                    statement.setString(1, entry.getName());
                    statement.setString(2, entry.getWarner());
                    statement.setString(3, entry.getMessage());

                    int count = statement.executeUpdate();
                    if(count == 1)
                    {
                        plugin.debug("Deleted warning from database.");
                    }
                    else
                    {
                        plugin.debug("Failed to delete warning from database." +
                            "Statement#executeUpdate() returned " + count + ".");
                    }
                }
            }
            catch(SQLException e)
            {
                plugin.debug("Failed to delete warning from database.");
                e.printStackTrace();
            }
        });
    }

    private void loadFromResultSet(ResultSet resultSet, List<WarningEntry> entriesToRemove)
    {
        WarningEntry entry;

        try
        {
            entry = new WarningEntry(
                resultSet.getString("name"),
                resultSet.getString("warner"),
                resultSet.getString("message"),
                resultSet.getTimestamp("createdAt").getTime());
        }
        catch(SQLException | NullPointerException e)
        {
            e.printStackTrace();
            return;
        }

        if(System.currentTimeMillis() - entry.getCreatedAt() >= warningDuration)
        {
            entriesToRemove.add(entry);
            return;
        }

        List<WarningEntry> warningEntries = warningsMap.get(entry.getName());

        if(warningEntries == null)
        {
            warningEntries = new ArrayList<>();
            warningsMap.put(entry.getName(), warningEntries);
        }

        warningEntries.add(entry);
    }
}
