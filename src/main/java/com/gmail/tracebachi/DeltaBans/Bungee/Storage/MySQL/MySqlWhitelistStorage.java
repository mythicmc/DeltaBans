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
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class MySqlWhitelistStorage implements WhitelistStorage
{
  private final int NORMAL_WHITELIST = 0b01;
  private final int RANGEBAN_WHITELIST = 0b10;

  private final DeltaBansPlugin plugin;
  private final BasicLogger logger;
  private final Map<String, Integer> whitelistMap = new HashMap<>();

  public MySqlWhitelistStorage(DeltaBansPlugin plugin, BasicLogger logger)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(logger, "logger");

    this.plugin = plugin;
    this.logger = logger;
  }

  @Override
  public synchronized void load() throws SQLException
  {
    try (Connection connection = plugin.getConnection())
    {
      try (PreparedStatement statement = connection.prepareStatement(
        MySqlQueries.SELECT_ALL_WHITELIST_STATEMENT))
      {
        try (ResultSet resultSet = statement.executeQuery())
        {
          while (resultSet.next())
          {
            try
            {
              String name = resultSet.getString("name").toLowerCase();
              int type = resultSet.getInt("type");

              whitelistMap.put(name, type);
            }
            catch (SQLException e)
            {
              e.printStackTrace();
            }
          }
        }
      }
    }

    // Log
    logger.info("Loaded normal and rangeban whitelists for %s players", whitelistMap.size());
  }

  @Override
  public synchronized void save()
  {

  }

  @Override
  public synchronized boolean isOnNormalWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    return (whitelistMap.getOrDefault(name, 0) & NORMAL_WHITELIST) != 0;
  }

  @Override
  public synchronized boolean isOnRangeBanWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    return (whitelistMap.getOrDefault(name, 0) & RANGEBAN_WHITELIST) != 0;
  }

  @Override
  public synchronized boolean addToNormalWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    Integer stored = whitelistMap.get(name);

    if (stored != null)
    {
      if ((stored & NORMAL_WHITELIST) != 0)
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
  public boolean addToRangeBanWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    Integer stored = whitelistMap.get(name);

    if (stored != null)
    {
      if ((stored & RANGEBAN_WHITELIST) != 0)
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
  public synchronized boolean removeFromNormalWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    Integer stored = whitelistMap.get(name);

    if (stored == null)
    {
      return false;
    }

    // Clear the flag
    Integer newFlags = stored & ~NORMAL_WHITELIST;

    if (stored.equals(newFlags))
    {
      return false;
    }
    else if (newFlags == 0)
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
  public synchronized boolean removeFromRangeBanWhitelist(String name)
  {
    Preconditions.checkNotNull(name, "name");
    name = name.toLowerCase();

    Integer stored = whitelistMap.get(name);

    if (stored == null)
    {
      return false;
    }

    // Clear the flag
    Integer newFlags = stored & ~RANGEBAN_WHITELIST;

    if (stored.equals(newFlags))
    {
      return false;
    }
    else if (newFlags == 0)
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

  private void addToDatabase(String name, Integer type)
  {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(type, "type");

    String finalName = name.toLowerCase();

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.ADD_TO_WHITELIST_STATEMENT))
        {
          statement.setString(1, finalName);
          statement.setInt(2, type);

          statement.execute();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private void removeFromDatabase(String name)
  {
    Preconditions.checkNotNull(name, "name");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.DELETE_FROM_WHITELIST_STATEMENT))
        {
          statement.setString(1, name);

          statement.executeUpdate();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private void updateInDatabase(String name, Integer type)
  {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(type, "type");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.UPDATE_IN_WHITELIST_STATEMENT))
        {
          statement.setInt(1, type);
          statement.setString(2, name);

          statement.executeUpdate();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }
}
