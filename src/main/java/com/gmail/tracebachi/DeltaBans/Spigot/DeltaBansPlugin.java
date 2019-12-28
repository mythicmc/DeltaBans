package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DbShare.DbShare;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.BanCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.BannedCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.KickCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.RangeBanCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.RangeBanWhitelistCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.RangeUnbanCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.TempBanCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.UnbanCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.UnwarnCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.WarnCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.WarningsCommand;
import com.gmail.tracebachi.DeltaBans.Spigot.Commands.WhitelistCommand;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public class DeltaBansPlugin extends JavaPlugin {
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

    public DeltaBansPlugin() {
    }

    public void onEnable() {
        this.saveDefaultConfig();
        this.reloadConfig();
        this.readConfiguration(this.getConfig());
        SockExchangeApi api = SockExchangeApi.instance();
        this.banCommand = new BanCommand(this, api, this.messageFormatMap);
        this.banCommand.register();
        this.bannedCommand = new BannedCommand(this, api, this.messageFormatMap);
        this.bannedCommand.register();
        this.kickCommand = new KickCommand(this, api, this.messageFormatMap);
        this.kickCommand.register();
        this.rangeBanCommand = new RangeBanCommand(this, api, this.messageFormatMap);
        this.rangeBanCommand.register();
        this.rangeUnbanCommand = new RangeUnbanCommand(this, api, this.messageFormatMap);
        this.rangeUnbanCommand.register();
        this.rangeBanWhitelistCommand = new RangeBanWhitelistCommand(this, api, this.messageFormatMap);
        this.rangeBanWhitelistCommand.register();
        this.tempBanCommand = new TempBanCommand(this, api, this.messageFormatMap);
        this.tempBanCommand.register();
        this.unbanCommand = new UnbanCommand(this, api, this.messageFormatMap);
        this.unbanCommand.register();
        this.unwarnCommand = new UnwarnCommand(this, api, this.messageFormatMap);
        this.unwarnCommand.register();
        this.warnCommand = new WarnCommand(this, api, this.messageFormatMap);
        this.warnCommand.register();
        this.warningsCommand = new WarningsCommand(this, api, this.messageFormatMap);
        this.warningsCommand.register();
        this.whitelistCommand = new WhitelistCommand(this, api, this.messageFormatMap);
        this.whitelistCommand.register();
        this.announcementListener = new AnnouncementListener(this, api, this.silentAnnouncementPrefix);
        this.announcementListener.register();
    }

    public void onDisable() {
        if (this.announcementListener != null) {
            this.announcementListener.unregister();
            this.announcementListener = null;
        }

        if (this.whitelistCommand != null) {
            this.whitelistCommand.unregister();
            this.whitelistCommand = null;
        }

        if (this.warningsCommand != null) {
            this.warningsCommand.unregister();
            this.warningsCommand = null;
        }

        if (this.warnCommand != null) {
            this.warnCommand.unregister();
            this.warnCommand = null;
        }

        if (this.unwarnCommand != null) {
            this.unwarnCommand.unregister();
            this.unwarnCommand = null;
        }

        if (this.unbanCommand != null) {
            this.unbanCommand.unregister();
            this.unbanCommand = null;
        }

        if (this.tempBanCommand != null) {
            this.tempBanCommand.unregister();
            this.tempBanCommand = null;
        }

        if (this.rangeBanCommand != null) {
            this.rangeUnbanCommand.unregister();
            this.rangeUnbanCommand = null;
        }

        if (this.rangeBanWhitelistCommand != null) {
            this.rangeBanWhitelistCommand.unregister();
            this.rangeBanWhitelistCommand = null;
        }

        if (this.rangeBanCommand != null) {
            this.rangeBanCommand.unregister();
            this.rangeBanCommand = null;
        }

        if (this.kickCommand != null) {
            this.kickCommand.unregister();
            this.kickCommand = null;
        }

        if (this.bannedCommand != null) {
            this.bannedCommand.unregister();
            this.bannedCommand = null;
        }

        if (this.banCommand != null) {
            this.banCommand.unregister();
            this.banCommand = null;
        }

    }

    public Connection getConnection() throws SQLException {
        return DbShare.instance().getDataSource(this.dbShareDataSourceName).getConnection();
    }

    public String getIpOfPlayer(String playerName) {
        String ipLookupQuery = "SELECT `" + this.ipColumnName + "` FROM `" + this.accountsTableName + "` WHERE `" + this.playerColumnName + "` = ?;";

        try {
            Connection connection = this.getConnection();
            Throwable var4 = null;

            Object var9;
            try {
                PreparedStatement statement = connection.prepareStatement(ipLookupQuery);
                Throwable var6 = null;

                try {
                    statement.setString(1, playerName);
                    ResultSet resultSet = statement.executeQuery();
                    Throwable var8 = null;

                    try {
                        var9 = resultSet.next() ? resultSet.getString(this.ipColumnName) : "";
                    } catch (Throwable var56) {
                        var9 = var56;
                        var8 = var56;
                        throw var56;
                    } finally {
                        if (resultSet != null) {
                            if (var8 != null) {
                                try {
                                    resultSet.close();
                                } catch (Throwable var55) {
                                    var8.addSuppressed(var55);
                                }
                            } else {
                                resultSet.close();
                            }
                        }

                    }
                } catch (Throwable var58) {
                    var6 = var58;
                    throw var58;
                } finally {
                    if (statement != null) {
                        if (var6 != null) {
                            try {
                                statement.close();
                            } catch (Throwable var54) {
                                var6.addSuppressed(var54);
                            }
                        } else {
                            statement.close();
                        }
                    }

                }
            } catch (Throwable var60) {
                var4 = var60;
                throw var60;
            } finally {
                if (connection != null) {
                    if (var4 != null) {
                        try {
                            connection.close();
                        } catch (Throwable var53) {
                            var4.addSuppressed(var53);
                        }
                    } else {
                        connection.close();
                    }
                }

            }

            return (String)var9;
        } catch (SQLException var62) {
            var62.printStackTrace();
            return "";
        }
    }

    public void executeSync(Runnable runnable) {
        this.getServer().getScheduler().runTask(this, runnable);
    }

    private void readConfiguration(ConfigurationSection config) {
        this.inDebugMode = config.getBoolean("DebugMode", false);
        this.dbShareDataSourceName = config.getString("DbShareDataSourceName", "<missing>");
        this.accountsTableName = config.getString("IpLookup.Table");
        this.ipColumnName = config.getString("IpLookup.IpColumn");
        this.playerColumnName = config.getString("IpLookup.PlayerColumn");
        this.silentAnnouncementPrefix = ChatColor.translateAlternateColorCodes('&', config.getString("SilentAnnouncementPrefix"));
        this.messageFormatMap = new MessageFormatMap();
        ConfigurationSection section = config.getConfigurationSection("Formats");
        Iterator var3 = section.getKeys(false).iterator();

        while(var3.hasNext()) {
            String formatKey = (String)var3.next();
            String format = section.getString(formatKey);
            String translated = ChatColor.translateAlternateColorCodes('&', format);
            this.messageFormatMap.put(formatKey, translated);
        }

    }
}
