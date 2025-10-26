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
package com.gmail.tracebachi.DeltaBans.Velocity;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.BuildMetadata;
import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPluginInterface;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.CheckBanListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.CheckWarningsListener;
import com.gmail.tracebachi.DeltaBans.Velocity.Listeners.WarningListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.*;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlRangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.MySqlQueries;
import com.gmail.tracebachi.DeltaBans.Velocity.Listeners.*;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.BungeeResourceUtil;
import com.gmail.tracebachi.SockExchange.Utilities.SlfBasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.github.kyzderp.bungeelogger.BungeeLog;
import io.github.kyzderp.bungeelogger.VelocityLoggerPlugin;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
@Plugin(id = "deltabans", name = "DeltaBans", version = BuildMetadata.VERSION,
        authors = {"GeeItsZee (tracebachi@gmail.com)"}, dependencies = {
        @Dependency(id = "dbshare"), @Dependency(id = "bungeelogger"), @Dependency(id = "sockexchange")})
public class DeltaBansPlugin implements DeltaBansPluginInterface
{
  private ConfigurationNode config;
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

  private final ProxyServer server;
  private final Logger logger;
  private final Path dataDirectory;

  @Inject
  public DeltaBansPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
    this.server = server;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  @Subscribe
  public void onProxyInitialization(ProxyInitializeEvent event)
  {
    setupLoggers();

    if (!reloadConfiguration())
    {
      return;
    }

    readConfiguration(config);
    ((SlfBasicLogger) basicLogger).setDebugMode(inDebugMode);

    if (!createDatabaseTables())
    {
      return;
    }

    try
    {
      banStorage = new MySqlBanStorage(this, basicLogger);
      banStorage.load();

      warningStorage = new MySqlWarningStorage(
              this, basicLogger, (long) warningDurationInMinutes * 60 * 1000);
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
            this, warningStorage, warningCommandsMap, api, messageFormatMap);
    warningListener.register();

    whitelistListener = new WhitelistListener(
            this, whitelistStorage, api, messageFormatMap, startWithWhitelistEnabled);
    whitelistListener.register();

    // Schedule clean up of expired warnings
    cleanupExpiredWarningsFuture = api.getScheduledExecutorService().scheduleAtFixedRate(
            warningStorage::removeExpiredWarnings, 1, 1, TimeUnit.HOURS);
  }

  @Subscribe
  public void onProxyShutdownEvent(ProxyShutdownEvent event)
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

  public ProxyServer getServer() {
    return server;
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
    basicLogger = new SlfBasicLogger(logger, inDebugMode);
    bungeeLoggerPluginLogger = null;

    Object foundPlugin = server.getPluginManager().getPlugin("BungeeLogger")
            .flatMap(PluginContainer::getInstance).orElse(null);

    if (foundPlugin != null)
    {
      VelocityLoggerPlugin bungeeLoggerPlugin = (VelocityLoggerPlugin) foundPlugin;
      bungeeLoggerPluginLogger = bungeeLoggerPlugin.createLogger(this, "DeltaBans");
    }
  }

  private boolean reloadConfiguration()
  {
    try
    {
      File file = BungeeResourceUtil.saveResource(
              s -> DeltaBansPlugin.this.getClass().getResourceAsStream(s), dataDirectory.toFile(),
              "bungee-config.yml", "config.yml", false);
      config = YAMLConfigurationLoader.builder().setPath(file.toPath()).build().load();

      if (config != null)
      {
        return true;
      }

      BungeeResourceUtil.saveResource(
              s -> DeltaBansPlugin.this.getClass().getResourceAsStream(s), dataDirectory.toFile(),
              "bungee-config.yml", "config-example.yml", true);
      logger.error("Invalid configuration file!");
      return false;
    }
    catch (IOException ex)
    {
      logger.error("Failed to load configuration file.");
      ex.printStackTrace();
      return false;
    }
  }

  public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
    char[] b = textToTranslate.toCharArray();
    for (int i = 0; i < b.length - 1; i++) {
      if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i+1]) > -1) {
        b[i] = '\u00A7';
        b[i+1] = Character.toLowerCase(b[i+1]);
      }
    }
    return new String(b);
  }

  private void readConfiguration(ConfigurationNode config)
  {
    inDebugMode = config.getNode("DebugMode").getBoolean(false);
    dbShareDataSourceName = config.getNode("DbShareDataSourceName").getString("<missing>");
    warningDurationInMinutes = config.getNode("WarningDurationInMinutes").getInt(10080);
    startWithWhitelistEnabled = config.getNode("StartWithWhitelistEnabled").getBoolean(false);
    messageFormatMap = new MessageFormatMap();
    warningCommandsMap = new HashMap<>();

    ConfigurationNode section;

    section = config.getNode("Formats");
    for (Map.Entry<Object, ? extends ConfigurationNode> formatNode : section.getChildrenMap().entrySet())
    {
      String formatKey = formatNode.getKey().toString();
      String format = formatNode.getValue().getString();
      String translated = translateAlternateColorCodes('&', format);
      messageFormatMap.put(formatKey, translated);
    }

    section = config.getNode("WarningCommands");
    for (Map.Entry<Object, ? extends ConfigurationNode> node : section.getChildrenMap().entrySet())
    {
      String key = node.getKey().toString();
      try
      {
        int amountOfWarnings = Integer.parseInt(key);
        List<String> commandsToRun = Arrays.asList(node.getValue().getChildrenList()
                .stream().map(ConfigurationNode::getString).toArray(String[]::new));
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
      logger.error("Failed to execute query: " + query);
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
