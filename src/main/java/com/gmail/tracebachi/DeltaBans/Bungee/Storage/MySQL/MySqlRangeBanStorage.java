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

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPluginInterface;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class MySqlRangeBanStorage implements RangeBanStorage
{
  private final DeltaBansPluginInterface plugin;
  private final BasicLogger logger;
  private final List<RangeBanEntry> rangeBanList = new ArrayList<>();

  public MySqlRangeBanStorage(DeltaBansPluginInterface plugin, BasicLogger logger)
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
        MySqlQueries.SELECT_ALL_RANGEBANS_STATEMENT))
      {
        try (ResultSet resultSet = statement.executeQuery())
        {
          while (resultSet.next())
          {
            RangeBanEntry rangeBanEntry = readRangeBanEntry(resultSet);
            if (rangeBanEntry != null)
            {
              rangeBanList.add(rangeBanEntry);
            }
          }
        }
      }
    }

    // Log
    logger.info("Loaded %s range bans", rangeBanList.size());
  }

  @Override
  public synchronized void save()
  {

  }

  @Override
  public synchronized RangeBanEntry getIpRangeBan(String ip)
  {
    return getIpRangeBan(DeltaBansUtils.convertIpToLong(ip));
  }

  @Override
  public synchronized RangeBanEntry getIpRangeBan(long ipAsLong)
  {
    for (RangeBanEntry entry : rangeBanList)
    {
      if (ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
      {
        return entry;
      }
    }
    return null;
  }

  @Override
  public synchronized void add(RangeBanEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    rangeBanList.add(entry);
    addToDatabase(entry);
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

    while (iterator.hasNext())
    {
      RangeBanEntry entry = iterator.next();

      if (ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
      {
        iterator.remove();
        removeFromDatabase(entry);
        count++;
      }
    }

    return count;
  }

  private void addToDatabase(RangeBanEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.ADD_RANGEBAN_ENTRY_STATEMENT))
        {
          statement.setString(1, entry.getStartAddress());
          statement.setString(2, entry.getEndAddress());
          statement.setString(3, entry.getBanner());
          statement.setString(4, entry.getMessage());
          statement.setTimestamp(5, new Timestamp(entry.getCreatedAt()));

          statement.execute();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private void removeFromDatabase(RangeBanEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.DELETE_RANGEBAN_ENTRY_STATEMENT))
        {
          statement.setString(1, entry.getStartAddress());
          statement.setString(2, entry.getEndAddress());
          statement.setString(3, entry.getBanner());
          statement.setString(4, entry.getMessage());

          statement.executeUpdate();
        }
      }
      catch (SQLException e)
      {
        e.printStackTrace();
      }
    });
  }

  private RangeBanEntry readRangeBanEntry(ResultSet resultSet)
  {
    try
    {
      String banner = resultSet.getString("banner");
      String message = resultSet.getString("message");
      String ipStart = resultSet.getString("ip_start");
      String ipEnd = resultSet.getString("ip_end");
      long createdAt = resultSet.getTimestamp("createdAt").getTime();
      return new RangeBanEntry(banner, message, ipStart, ipEnd, createdAt);
    }
    catch (SQLException | NullPointerException e)
    {
      e.printStackTrace();
      return null;
    }
  }
}
