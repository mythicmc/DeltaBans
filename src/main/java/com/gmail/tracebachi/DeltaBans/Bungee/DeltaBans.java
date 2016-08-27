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
package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.BanListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.GeneralListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.WarningListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.BungeeLoggerLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.DeltaBansLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.JavaLoggingLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySQLBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySQLRangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySQLWarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySQLWhitelistStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaRedis.Bungee.ConfigUtil;
import io.github.kyzderp.bungeelogger.BungeeLoggerPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class DeltaBans extends Plugin
{
    private Configuration config;
    private DeltaBansLogger logger;

    private GeneralListener generalListener;
    private BanListener banListener;
    private WarningListener warningListener;
    private WhitelistStorage whitelistStorage;

    private BanStorage banStorage;
    private WarningStorage warningStorage;
    private RangeBanStorage rangeBanStorage;

    @Override
    public void onEnable()
    {
        reloadConfig();
        if(config == null) { return; }

        Settings.read(config);

        setupLogger();

        if(!createDatabaseTables())
        {
            severe("Failed to setup database tables. DeltaBans will not function.");
            return;
        }
        else
        {
            debug("Database table setup complete.");
        }

        try
        {
            banStorage = new MySQLBanStorage(this);
            banStorage.load();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            severe("Failed to load BanStorage. DeltaBans will not function.");
            return;
        }

        try
        {
            warningStorage = new MySQLWarningStorage(this);
            warningStorage.load();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            severe("Failed to load WarningStorage. DeltaBans will not function.");
            return;
        }

        try
        {
            rangeBanStorage = new MySQLRangeBanStorage(this);
            rangeBanStorage.load();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            severe("Failed to load RangeBanStorage. DeltaBans will not function.");
            return;
        }

        try
        {
            whitelistStorage = new MySQLWhitelistStorage(this);
            whitelistStorage.load();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            severe("Failed to load WhitelistStorage. DeltaBans will not function.");
            return;
        }

        banListener = new BanListener(this);
        banListener.register();

        warningListener = new WarningListener(warningStorage, this);
        warningListener.register();

        generalListener = new GeneralListener(this);
        generalListener.register();
    }

    @Override
    public void onDisable()
    {
        getProxy().getScheduler().cancel(this);

        if(generalListener != null)
        {
            generalListener.shutdown();
            generalListener = null;
        }

        if(warningListener != null)
        {
            warningListener.shutdown();
            warningListener = null;
        }

        if(banListener != null)
        {
            banListener.shutdown();
            banListener = null;
        }
    }

    public BanStorage getBanStorage()
    {
        return banStorage;
    }

    public RangeBanStorage getRangeBanStorage()
    {
        return rangeBanStorage;
    }

    public WarningStorage getWarningStorage()
    {
        return warningStorage;
    }

    public WhitelistStorage getWhitelistStorage()
    {
        return whitelistStorage;
    }

    public void info(String message)
    {
        logger.info(message);
    }

    public void severe(String message)
    {
        logger.severe(message);
    }

    public void debug(String message)
    {
        if(Settings.isDebugEnabled()) { logger.debug(message); }
    }

    private void reloadConfig()
    {
        try
        {
            File file = ConfigUtil.saveResource(this, "bungee-config.yml", "config.yml");
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            if(config == null)
            {
                ConfigUtil.saveResource(this, "bungee-config.yml", "config-example.yml", true);
                getLogger().severe("Invalid configuration file! " +
                    "An example configuration has been saved to the DeltaBans folder.");
            }
        }
        catch(IOException e)
        {
            getLogger().severe("Failed to load configuration file.");
            e.printStackTrace();
        }
    }

    private void setupLogger()
    {
        Plugin foundPlugin = getProxy().getPluginManager().getPlugin("BungeeLogger");

        if(foundPlugin != null)
        {
            BungeeLoggerPlugin bungeeLoggerPlugin = (BungeeLoggerPlugin) foundPlugin;

            logger = new BungeeLoggerLogger(
                bungeeLoggerPlugin.createLogger(this));
        }
        else
        {
            logger = new JavaLoggingLogger(getLogger());
        }
    }

    private boolean createDatabaseTables()
    {
        String createBanTable =
            " CREATE TABLE IF NOT EXISTS deltabans_bans (" +
            " `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            " `name`      VARCHAR(32)," +
            " `ip`        VARCHAR(40)," +
            " `banner`    VARCHAR(32) NOT NULL," +
            " `message`   VARCHAR(255) NOT NULL," +
            " `duration`  BIGINT," +
            " `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            " INDEX (`name`)," +
            " INDEX (`ip`));";
        String createWarningTable =
            " CREATE TABLE IF NOT EXISTS deltabans_warnings (" +
            " `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            " `name`      VARCHAR(32) NOT NULL," +
            " `warner`    VARCHAR(32) NOT NULL," +
            " `message`   VARCHAR(255) NOT NULL," +
            " `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            " INDEX (`name`));";
        String createRangeBanTable =
            " CREATE TABLE IF NOT EXISTS deltabans_rangebans (" +
            " `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY," +
            " `ip_start`  VARCHAR(40) NOT NULL," +
            " `ip_end`    VARCHAR(40) NOT NULL," +
            " `banner`    VARCHAR(32) NOT NULL," +
            " `message`   VARCHAR(255) NOT NULL," +
            " `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            " INDEX (`ip_start`, `ip_end`));";
        String createWhitelistTable =
            " CREATE TABLE IF NOT EXISTS deltabans_whitelist (" +
            " `name` VARCHAR(32) NOT NULL PRIMARY KEY," +
            " `type` INT UNSIGNED NOT NULL DEFAULT 0);";

        try(Connection connection = Settings.getDataSource().getConnection())
        {
            if(!createDatabaseTable(createBanTable, connection)) { return false; }
            if(!createDatabaseTable(createWarningTable, connection)) { return false; }
            if(!createDatabaseTable(createRangeBanTable, connection)) { return false; }
            if(!createDatabaseTable(createWhitelistTable, connection)) { return false; }
        }
        catch(SQLException e)
        {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean createDatabaseTable(String query, Connection connection) throws SQLException
    {
        try(Statement statement = connection.createStatement())
        {
            statement.execute(query);
            return true;
        }
        catch(SQLException ex)
        {
            severe("Failed to execute query:\n" + query);
            ex.printStackTrace();
            return false;
        }
    }
}
