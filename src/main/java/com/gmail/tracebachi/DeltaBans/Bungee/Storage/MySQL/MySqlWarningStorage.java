/*
 * DeltaBans - Ban and warning plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.*;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class MySqlWarningStorage implements WarningStorage
{
  private final DeltaBansPlugin plugin;
  private final BasicLogger logger;
  private final long warningDuration;
  private final HashMap<String, List<WarningEntry>> warningsMap = new HashMap<>();

  public MySqlWarningStorage(
    DeltaBansPlugin plugin, BasicLogger logger, long warningDuration)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(logger, "logger");

    this.plugin = plugin;
    this.logger = logger;
    this.warningDuration = warningDuration;
  }

  @Override
  public synchronized void load() throws SQLException
  {
    List<WarningEntry> entriesToRemove = new ArrayList<>(16);

    try (Connection connection = plugin.getConnection())
    {
      try (PreparedStatement statement = connection.prepareStatement(
        MySqlQueries.SELECT_ALL_WARNINGS_STATEMENT))
      {
        try (ResultSet resultSet = statement.executeQuery())
        {
          while (resultSet.next())
          {
            WarningEntry warningEntry = readWarningEntry(resultSet);
            if (warningEntry != null)
            {
              // Add entry to the list of entries to remove if it has expired
              if (System.currentTimeMillis() - warningEntry.getCreatedAt() >= warningDuration)
              {
                entriesToRemove.add(warningEntry);
                continue;
              }

              String name = warningEntry.getName();
              List<WarningEntry> warningEntries = warningsMap.computeIfAbsent(name, k -> new ArrayList<>());

              // Add the entry
              warningEntries.add(warningEntry);
            }
          }
        }
      }
    }

    // Remove the entries that need to be removed
    for (WarningEntry warningEntry : entriesToRemove)
    {
      removeFromDatabase(warningEntry);
    }

    // Log
    logger.info("Loaded warnings for %s players", warningsMap.size());
  }

  @Override
  public synchronized void save()
  {

  }

  @Override
  public synchronized int addWarning(WarningEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    String name = entry.getName();
    List<WarningEntry> warnings = warningsMap.computeIfAbsent(name, k -> new ArrayList<>());

    warnings.add(entry);
    addToDatabase(entry);

    return warnings.size();
  }

  @Override
  public synchronized int removeWarnings(String name, int amount)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    int count = 0;
    List<WarningEntry> warnings = warningsMap.get(name);

    if (warnings != null)
    {
      amount = Math.min(amount, warnings.size());

      for (int i = amount; i > 0; i--)
      {
        WarningEntry warningEntry = warnings.remove(i - 1);

        removeFromDatabase(warningEntry);
        count++;
      }

      if (warnings.isEmpty())
      {
        warningsMap.remove(name);
      }
    }

    return count;
  }

  @Override
  public synchronized List<WarningEntry> getWarnings(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    List<WarningEntry> warnings = warningsMap.get(name);

    if (warnings == null)
    {
      return Collections.emptyList();
    }
    else if (warnings.size() == 0)
    {
      // Remove the list if it is empty
      warningsMap.remove(name);

      return Collections.emptyList();
    }
    else
    {
      return Collections.unmodifiableList(warnings);
    }
  }

  @Override
  public synchronized void removeExpiredWarnings()
  {
    Iterator<Map.Entry<String, List<WarningEntry>>> iterator = warningsMap.entrySet().iterator();
    long oldestTime = System.currentTimeMillis() - warningDuration;

    while (iterator.hasNext())
    {
      Map.Entry<String, List<WarningEntry>> entry = iterator.next();
      ListIterator<WarningEntry> listIterator = entry.getValue().listIterator();

      while (listIterator.hasNext())
      {
        WarningEntry warningEntry = listIterator.next();

        if (warningEntry.getCreatedAt() < oldestTime)
        {
          listIterator.remove();
          removeFromDatabase(warningEntry);
        }
      }

      if (entry.getValue().size() == 0)
      {
        iterator.remove();
      }
    }
  }

  private void addToDatabase(WarningEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.ADD_WARNING_STATEMENT))
        {
          statement.setString(1, entry.getName());
          statement.setString(2, entry.getWarner());
          statement.setString(3, entry.getMessage());
          statement.setTimestamp(4, new Timestamp(entry.getCreatedAt()));

          statement.execute();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private void removeFromDatabase(WarningEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.DELETE_WARNING_STATEMENT))
        {
          statement.setString(1, entry.getName());
          statement.setString(2, entry.getWarner());
          statement.setString(3, entry.getMessage());

          statement.executeUpdate();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private WarningEntry readWarningEntry(ResultSet resultSet)
  {
    try
    {
      String name = resultSet.getString("name");
      String warner = resultSet.getString("warner");
      String message = resultSet.getString("message");
      long createdAt = resultSet.getTimestamp("createdAt").getTime();

      return new WarningEntry(name, warner, message, createdAt);
    }
    catch (SQLException | NullPointerException e)
    {
      e.printStackTrace();
      return null;
    }
  }
}
