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
package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.*;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.*;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlRangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.BungeeResourceUtil;
import com.gmail.tracebachi.SockExchange.Utilities.JulBasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import io.github.kyzderp.bungeelogger.BungeeLog;
import io.github.kyzderp.bungeelogger.BungeeLoggerPlugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class DeltaBansPlugin extends Plugin
{
  private Configuration config;
  private boolean inDebugMode;
  private boolean startWithWhitelistEnabled;
  private int warningDurationInMinutes;
  private String dbShareDataSourceName;
  private MessageFormatMap messageFormatMap;
  private HashMap<Integer, List<String>> warningCommandsMap;
  private ScheduledFuture<?> cleanupExpiredWarningsFuture;

  private BasicLogger basicLogger;
  private BungeeLog bungeeLoggerPluginLogger;
  private BanListener banListener;
  private CheckBanListener checkBanListener;
  private CheckWarningsListener checkWarningsListener;
  private KickListener kickListener;
  private PlayerLoginListener playerLoginListener;
  private WarningListener warningListener;
  private WhitelistListener whitelistListener;

  private BanStorage banStorage;
  private WarningStorage warningStorage;
  private RangeBanStorage rangeBanStorage;
  private WhitelistStorage whitelistStorage;

  @Override
  public void onEnable()
  {
    setupLoggers();

    if (!reloadConfiguration())
    {
      return;
    }

    readConfiguration(config);
    ((JulBasicLogger) basicLogger).setDebugMode(inDebugMode);

    if (!createDatabaseTables())
    {
      return;
    }

    try
    {
      banStorage = new MySqlBanStorage(this, basicLogger);
      banStorage.load();

      warningStorage = new MySqlWarningStorage(
        this, basicLogger, warningDurationInMinutes * 60 * 1000);
      warningStorage.load();

      rangeBanStorage = new MySqlRangeBanStorage(this, basicLogger);
      rangeBanStorage.load();

      whitelistStorage = new MySqlWhitelistStorage(this, basicLogger);
      whitelistStorage.load();
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return;
    }

    SockExchangeApi api = SockExchangeApi.instance();

    banListener = new BanListener(
      this, banStorage, rangeBanStorage, api, messageFormatMap);
    banListener.register();

    checkBanListener = new CheckBanListener(banStorage, api, messageFormatMap);
    checkBanListener.register();

    checkWarningsListener = new CheckWarningsListener(warningStorage, api, messageFormatMap);
    checkWarningsListener.register();

    kickListener = new KickListener(this, api, messageFormatMap);
    kickListener.register();

    playerLoginListener = new PlayerLoginListener(
      this, banStorage, rangeBanStorage, whitelistStorage, messageFormatMap, basicLogger,
      bungeeLoggerPluginLogger);
    playerLoginListener.register();

    warningListener = new WarningListener(
      warningStorage, warningCommandsMap, api, messageFormatMap);
    warningListener.register();

    whitelistListener = new WhitelistListener(
      this, whitelistStorage, api, messageFormatMap, startWithWhitelistEnabled);
    whitelistListener.register();

    // Schedule clean up of expired warnings
    cleanupExpiredWarningsFuture = api.getScheduledExecutorService().scheduleAtFixedRate(
      warningStorage::removeExpiredWarnings, 1, 1, TimeUnit.HOURS);
  }

  @Override
  public void onDisable()
  {
    if (cleanupExpiredWarningsFuture != null)
    {
      cleanupExpiredWarningsFuture.cancel(false);
      cleanupExpiredWarningsFuture = null;
    }

    if (whitelistListener != null)
    {
      whitelistListener.unregister();
      whitelistListener = null;
    }

    if (warningListener != null)
    {
      warningListener.unregister();
      warningListener = null;
    }

    if (playerLoginListener != null)
    {
      playerLoginListener.unregister();
      playerLoginListener = null;
    }

    if (kickListener != null)
    {
      kickListener.unregister();
      kickListener = null;
    }

    if (checkWarningsListener != null)
    {
      checkWarningsListener.unregister();
      checkWarningsListener = null;
    }

    if (checkBanListener != null)
    {
      checkBanListener.unregister();
      checkBanListener = null;
    }

    if (banListener != null)
    {
      banListener.unregister();
      banListener = null;
    }

    if (banStorage != null)
    {
      saveLoadAndSaveable(banStorage);
      banStorage = null;
    }

    if (rangeBanStorage != null)
    {
      saveLoadAndSaveable(rangeBanStorage);
      rangeBanStorage = null;
    }

    if (warningStorage != null)
    {
      saveLoadAndSaveable(warningStorage);
      warningStorage = null;
    }

    if (whitelistStorage != null)
    {
      saveLoadAndSaveable(whitelistStorage);
      whitelistStorage = null;
    }
  }

  public Connection getConnection() throws SQLException
  {
    return DbShare.instance().getDataSource(dbShareDataSourceName).getConnection();
  }

  public Executor getExecutor()
  {
    return SockExchangeApi.instance().getScheduledExecutorService();
  }

  private void setupLoggers()
  {
    basicLogger = new JulBasicLogger(getLogger(), inDebugMode);
    bungeeLoggerPluginLogger = null;

    Plugin foundPlugin = getProxy().getPluginManager().getPlugin("BungeeLogger");

    if (foundPlugin != null)
    {
      BungeeLoggerPlugin bungeeLoggerPlugin = (BungeeLoggerPlugin) foundPlugin;
      bungeeLoggerPluginLogger = bungeeLoggerPlugin.createLogger(this);
    }
  }

  private boolean reloadConfiguration()
  {
    try
    {
      File file = BungeeResourceUtil.saveResource(this, "bungee-config.yml", "config.yml");
      config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

      if (config != null)
      {
        return true;
      }

      BungeeResourceUtil.saveResource(this, "bungee-config.yml", "config-example.yml", true);
      getLogger().severe("Invalid configuration file!");
      return false;
    }
    catch (IOException ex)
    {
      getLogger().severe("Failed to load configuration file.");
      ex.printStackTrace();
      return false;
    }
  }

  private void readConfiguration(Configuration config)
  {
    inDebugMode = config.getBoolean("DebugMode", false);
    dbShareDataSourceName = config.getString("DbShareDataSourceName", "<missing>");
    warningDurationInMinutes = config.getInt("WarningDurationInMinutes", 10080);
    startWithWhitelistEnabled = config.getBoolean("StartWithWhitelistEnabled", false);
    messageFormatMap = new MessageFormatMap();
    warningCommandsMap = new HashMap<>();

    Configuration section;

    section = config.getSection("Formats");
    for (String formatKey : section.getKeys())
    {
      String format = section.getString(formatKey);
      String translated = ChatColor.translateAlternateColorCodes('&', format);
      messageFormatMap.put(formatKey, translated);
    }

    section = config.getSection("WarningCommands");
    for (String key : section.getKeys())
    {
      try
      {
        int amountOfWarnings = Integer.parseInt(key);
        List<String> commandsToRun = section.getStringList(key);
        warningCommandsMap.put(amountOfWarnings, commandsToRun);
      }
      catch (NumberFormatException ex)
      {
        // Ignore keys that are not numbers
      }
    }
  }

  private boolean createDatabaseTables()
  {
    try (Connection connection = getConnection())
    {
      if (!executeQuery(MySqlQueries.CREATE_BAN_TABLE, connection))
      {
        return false;
      }
      if (!executeQuery(MySqlQueries.CREATE_WARNING_TABLE, connection))
      {
        return false;
      }
      if (!executeQuery(MySqlQueries.CREATE_RANGE_BAN_TABLE, connection))
      {
        return false;
      }
      if (!executeQuery(MySqlQueries.CREATE_WHITELIST_TABLE, connection))
      {
        return false;
      }
    }
    catch (SQLException ex)
    {
      ex.printStackTrace();
      return false;
    }

    return true;
  }

  private boolean executeQuery(String query, Connection connection) throws SQLException
  {
    try (Statement statement = connection.createStatement())
    {
      statement.execute(query);
      return true;
    }
    catch (SQLException ex)
    {
      getLogger().severe("Failed to execute query: " + query);
      ex.printStackTrace();
      return false;
    }
  }

  private void saveLoadAndSaveable(LoadAndSaveable loadAndSaveable)
  {
    try
    {
      loadAndSaveable.save();
    }
    catch (Exception ex)
    {
      ex.printStackTrace();
    }
  }
}
