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
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Velocity.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Velocity.Events.BanEvent;
import com.gmail.tracebachi.DeltaBans.Velocity.Events.RangeBanEvent;
import com.gmail.tracebachi.DeltaBans.Velocity.Events.RangeUnbanEvent;
import com.gmail.tracebachi.DeltaBans.Velocity.Events.UnbanEvent;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class BanListener implements Registerable
{
  private final String HIDDEN_IP =
    "\u00A7rx\u00A76.\u00A7rx\u00A76.\u00A7rx\u00A76.\u00A7rx";

  private final DeltaBansPlugin plugin;
  private final BanStorage banStorage;
  private final RangeBanStorage rangeBanStorage;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> banChannelListener;
  private final Consumer<ReceivedMessage> unbanChannelListener;
  private final Consumer<ReceivedMessage> rangeBanChannelListener;
  private final Consumer<ReceivedMessage> rangeUnbanChannelListener;

  public BanListener(
          DeltaBansPlugin plugin, BanStorage banStorage, RangeBanStorage rangeBanStorage,
          SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(banStorage, "banStorage");
    Preconditions.checkNotNull(rangeBanStorage, "rangeBanStorage");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.banStorage = banStorage;
    this.rangeBanStorage = rangeBanStorage;
    this.api = api;
    this.formatMap = formatMap;
    this.banChannelListener = this::onBanChannelRequest;
    this.unbanChannelListener = this::onUnbanChannelRequest;
    this.rangeBanChannelListener = this::onRangeBanChannelRequest;
    this.rangeUnbanChannelListener = this::onRangeUnbanChannelRequest;
  }

  @Override
  public void register()
  {
    plugin.getServer().getEventManager().register(plugin, this);

    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.register(Channels.BAN, banChannelListener);
    messageNotifier.register(Channels.UNBAN, unbanChannelListener);
    messageNotifier.register(Channels.RANGE_BAN, rangeBanChannelListener);
    messageNotifier.register(Channels.RANGE_UNBAN, rangeUnbanChannelListener);
  }

  @Override
  public void unregister()
  {
    plugin.getServer().getEventManager().unregisterListener(plugin, this);

    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.unregister(Channels.BAN, banChannelListener);
    messageNotifier.unregister(Channels.UNBAN, unbanChannelListener);
    messageNotifier.unregister(Channels.RANGE_BAN, rangeBanChannelListener);
    messageNotifier.unregister(Channels.RANGE_UNBAN, rangeUnbanChannelListener);
  }

  private void onBanChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String name = in.readUTF();
    String ip = in.readUTF();
    String banner = in.readUTF();
    String message = in.readUTF();
    Long duration = in.readBoolean() ? in.readLong() : null;
    boolean isSilent = in.readBoolean();

    // TODO: Update to not use nulls. Use empty strings instead
    name = name.equals("") ? null : name;
    ip = ip.equals("") ? null : ip;
    message = message.equals("") ? formatMap.format(Formats.DEFAULT_MESSAGE_BAN) : message;

    // Create and try to add the ban entry
    BanEntry entry = new BanEntry(name, ip, banner, message, duration);
    BanStorage.AddResult addResult = banStorage.addBanEntry(entry);

    if (addResult == BanStorage.AddResult.EXISTING_NAME_BAN)
    {
      String chatMessage = formatMap.format(Formats.BAN_ALREADY_EXISTS, name, "-");
      sendChatMessage(chatMessage, banner, sendingServer);
    }
    else if (addResult == BanStorage.AddResult.EXISTING_IP_BAN)
    {
      String chatMessage = formatMap.format(Formats.BAN_ALREADY_EXISTS, "-", ip);
      sendChatMessage(chatMessage, banner, sendingServer);
    }
    else if (addResult == BanStorage.AddResult.EXISTING_NAME_AND_IP_BAN)
    {
      String chatMessage = formatMap.format(Formats.BAN_ALREADY_EXISTS, name, ip);
      sendChatMessage(chatMessage, banner, sendingServer);
    }
    else if (addResult == BanStorage.AddResult.SUCCESS)
    {
      kickOffProxy(name, ip, getKickMessage(entry));
      announce(formatBanAnnouncement(entry, isSilent), isSilent);

      // fire event
      plugin.getServer().getEventManager().fire(new BanEvent(banner, name, message, ip, isSilent, duration));
    }
  }

  private void onUnbanChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String unbanner = in.readUTF();
    String nameOrIpToUnban = in.readUTF();
    boolean isIp = in.readBoolean();
    boolean isSilent = in.readBoolean();
    List<BanEntry> entries;

    if (isIp)
    {
      entries = banStorage.removeUsingIp(nameOrIpToUnban);
    }
    else
    {
      entries = banStorage.removeUsingName(nameOrIpToUnban);
    }

    if (entries == null || entries.size() == 0)
    {
      String chatMessage = formatMap.format(Formats.BAN_NOT_FOUND, nameOrIpToUnban);
      sendChatMessage(chatMessage, unbanner, sendingServer);
      return;
    }

    String announcement = formatUnbanAnnouncement(unbanner, nameOrIpToUnban, isIp, isSilent);
    announce(announcement, isSilent);

    // fire event
    plugin.getServer().getEventManager().fire(new UnbanEvent(unbanner, nameOrIpToUnban, isIp, isSilent));
  }

  private void onRangeBanChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String banner = in.readUTF();
    String message = in.readUTF();
    String startIp = in.readUTF();
    String endIp = in.readUTF();
    boolean isSilent = in.readBoolean();

    message = message.equals("") ? formatMap.format(Formats.DEFAULT_MESSAGE_RANGEBAN) : message;

    // Create and add the range ban entry
    RangeBanEntry entry = new RangeBanEntry(banner, message, startIp, endIp);
    rangeBanStorage.add(entry);

    // Kick off all players within the start and end IP range
    long startAddressLong = entry.getStartAddressLong();
    long endAddressLong = entry.getEndAddressLong();
    kickOffProxy(startAddressLong, endAddressLong, entry.getMessage());

    // Announce the range ban
    String rangeBanRange;
    if (isSilent)
    {
      rangeBanRange = startIp + "-" + endIp;
    }
    else
    {
      rangeBanRange = HIDDEN_IP + "-" + HIDDEN_IP;
    }

    announce(formatMap.format(Formats.ANNOUNCE_RANGEBAN, banner, rangeBanRange, message), isSilent);

    // fire event
    plugin.getServer().getEventManager().fire(new RangeBanEvent(banner, startIp, endIp, message, isSilent));
  }

  private void onRangeUnbanChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String unbanner = in.readUTF();
    String ip = in.readUTF();
    boolean isSilent = in.readBoolean();

    // Remove range bans that overlap the specified IP
    int count = rangeBanStorage.removeIpRangeBan(ip);

    // Announce
    String announcement = formatMap.format(Formats.ANNOUNCE_RANGEUNBAN, unbanner,
      String.valueOf(count), ip);
    announce(announcement, isSilent);

    // fire event
    plugin.getServer().getEventManager().fire(new RangeUnbanEvent(unbanner, ip, isSilent));
  }

  private void kickOffProxy(String name, String ip, String message)
  {
    Component componentMessage = LegacyComponentSerializer.legacySection().deserialize(message);

    if (name != null)
    {
      plugin.getServer().getPlayer(name).ifPresent(player -> player.disconnect(componentMessage));

    }

    if (ip != null)
    {
      for (Player player : plugin.getServer().getAllPlayers())
      {
        if (player.getRemoteAddress().getAddress().getHostAddress().equals(ip))
        {
          player.disconnect(componentMessage);
        }
      }
    }
  }

  private void kickOffProxy(long startIp, long endIp, String message)
  {
    for (Player player : plugin.getServer().getAllPlayers())
    {
      long ipAsLong = DeltaBansUtils.convertIpToLong(
        player.getRemoteAddress().getAddress().getHostAddress());

      if (ipAsLong >= startIp && ipAsLong <= endIp)
      {
        player.disconnect(LegacyComponentSerializer.legacySection().deserialize(message));
      }
    }
  }

  private String formatBanAnnouncement(BanEntry entry, boolean isSilent)
  {
    String banner = entry.getBanner();
    String banMessage = entry.getMessage();
    String formattedDuration = DeltaBansUtils.formatDuration(entry.getDuration());
    String bannedNameOrIp;

    if (entry.hasName() || isSilent)
    {
      bannedNameOrIp = (entry.hasName()) ? entry.getName() : entry.getIp();
    }
    else
    {
      bannedNameOrIp = HIDDEN_IP;
    }

    return formatMap.format(
      Formats.ANNOUNCE_BAN, banner, bannedNameOrIp, banMessage, formattedDuration);
  }

  private String formatUnbanAnnouncement(
    String sender, String nameOrIpToUnban, boolean isIp, boolean isSilent)
  {
    if (isIp && !isSilent)
    {
      return formatMap.format(Formats.ANNOUNCE_UNBAN, sender, HIDDEN_IP);
    }
    else
    {
      return formatMap.format(Formats.ANNOUNCE_UNBAN, sender, nameOrIpToUnban);
    }
  }

  private void announce(String announcement, boolean isSilent)
  {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
    out.writeBoolean(isSilent);
    out.writeUTF(announcement);

    api.sendToServers(Channels.ANNOUNCEMENT, out.toByteArray());
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

  private void sendChatMessage(String message, String playerName, String serverName)
  {
    api.sendChatMessages(Collections.singletonList(message), playerName, serverName);
  }
}
