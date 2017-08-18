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
package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class WarningListener implements Registerable
{
  private static final Pattern NAME_PATTERN = Pattern.compile("\\{name}");
  private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\{message}");

  private final WarningStorage warningStorage;
  private final Map<Integer, List<String>> warningCommandsMap;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> warnChannelListener;
  private final Consumer<ReceivedMessage> unwarnChannelListener;

  public WarningListener(
    WarningStorage warningStorage, Map<Integer, List<String>> warningCommandsMap,
    SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(warningStorage, "warningStorage");
    Preconditions.checkNotNull(warningCommandsMap, "warningCommandsMap");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.warningStorage = warningStorage;
    this.warningCommandsMap = warningCommandsMap;
    this.api = api;
    this.formatMap = formatMap;
    this.warnChannelListener = this::onWarnChannelRequest;
    this.unwarnChannelListener = this::onUnwarnChannelRequest;
  }

  @Override
  public void register()
  {
    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.register(Channels.WARN, warnChannelListener);
    messageNotifier.register(Channels.UNWARN, unwarnChannelListener);
  }

  @Override
  public void unregister()
  {
    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.unregister(Channels.WARN, warnChannelListener);
    messageNotifier.unregister(Channels.UNWARN, unwarnChannelListener);
  }

  private void onWarnChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String warner = in.readUTF();
    String nameToWarn = in.readUTF();
    String message = in.readUTF();
    boolean isSilent = in.readBoolean();

    message = message.equals("") ? formatMap.format(Formats.DEFAULT_MESSAGE_WARN) : message;

    // Add warning
    WarningEntry warningEntry = new WarningEntry(nameToWarn, warner, message);
    int count = warningStorage.addWarning(warningEntry);

    String announcement = formatMap.format(Formats.ANNOUNCE_WARN, warner, nameToWarn, message);
    announce(announcement, isSilent);

    // Get the list of command formats for warnings
    List<String> commandFormats = warningCommandsMap.getOrDefault(count, Collections.emptyList());

    // Respond with the list of commands to run
    byte[] bytes = getBytesForCommandsToRunForWarning(nameToWarn, message, commandFormats);
    receivedMessage.respond(bytes);
  }

  private void onUnwarnChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String warner = in.readUTF();
    String nameToUnwarn = in.readUTF();
    int amountToUnwarn = in.readInt();
    boolean isSilent = in.readBoolean();

    // Remove warnings
    int removeCount = warningStorage.removeWarnings(nameToUnwarn, amountToUnwarn);
    if (removeCount == 0)
    {
      String chatMessage = formatMap.format(Formats.WARNINGS_NOT_FOUND, nameToUnwarn);
      api.sendChatMessages(Collections.singletonList(chatMessage), warner, sendingServer);
    }
    else
    {
      String announcement = formatMap.format(
        Formats.ANNOUNCE_UNWARN, warner, nameToUnwarn, String.valueOf(removeCount));
      announce(announcement, isSilent);
    }
  }

  private void announce(String announcement, boolean isSilent)
  {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
    out.writeBoolean(isSilent);
    out.writeUTF(announcement);

    api.sendToServers(Channels.ANNOUNCEMENT, out.toByteArray());
  }

  private byte[] getBytesForCommandsToRunForWarning(
    String nameToWarn, String message, List<String> commandFormats)
  {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(512);
    out.writeInt(commandFormats.size());

    for (String commandFormat : commandFormats)
    {
      String withNameReplaced = NAME_PATTERN.matcher(commandFormat).replaceAll(nameToWarn);
      String withMessageReplaced = MESSAGE_PATTERN.matcher(withNameReplaced).replaceAll(message);

      out.writeUTF(withMessageReplaced);
    }

    return out.toByteArray();
  }
}
