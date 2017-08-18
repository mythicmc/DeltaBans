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
package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.*;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaBansPlugin extends JavaPlugin
{
  private boolean inDebugMode;
  private String dbShareDataSourceName;
  private String accountsTableName;
  private String ipColumnName;
  private String playerColumnName;
  private String silentAnnouncementPrefix;
  private MessageFormatMap messageFormatMap;

  private BanCommand banCommand;
  private BannedCommand bannedCommand;
  private KickCommand kickCommand;
  private RangeBanCommand rangeBanCommand;
  private RangeUnbanCommand rangeUnbanCommand;
  private RangeBanWhitelistCommand rangeBanWhitelistCommand;
  private TempBanCommand tempBanCommand;
  private UnbanCommand unbanCommand;
  private UnwarnCommand unwarnCommand;
  private WarnCommand warnCommand;
  private WarningsCommand warningsCommand;
  private WhitelistCommand whitelistCommand;
  private AnnouncementListener announcementListener;

  @Override
  public void onEnable()
  {
    saveDefaultConfig();
    reloadConfig();
    readConfiguration(getConfig());

    SockExchangeApi api = SockExchangeApi.instance();

    banCommand = new BanCommand(this, api, messageFormatMap);
    banCommand.register();

    bannedCommand = new BannedCommand(this, api, messageFormatMap);
    bannedCommand.register();

    kickCommand = new KickCommand(this, api, messageFormatMap);
    kickCommand.register();

    rangeBanCommand = new RangeBanCommand(this, api, messageFormatMap);
    rangeBanCommand.register();

    rangeUnbanCommand = new RangeUnbanCommand(this, api, messageFormatMap);
    rangeUnbanCommand.register();

    rangeBanWhitelistCommand = new RangeBanWhitelistCommand(this, api, messageFormatMap);
    rangeBanWhitelistCommand.register();

    tempBanCommand = new TempBanCommand(this, api, messageFormatMap);
    tempBanCommand.register();

    unbanCommand = new UnbanCommand(this, api, messageFormatMap);
    unbanCommand.register();

    unwarnCommand = new UnwarnCommand(this, api, messageFormatMap);
    unwarnCommand.register();

    warnCommand = new WarnCommand(this, api, messageFormatMap);
    warnCommand.register();

    warningsCommand = new WarningsCommand(this, api, messageFormatMap);
    warningsCommand.register();

    whitelistCommand = new WhitelistCommand(this, api, messageFormatMap);
    whitelistCommand.register();

    announcementListener = new AnnouncementListener(this, api, silentAnnouncementPrefix);
    announcementListener.register();
  }

  @Override
  public void onDisable()
  {
    if (announcementListener != null)
    {
      announcementListener.unregister();
      announcementListener = null;
    }

    if (whitelistCommand != null)
    {
      whitelistCommand.unregister();
      whitelistCommand = null;
    }

    if (warningsCommand != null)
    {
      warningsCommand.unregister();
      warningsCommand = null;
    }

    if (warnCommand != null)
    {
      warnCommand.unregister();
      warnCommand = null;
    }

    if (unwarnCommand != null)
    {
      unwarnCommand.unregister();
      unwarnCommand = null;
    }

    if (unbanCommand != null)
    {
      unbanCommand.unregister();
      unbanCommand = null;
    }

    if (tempBanCommand != null)
    {
      tempBanCommand.unregister();
      tempBanCommand = null;
    }

    if (rangeBanCommand != null)
    {
      rangeUnbanCommand.unregister();
      rangeUnbanCommand = null;
    }

    if (rangeBanWhitelistCommand != null)
    {
      rangeBanWhitelistCommand.unregister();
      rangeBanWhitelistCommand = null;
    }

    if (rangeBanCommand != null)
    {
      rangeBanCommand.unregister();
      rangeBanCommand = null;
    }

    if (kickCommand != null)
    {
      kickCommand.unregister();
      kickCommand = null;
    }

    if (bannedCommand != null)
    {
      bannedCommand.unregister();
      bannedCommand = null;
    }

    if (banCommand != null)
    {
      banCommand.unregister();
      banCommand = null;
    }
  }

  public Connection getConnection() throws SQLException
  {
    return DbShare.instance().getDataSource(dbShareDataSourceName).getConnection();
  }

  public String getIpOfPlayer(String playerName)
  {
    String ipLookupQuery = "SELECT `" + ipColumnName + "` " + "FROM `" + accountsTableName + "` " + "WHERE `" + playerColumnName + "` = ?;";

    try (Connection connection = getConnection())
    {
      try (PreparedStatement statement = connection.prepareStatement(ipLookupQuery))
      {
        statement.setString(1, playerName);

        try (ResultSet resultSet = statement.executeQuery())
        {
          return (resultSet.next()) ? resultSet.getString(ipColumnName) : "";
        }
      }
    }
    catch (SQLException ex)
    {
      ex.printStackTrace();
      return "";
    }
  }

  public void executeSync(Runnable runnable)
  {
    getServer().getScheduler().runTask(this, runnable);
  }

  private void readConfiguration(ConfigurationSection config)
  {
    inDebugMode = config.getBoolean("DebugMode", false);
    dbShareDataSourceName = config.getString("DbShareDataSourceName", "<missing>");
    accountsTableName = config.getString("IpLookup.Table");
    ipColumnName = config.getString("IpLookup.IpColumn");
    playerColumnName = config.getString("IpLookup.PlayerColumn");
    silentAnnouncementPrefix = ChatColor.translateAlternateColorCodes(
      '&', config.getString("SilentAnnouncementPrefix"));
    messageFormatMap = new MessageFormatMap();

    ConfigurationSection section = config.getConfigurationSection("Formats");
    for (String formatKey : section.getKeys(false))
    {
      String format = section.getString(formatKey);
      String translated = ChatColor.translateAlternateColorCodes('&', format);
      messageFormatMap.put(formatKey, translated);
    }
  }
}
