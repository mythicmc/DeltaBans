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
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class CheckBanListener implements Registerable
{
  private final BanStorage banStorage;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> checkBanChannelListener;

  public CheckBanListener(BanStorage banStorage, SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(banStorage, "banStorage");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.banStorage = banStorage;
    this.api = api;
    this.formatMap = formatMap;
    this.checkBanChannelListener = this::onCheckBanChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.CHECK_BAN, checkBanChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.CHECK_BAN, checkBanChannelListener);
  }

  private void onCheckBanChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String sender = in.readUTF();
    String nameOrIpToCheck = in.readUTF();
    boolean isIp = in.readBoolean();
    List<String> chatMessages = new ArrayList<>(4);
    BanEntry banEntry;

    // Get a ban entry based on the name or IP
    if (isIp)
    {
      banEntry = banStorage.getBanEntry(null, nameOrIpToCheck);
    }
    else
    {
      banEntry = banStorage.getBanEntry(nameOrIpToCheck, null);
    }

    // Create the messages to reply with
    if (banEntry == null)
    {
      chatMessages.add(formatMap.format(Formats.BAN_NOT_FOUND, nameOrIpToCheck));
    }
    else
    {
      String name = banEntry.hasName() ? banEntry.getName() : "-";
      String banner = banEntry.getBanner();
      String message = banEntry.getMessage();
      String formattedDuration = DeltaBansUtils.formatDuration(banEntry.getDuration());

      chatMessages.add(formatMap.format(Formats.BAN_INFO_LINE, "Name", name));
      chatMessages.add(formatMap.format(Formats.BAN_INFO_LINE, "Banner", banner));
      chatMessages.add(formatMap.format(Formats.BAN_INFO_LINE, "Message", message));
      chatMessages.add(formatMap.format(Formats.BAN_INFO_LINE, "Duration", formattedDuration));
    }

    // Send the messages
    api.sendChatMessages(chatMessages, sender, sendingServer);
  }
}
