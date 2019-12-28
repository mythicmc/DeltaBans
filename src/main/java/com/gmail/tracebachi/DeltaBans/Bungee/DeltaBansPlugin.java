package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.BanListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.CheckBanListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.CheckWarningsListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.KickListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.PlayerLoginListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.WarningListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.WhitelistListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.LoadAndSaveable;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlRangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL.MySqlWhitelistStorage;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.BungeeResourceUtil;
import com.gmail.tracebachi.SockExchange.Utilities.JulBasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import io.github.kyzderp.bungeelogger.BungeeLog;
import io.github.kyzderp.bungeelogger.BungeeLoggerPlugin;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class DeltaBansPlugin extends Plugin {
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

    public DeltaBansPlugin() {
    }

    public void onEnable() {
        this.setupLoggers();
        if (this.reloadConfiguration()) {
            this.readConfiguration(this.config);
            if (this.createDatabaseTables()) {
                try {
                    this.banStorage = new MySqlBanStorage(this, this.basicLogger);
                    this.banStorage.load();
                    this.warningStorage = new MySqlWarningStorage(this, this.basicLogger, (long)(this.warningDurationInMinutes * 60 * 1000));
                    this.warningStorage.load();
                    this.rangeBanStorage = new MySqlRangeBanStorage(this, this.basicLogger);
                    this.rangeBanStorage.load();
                    this.whitelistStorage = new MySqlWhitelistStorage(this, this.basicLogger);
                    this.whitelistStorage.load();
                } catch (Exception var2) {
                    var2.printStackTrace();
                    return;
                }

                SockExchangeApi api = SockExchangeApi.instance();
                this.banListener = new BanListener(this, this.banStorage, this.rangeBanStorage, api, this.messageFormatMap);
                this.banListener.register();
                this.checkBanListener = new CheckBanListener(this.banStorage, api, this.messageFormatMap);
                this.checkBanListener.register();
                this.checkWarningsListener = new CheckWarningsListener(this.warningStorage, api, this.messageFormatMap);
                this.checkWarningsListener.register();
                this.kickListener = new KickListener(this, api, this.messageFormatMap);
                this.kickListener.register();
                this.playerLoginListener = new PlayerLoginListener(this, this.banStorage, this.rangeBanStorage, this.whitelistStorage, this.messageFormatMap, this.basicLogger, this.bungeeLoggerPluginLogger);
                this.playerLoginListener.register();
                this.warningListener = new WarningListener(this.warningStorage, this.warningCommandsMap, api, this.messageFormatMap);
                this.warningListener.register();
                this.whitelistListener = new WhitelistListener(this, this.whitelistStorage, api, this.messageFormatMap, this.startWithWhitelistEnabled);
                this.whitelistListener.register();
                this.cleanupExpiredWarningsFuture = api.getScheduledExecutorService().scheduleAtFixedRate(this.warningStorage::removeExpiredWarnings, 1L, 1L, TimeUnit.HOURS);
            }
        }
    }

    public void onDisable() {
        if (this.cleanupExpiredWarningsFuture != null) {
            this.cleanupExpiredWarningsFuture.cancel(false);
            this.cleanupExpiredWarningsFuture = null;
        }

        if (this.whitelistListener != null) {
            this.whitelistListener.unregister();
            this.whitelistListener = null;
        }

        if (this.warningListener != null) {
            this.warningListener.unregister();
            this.warningListener = null;
        }

        if (this.playerLoginListener != null) {
            this.playerLoginListener.unregister();
            this.playerLoginListener = null;
        }

        if (this.kickListener != null) {
            this.kickListener.unregister();
            this.kickListener = null;
        }

        if (this.checkWarningsListener != null) {
            this.checkWarningsListener.unregister();
            this.checkWarningsListener = null;
        }

        if (this.checkBanListener != null) {
            this.checkBanListener.unregister();
            this.checkBanListener = null;
        }

        if (this.banListener != null) {
            this.banListener.unregister();
            this.banListener = null;
        }

        if (this.banStorage != null) {
            this.saveLoadAndSaveable(this.banStorage);
            this.banStorage = null;
        }

        if (this.rangeBanStorage != null) {
            this.saveLoadAndSaveable(this.rangeBanStorage);
            this.rangeBanStorage = null;
        }

        if (this.warningStorage != null) {
            this.saveLoadAndSaveable(this.warningStorage);
            this.warningStorage = null;
        }

        if (this.whitelistStorage != null) {
            this.saveLoadAndSaveable(this.whitelistStorage);
            this.whitelistStorage = null;
        }

    }

    public Connection getConnection() throws SQLException {
        return DbShare.instance().getDataSource(this.dbShareDataSourceName).getConnection();
    }

    public Executor getExecutor() {
        return SockExchangeApi.instance().getScheduledExecutorService();
    }

    private void setupLoggers() {
        this.basicLogger = new JulBasicLogger(this.getLogger(), this.inDebugMode);
        this.bungeeLoggerPluginLogger = null;
        Plugin foundPlugin = this.getProxy().getPluginManager().getPlugin("BungeeLogger");
        if (foundPlugin != null) {
            BungeeLoggerPlugin bungeeLoggerPlugin = (BungeeLoggerPlugin)foundPlugin;
            this.bungeeLoggerPluginLogger = bungeeLoggerPlugin.createLogger(this);
        }

    }

    private boolean reloadConfiguration() {
        try {
            File file = BungeeResourceUtil.saveResource(this, "bungee-config.yml", "config.yml");
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            if (this.config != null) {
                return true;
            } else {
                BungeeResourceUtil.saveResource(this, "bungee-config.yml", "config-example.yml", true);
                this.getLogger().severe("Invalid configuration file!");
                return false;
            }
        } catch (IOException var2) {
            this.getLogger().severe("Failed to load configuration file.");
            var2.printStackTrace();
            return false;
        }
    }

    private void readConfiguration(Configuration config) {
        this.inDebugMode = config.getBoolean("DebugMode", false);
        this.dbShareDataSourceName = config.getString("DbShareDataSourceName", "<missing>");
        this.warningDurationInMinutes = config.getInt("WarningDurationInMinutes", 10080);
        this.startWithWhitelistEnabled = config.getBoolean("StartWithWhitelistEnabled", false);
        this.messageFormatMap = new MessageFormatMap();
        this.warningCommandsMap = new HashMap();
        Configuration section = config.getSection("Formats");
        Iterator var3 = section.getKeys().iterator();

        String key;
        while(var3.hasNext()) {
            key = (String)var3.next();
            String format = section.getString(key);
            String translated = ChatColor.translateAlternateColorCodes('&', format);
            this.messageFormatMap.put(key, translated);
        }

        section = config.getSection("WarningCommands");
        var3 = section.getKeys().iterator();

        while(var3.hasNext()) {
            key = (String)var3.next();

            try {
                int amountOfWarnings = Integer.parseInt(key);
                List<String> commandsToRun = section.getStringList(key);
                this.warningCommandsMap.put(amountOfWarnings, commandsToRun);
            } catch (NumberFormatException var7) {
            }
        }

    }

    private boolean createDatabaseTables() {
        try {
            Connection connection = this.getConnection();
            Throwable var2 = null;

            boolean var3;
            try {
                if (this.executeQuery(" CREATE TABLE IF NOT EXISTS deltabans_bans ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `name`      VARCHAR(32), `ip`        VARCHAR(40), `banner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `duration`  BIGINT, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`name`), INDEX (`ip`));", connection)) {
                    if (!this.executeQuery(" CREATE TABLE IF NOT EXISTS deltabans_warnings ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `name`      VARCHAR(32) NOT NULL, `warner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`name`));", connection)) {
                        var3 = false;
                        return var3;
                    }

                    if (!this.executeQuery(" CREATE TABLE IF NOT EXISTS deltabans_rangebans ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `ip_start`  VARCHAR(40) NOT NULL, `ip_end`    VARCHAR(40) NOT NULL, `banner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`ip_start`, `ip_end`));", connection)) {
                        var3 = false;
                        return var3;
                    }

                    if (this.executeQuery(" CREATE TABLE IF NOT EXISTS deltabans_whitelist ( `name` VARCHAR(32) NOT NULL PRIMARY KEY, `type` INT UNSIGNED NOT NULL DEFAULT 0);", connection)) {
                        return true;
                    }

                    var3 = false;
                    return var3;
                }

                var3 = false;
            } catch (Throwable var17) {
                var2 = var17;
                throw var17;
            } finally {
                if (connection != null) {
                    if (var2 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var16) {
                            var2.addSuppressed(var16);
                        }
                    } else {
                        connection.close();
                    }
                }

            }

            return var3;
        } catch (SQLException var19) {
            var19.printStackTrace();
            return false;
        }
    }

    private boolean executeQuery(String query, Connection connection) throws SQLException {
        try {
            Statement statement = connection.createStatement();
            Throwable var4 = null;

            boolean var5;
            try {
                statement.execute(query);
                var5 = true;
            } catch (Throwable var15) {
                var4 = var15;
                throw var15;
            } finally {
                if (statement != null) {
                    if (var4 != null) {
                        try {
                            statement.close();
                        } catch (Throwable var14) {
                            var4.addSuppressed(var14);
                        }
                    } else {
                        statement.close();
                    }
                }

            }

            return var5;
        } catch (SQLException var17) {
            this.getLogger().severe("Failed to execute query: " + query);
            var17.printStackTrace();
            return false;
        }
    }

    private void saveLoadAndSaveable(LoadAndSaveable loadAndSaveable) {
        try {
            loadAndSaveable.save();
        } catch (Exception var3) {
            var3.printStackTrace();
        }

    }
}
