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

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
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
public class CheckWarningsListener implements Registerable
{
  private final WarningStorage warningStorage;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> checkWarningsChannelListener;

  public CheckWarningsListener(
    WarningStorage warningStorage, SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(warningStorage, "warningStorage");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.warningStorage = warningStorage;
    this.api = api;
    this.formatMap = formatMap;
    this.checkWarningsChannelListener = this::onCheckWarningsChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.CHECK_WARNINGS, checkWarningsChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.CHECK_WARNINGS, checkWarningsChannelListener);
  }

  private void onCheckWarningsChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String sender = in.readUTF();
    String nameToCheck = in.readUTF();
    List<String> chatMessages = new ArrayList<>(4);
    List<WarningEntry> warningEntryList = warningStorage.getWarnings(nameToCheck);

    // Add messages for the found warnings
    if (warningEntryList == null || warningEntryList.isEmpty())
    {
      chatMessages.add(formatMap.format(Formats.WARNINGS_NOT_FOUND, nameToCheck));
    }
    else
    {
      for (WarningEntry warningEntry : warningEntryList)
      {
        String warner = warningEntry.getWarner();
        String message = warningEntry.getMessage();

        chatMessages.add(formatMap.format(Formats.WARNING_INFO_LINE, warner, message));
      }
    }

    // Send the messages
    api.sendChatMessages(chatMessages, sender, sendingServer);
  }
}
