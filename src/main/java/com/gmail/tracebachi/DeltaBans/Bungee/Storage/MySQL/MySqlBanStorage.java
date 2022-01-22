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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;

import java.sql.*;
import java.util.*;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class MySqlBanStorage implements BanStorage
{
  private final DeltaBansPluginInterface plugin;
  private final Set<BanEntry> banSet = new HashSet<>();
  private final Map<String, List<BanEntry>> nameBanMap = new HashMap<>();
  private final Map<String, List<BanEntry>> ipBanMap = new HashMap<>();
  private final BasicLogger logger;

  public MySqlBanStorage(DeltaBansPluginInterface plugin, BasicLogger logger)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(logger, "logger");

    this.plugin = plugin;
    this.logger = logger;
  }

  @Override
  public synchronized void load() throws SQLException
  {
    // Clear the existing storage
    banSet.clear();
    nameBanMap.clear();
    ipBanMap.clear();

    try (Connection connection = plugin.getConnection())
    {
      try (PreparedStatement statement = connection.prepareStatement(
        MySqlQueries.SELECT_ALL_BANS_STATEMENT))
      {
        try (ResultSet resultSet = statement.executeQuery())
        {
          // Read all ban entries from the database
          while (resultSet.next())
          {
            BanEntry banEntry = readBanEntry(resultSet);
            if (banEntry != null)
            {
              // Add to memory
              addToMemory(banEntry);
            }
          }
        }
      }
    }

    // Remove expired and duplicate bans
    removeExpiredBans();
    removeDuplicateBans();

    // Log
    logger.info(
      "Loaded %s bans. %s bans have a name. %s bans have an IP.", banSet.size(), nameBanMap.size(),
      ipBanMap.size());
  }

  @Override
  public synchronized void save()
  {

  }

  @Override
  public synchronized BanEntry getBanEntry(String name, String ip)
  {
    Preconditions.checkNotNull(name != null || ip != null, "name and ip both null");

    List<BanEntry> banEntryList;
    BanEntry foundEntry = null;

    if (name != null)
    {
      name = name.toLowerCase();
      banEntryList = nameBanMap.get(name);

      if (banEntryList != null)
      {
        if (banEntryList.isEmpty())
        {
          // Remove the list if it is empty
          nameBanMap.remove(name);
        }
        else
        {
          List<BanEntry> entriesToRemove = new ArrayList<>(2);

          // Iterate through the list to find the first non-expired ban
          for (BanEntry b : banEntryList)
          {
            if (b.isDurationComplete())
            {
              entriesToRemove.add(b);
            }
            else if (foundEntry == null)
            {
              // A valid ban has been found
              foundEntry = b;
            }
          }

          // Remove all the entries that were seen as expired
          for (BanEntry b : entriesToRemove)
          {
            removeFromMemory(b);
            removeFromDatabase(b);
          }
        }
      }
    }

    // If a ban was found, there is no need to look up by ip.
    if (foundEntry != null)
    {
      return foundEntry;
    }

    if (ip != null)
    {
      banEntryList = ipBanMap.get(ip);

      if (banEntryList != null)
      {
        if (banEntryList.isEmpty())
        {
          // Remove the list if it is empty
          ipBanMap.remove(ip);
        }
        else
        {
          List<BanEntry> entriesToRemove = new ArrayList<>(2);

          // Iterate through the list to find the first non-expired ban
          for (BanEntry b : banEntryList)
          {
            if (b.isDurationComplete())
            {
              entriesToRemove.add(b);
            }
            else if (foundEntry == null)
            {
              // A valid ban has been found
              foundEntry = b;
            }
          }

          // Remove all the entries that were seen as expired
          for (BanEntry b : entriesToRemove)
          {
            removeFromMemory(b);
            removeFromDatabase(b);
          }
        }
      }
    }

    return foundEntry;
  }

  @Override
  public synchronized AddResult addBanEntry(BanEntry entry)
  {
    Preconditions.checkNotNull(entry, "entry");

    boolean hasName = entry.hasName();
    boolean hasIp = entry.hasIp();
    String name = entry.getName();
    String ip = entry.getIp();

    // If the entry is a name-only ban
    if (hasName && !hasIp)
    {
      List<BanEntry> banEntryList = nameBanMap.get(name);
      if (banEntryList != null && !banEntryList.isEmpty())
      {
        return AddResult.EXISTING_NAME_BAN;
      }

      addToMemory(entry);
      addToDatabase(entry);
      return AddResult.SUCCESS;
    }

    // If the entry is an ip-only ban
    if (!hasName && hasIp)
    {
      List<BanEntry> banEntryList = ipBanMap.get(ip);
      if (banEntryList != null && !banEntryList.isEmpty())
      {
        return AddResult.EXISTING_IP_BAN;
      }

      addToMemory(entry);
      addToDatabase(entry);
      return AddResult.SUCCESS;
    }

    // The entry is a name+ip ban
    List<BanEntry> banEntryList;

    // Check if there is an existing name+ip ban in the ban entry list for the same name
    banEntryList = nameBanMap.get(name);
    if (banEntryList != null)
    {
      if (banEntryList.isEmpty())
      {
        // Remove the list if it is empty
        nameBanMap.remove(name);
      }
      else
      {
        for (BanEntry b : banEntryList)
        {
          if (b.hasName() && b.hasIp() && b.getName().equalsIgnoreCase(name))
          {
            return AddResult.EXISTING_NAME_AND_IP_BAN;
          }
        }
      }
    }

    // Check if there is an existing name+ip ban in the ban entry list for the same ip
    banEntryList = ipBanMap.get(ip);
    if (banEntryList != null)
    {
      if (banEntryList.isEmpty())
      {
        // Remove the list if it is empty
        ipBanMap.remove(ip);
      }
      else
      {
        for (BanEntry b : banEntryList)
        {
          if (b.hasName() && b.hasIp() && b.getIp().equals(ip))
          {
            return AddResult.EXISTING_NAME_AND_IP_BAN;
          }
        }
      }
    }

    // Add this new entry
    addToMemory(entry);
    addToDatabase(entry);
    return AddResult.SUCCESS;
  }

  @Override
  public synchronized List<BanEntry> removeUsingIp(String ip)
  {
    Preconditions.checkNotNull(ip, "ip");

    List<BanEntry> banEntryList = ipBanMap.remove(ip);

    if (banEntryList == null)
    {
      return Collections.emptyList();
    }

    for (BanEntry entry : banEntryList)
    {
      // This call to removeFromMemory is safe from ConcurrentModificationException
      // because the ban entry list was removed from the ip ban map.
      removeFromMemory(entry);
      removeFromDatabase(entry);

      // Unbanning IPs should keep the names banned
      if (entry.hasName())
      {
        BanEntry newEntry = new BanEntry(entry.getName(), null, entry.getBanner(),
          entry.getMessage(), entry.getDuration(), entry.getCreatedAt());

        addToMemory(newEntry);
        addToDatabase(newEntry);
      }
    }

    // Return the actual list since we're not going to use it anymore
    return banEntryList;
  }

  @Override
  public synchronized List<BanEntry> removeUsingName(String name)
  {
    name = Preconditions.checkNotNull(name, "name").toLowerCase();

    List<BanEntry> banEntryList = nameBanMap.remove(name);

    if (banEntryList == null)
    {
      return Collections.emptyList();
    }

    for (BanEntry entry : banEntryList)
    {
      // This call to removeFromMemory is safe from ConcurrentModificationException
      // because the ban entry list was removed from the ip ban map.
      removeFromMemory(entry);
      removeFromDatabase(entry);
    }

    // Return the actual list since we're not going to use it anymore
    return banEntryList;
  }

  private void addToMemory(BanEntry entry)
  {
    banSet.add(entry);

    if (entry.hasName())
    {
      String name = entry.getName();
      nameBanMap.computeIfAbsent(name, k -> new ArrayList<>()).add(entry);
    }

    if (entry.hasIp())
    {
      String ip = entry.getIp();
      ipBanMap.computeIfAbsent(ip, k -> new ArrayList<>()).add(entry);
    }
  }

  private void removeFromMemory(BanEntry entry)
  {
    // Comparison is based on reference, which is the desired behavior
    banSet.remove(entry);

    if (entry.hasName())
    {
      String name = entry.getName();
      List<BanEntry> banEntryList = nameBanMap.get(name);

      if (banEntryList != null)
      {
        // Comparison is based on reference, which is the desired behavior
        banEntryList.remove(entry);

        // Remove the list if it is now empty
        if (banEntryList.isEmpty())
        {
          nameBanMap.remove(name);
        }
      }
    }

    if (entry.hasIp())
    {
      String ip = entry.getIp();
      List<BanEntry> banEntryList = ipBanMap.get(ip);

      if (banEntryList != null)
      {
        // Comparison is based on reference, which is the desired behavior
        banEntryList.remove(entry);

        // Remove the list if it is now empty
        if (banEntryList.isEmpty())
        {
          ipBanMap.remove(ip);
        }
      }
    }
  }

  private void addToDatabase(BanEntry entry)
  {
    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        try (PreparedStatement statement = connection.prepareStatement(
          MySqlQueries.ADD_BAN_ENTRY_STATEMENT))
        {
          statement.setString(1, entry.getName());
          statement.setString(2, entry.getIp());
          statement.setString(3, entry.getBanner());
          statement.setString(4, entry.getMessage());

          if (entry.getDuration() == null)
          {
            statement.setNull(5, Types.BIGINT);
          }
          else
          {
            statement.setLong(5, entry.getDuration());
          }

          statement.setTimestamp(6, new Timestamp(entry.getCreatedAt()));

          statement.execute();
        }
      }
      catch (SQLException ex)
      {
        ex.printStackTrace();
      }
    });
  }

  private void removeFromDatabase(BanEntry entry)
  {
    plugin.getExecutor().execute(() ->
    {
      try (Connection connection = plugin.getConnection())
      {
        if (entry.hasName() && entry.hasIp())
        {
          try (PreparedStatement statement = connection.prepareStatement(
            MySqlQueries.DELETE_BAN_ENTRY_NO_NULL_STATEMENT))
          {
            statement.setString(1, entry.getName());
            statement.setString(2, entry.getIp());
            statement.setString(3, entry.getBanner());
            statement.setString(4, entry.getMessage());

            statement.executeUpdate();
          }
        }
        else if (entry.hasName() && !entry.hasIp())
        {
          try (PreparedStatement statement = connection.prepareStatement(
            MySqlQueries.DELETE_BAN_ENTRY_IP_NULL_STATEMENT))
          {
            statement.setString(1, entry.getName());
            statement.setString(2, entry.getBanner());
            statement.setString(3, entry.getMessage());

            statement.executeUpdate();
          }
        }
        else if (!entry.hasName() && entry.hasIp())
        {
          try (PreparedStatement statement = connection.prepareStatement(
            MySqlQueries.DELETE_BAN_ENTRY_NAME_NULL_STATEMENT))
          {
            statement.setString(1, entry.getIp());
            statement.setString(2, entry.getBanner());
            statement.setString(3, entry.getMessage());

            statement.executeUpdate();
          }
        }
      }
      catch (SQLException ex)
      {
        ex.printStackTrace();
      }
    });
  }

  private BanEntry readBanEntry(ResultSet resultSet)
  {
    try
    {
      String name = resultSet.getString("name");
      String ip = resultSet.getString("ip");
      String banner = resultSet.getString("banner");
      String message = resultSet.getString("message");
      Long duration = resultSet.getLong("duration");
      Long createdAt = resultSet.getTimestamp("createdAt").getTime();

      return new BanEntry(name, ip, banner, message, duration, createdAt);
    }
    catch (SQLException | IllegalArgumentException e)
    {
      e.printStackTrace();
      return null;
    }
  }

  private void removeExpiredBans()
  {
    List<BanEntry> entriesToRemove = new ArrayList<>(16);

    for (BanEntry b : banSet)
    {
      if (b.isDurationComplete())
      {
        entriesToRemove.add(b);
      }
    }

    for (BanEntry b : entriesToRemove)
    {
      removeFromMemory(b);
      removeFromDatabase(b);
    }
  }

  private void removeDuplicateBans()
  {
    List<BanEntry> entriesToRemove = new ArrayList<>(16);

    for (BanEntry ban : banSet)
    {
      List<BanEntry> list;
      String banName = ban.getName();
      String banIp = ban.getIp();

      list = nameBanMap.get(banName);
      list = list == null ? Collections.emptyList() : list;

      for (BanEntry banToCheck : list)
      {
        if (banToCheck == ban)
        {
          continue;
        }

        if (Objects.equals(banToCheck.getName(), banName) && Objects.equals(banToCheck.getIp(), banIp))
        {
          entriesToRemove.add(banToCheck);
        }
      }

      list = ipBanMap.get(banIp);
      list = list == null ? Collections.emptyList() : list;

      for (BanEntry banToCheck : list)
      {
        if (banToCheck == ban)
        {
          continue;
        }

        if (Objects.equals(banToCheck.getName(), banName) && Objects.equals(banToCheck.getIp(), banIp))
        {
          entriesToRemove.add(banToCheck);
        }
      }
    }

    for (BanEntry b : entriesToRemove)
    {
      removeFromMemory(b);
      removeFromDatabase(b);
    }
  }
}
