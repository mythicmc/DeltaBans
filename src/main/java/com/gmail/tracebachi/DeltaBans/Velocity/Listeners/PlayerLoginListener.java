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
package com.gmail.tracebachi.DeltaBans.Velocity.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Velocity.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import io.github.kyzderp.bungeelogger.BungeeLog;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class PlayerLoginListener implements Registerable
{
  private final DeltaBansPlugin plugin;
  private final BanStorage banStorage;
  private final RangeBanStorage rangeBanStorage;
  private final WhitelistStorage whitelistStorage;
  private final MessageFormatMap formatMap;
  private final BasicLogger logger;
  private final BungeeLog bungeeLoggerPluginLogger;

  public PlayerLoginListener(
          DeltaBansPlugin plugin, BanStorage banStorage, RangeBanStorage rangeBanStorage,
          WhitelistStorage whitelistStorage, MessageFormatMap formatMap, BasicLogger logger,
          BungeeLog bungeeLoggerPluginLogger)
  {
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

  @Override
  public void register()
  {
    plugin.getServer().getEventManager().register(plugin, this);
  }

  @Override
  public void unregister()
  {
    plugin.getServer().getEventManager().unregisterListener(plugin, this);
  }

  @Subscribe
  public void onPlayerLogin(LoginEvent event)
  {
    Player pending = event.getPlayer();
    String playerName = pending.getUsername().toLowerCase();
    String address = pending.getRemoteAddress().getAddress().getHostAddress();
    RangeBanEntry rangeBanEntry = rangeBanStorage.getIpRangeBan(address);

    if (rangeBanEntry != null && !whitelistStorage.isOnRangeBanWhitelist(playerName))
    {
      String message = rangeBanEntry.getMessage();
      String banner = rangeBanEntry.getBanner();
      logDeniedLoginAttempt(playerName, address, message, banner);

      event.setResult(LoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacySection()
              .deserialize(getKickMessage(rangeBanEntry))));
      event.getPlayer().disconnect(LegacyComponentSerializer.legacySection()
              .deserialize(getKickMessage(rangeBanEntry)));
      return;
    }

    BanEntry banEntry = banStorage.getBanEntry(playerName, address);

    if (banEntry != null)
    {
      String message = banEntry.getMessage();
      String banner = banEntry.getBanner();
      logDeniedLoginAttempt(playerName, address, message, banner);

      event.setResult(LoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacySection()
              .deserialize(getKickMessage(banEntry))));
      event.getPlayer().disconnect(LegacyComponentSerializer.legacySection()
              .deserialize(getKickMessage(banEntry)));
    }
  }

  private String getKickMessage(BanEntry entry)
  {
    String message = entry.getMessage();
    String banner = entry.getBanner();

    if (entry.hasDuration())
    {
      long currentTime = System.currentTimeMillis();
      long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;
      String formattedDuration = DeltaBansUtils.formatDuration(remainingTime);

      return formatMap.format(Formats.TEMP_BAN_MESSAGE, message, banner, formattedDuration);
    }
    else
    {
      return formatMap.format(Formats.PERMANENT_BAN_MESSAGE, message, banner);
    }
  }

  private String getKickMessage(RangeBanEntry entry)
  {
    String message = entry.getMessage();
    String banner = entry.getBanner();
    return formatMap.format(Formats.RANGE_BAN_MESSAGE, message, banner);
  }

  private void logDeniedLoginAttempt(String name, String ip, String reason, String banner)
  {
    String message = "[DeniedLoginAttempt] " + name + " @ " + ip + " for \"" + reason + "\" by " + banner;

    logger.info(message);

    if (bungeeLoggerPluginLogger != null)
    {
      bungeeLoggerPluginLogger.info(message);
    }
  }
}
