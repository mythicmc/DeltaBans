package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import io.github.kyzderp.bungeelogger.BungeeLog;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class PlayerLoginListener implements Listener, Registerable {
    private final DeltaBansPlugin plugin;
    private final BanStorage banStorage;
    private final RangeBanStorage rangeBanStorage;
    private final WhitelistStorage whitelistStorage;
    private final MessageFormatMap formatMap;
    private final BasicLogger logger;
    private final BungeeLog bungeeLoggerPluginLogger;

    public PlayerLoginListener(DeltaBansPlugin plugin, BanStorage banStorage, RangeBanStorage rangeBanStorage, WhitelistStorage whitelistStorage, MessageFormatMap formatMap, BasicLogger logger, BungeeLog bungeeLoggerPluginLogger) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(banStorage, "banStorage");
        Preconditions.checkNotNull(rangeBanStorage, "rangeBanStorage");
        Preconditions.checkNotNull(whitelistStorage, "whitelistStorage");
        Preconditions.checkNotNull(formatMap, "formatMap");
        Preconditions.checkNotNull(logger, "logger");
        this.plugin = plugin;
        this.banStorage = banStorage;
        this.rangeBanStorage = rangeBanStorage;
        this.whitelistStorage = whitelistStorage;
        this.formatMap = formatMap;
        this.logger = logger;
        this.bungeeLoggerPluginLogger = bungeeLoggerPluginLogger;
    }

    public void register() {
        this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
    }

    public void unregister() {
        this.plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @EventHandler(
            priority = -64
    )
    public void onPlayerLogin(LoginEvent event) {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName().toLowerCase();
        String address = pending.getAddress().getAddress().getHostAddress();
        RangeBanEntry rangeBanEntry = this.rangeBanStorage.getIpRangeBan(address);
        String message;
        if (rangeBanEntry != null && !this.whitelistStorage.isOnRangeBanWhitelist(playerName)) {
            String message = rangeBanEntry.getMessage();
            message = rangeBanEntry.getBanner();
            this.logDeniedLoginAttempt(playerName, address, message, message);
            event.setCancelReason(this.getKickMessage(rangeBanEntry));
            event.setCancelled(true);
        } else {
            BanEntry banEntry = this.banStorage.getBanEntry(playerName, address);
            if (banEntry != null) {
                message = banEntry.getMessage();
                String banner = banEntry.getBanner();
                this.logDeniedLoginAttempt(playerName, address, message, banner);
                event.setCancelReason(this.getKickMessage(banEntry));
                event.setCancelled(true);
            }

        }
    }

    private String getKickMessage(BanEntry entry) {
        String message = entry.getMessage();
        String banner = entry.getBanner();
        if (entry.hasDuration()) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;
            String formattedDuration = DeltaBansUtils.formatDuration(remainingTime);
            return this.formatMap.format("TemporaryBanMessage", new Object[]{message, banner, formattedDuration});
        } else {
            return this.formatMap.format("PermanentBanMessage", new Object[]{message, banner});
        }
    }

    private String getKickMessage(RangeBanEntry entry) {
        String message = entry.getMessage();
        String banner = entry.getBanner();
        return this.formatMap.format("RangeBanMessage", new Object[]{message, banner});
    }

    private void logDeniedLoginAttempt(String name, String ip, String reason, String banner) {
        String message = "[DeniedLoginAttempt] " + name + " @ " + ip + " for \"" + reason + "\" by " + banner;
        this.logger.info(message, new Object[0]);
        if (this.bungeeLoggerPluginLogger != null) {
            this.bungeeLoggerPluginLogger.info(message);
        }

    }
}
