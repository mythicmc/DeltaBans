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

import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.Velocity.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collections;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class WhitelistListener implements Registerable
{
  private final DeltaBansPlugin plugin;
  private final WhitelistStorage whitelistStorage;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> whitelistEditChannelListener;
  private final Consumer<ReceivedMessage> whitelistToggleChannelListener;
  private boolean whitelistEnabled;

  public WhitelistListener(
          DeltaBansPlugin plugin, WhitelistStorage whitelistStorage, SockExchangeApi api,
          MessageFormatMap formatMap, boolean shouldStartWithWhitelist)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(whitelistStorage, "whitelistStorage");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.whitelistStorage = whitelistStorage;
    this.api = api;
    this.formatMap = formatMap;
    this.whitelistEditChannelListener = this::onWhitelistEditChannelRequest;
    this.whitelistToggleChannelListener = this::onWhitelistToggleChannelRequest;
    this.whitelistEnabled = shouldStartWithWhitelist;
  }

  @Override
  public void register()
  {
    plugin.getServer().getEventManager().register(plugin, this);

    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.register(Channels.WHITELIST_EDIT, whitelistEditChannelListener);
    messageNotifier.register(Channels.WHITELIST_TOGGLE, whitelistToggleChannelListener);
  }

  @Override
  public void unregister()
  {
    plugin.getServer().getEventManager().unregisterListener(plugin, this);

    ReceivedMessageNotifier messageNotifier = api.getMessageNotifier();
    messageNotifier.unregister(Channels.WHITELIST_EDIT, whitelistEditChannelListener);
    messageNotifier.unregister(Channels.WHITELIST_TOGGLE, whitelistToggleChannelListener);
  }

  @Subscribe
  public void onLogin(LoginEvent event)
  {
    if (!event.getResult().isAllowed())
    {
      return;
    }

    Player pending = event.getPlayer();
    String playerName = pending.getUsername();

    if (whitelistEnabled && !whitelistStorage.isOnNormalWhitelist(playerName))
    {
      event.setResult(LoginEvent.ComponentResult.denied(LegacyComponentSerializer.legacySection()
              .deserialize(formatMap.format(Formats.SERVER_IN_WHITELIST_MODE))));
      event.getPlayer().disconnect(LegacyComponentSerializer.legacySection()
              .deserialize(formatMap.format(Formats.SERVER_IN_WHITELIST_MODE)));
    }
  }

  private void onWhitelistEditChannelRequest(ReceivedMessage message)
  {
    ByteArrayDataInput in = message.getDataInput();
    String sendingServer = in.readUTF();
    String sender = in.readUTF();
    String whitelistType = in.readUTF().toLowerCase();
    String nameToEdit = in.readUTF();
    boolean isAdd = in.readBoolean();

    if (whitelistType.equals("normal"))
    {
      if (isAdd)
      {
        whitelistStorage.addToNormalWhitelist(nameToEdit);

        String chatMessage = formatMap.format(
          Formats.ADDED_TO_WHITELIST, nameToEdit, whitelistType);
        sendChatMessage(chatMessage, sender, sendingServer);
      }
      else
      {
        whitelistStorage.removeFromNormalWhitelist(nameToEdit);

        String chatMessage = formatMap.format(
          Formats.REMOVED_FROM_WHITELIST, nameToEdit, whitelistType);
        sendChatMessage(chatMessage, sender, sendingServer);
      }
    }
    else if (whitelistType.equals("rangeban"))
    {
      if (isAdd)
      {
        whitelistStorage.addToRangeBanWhitelist(nameToEdit);

        String chatMessage = formatMap.format(
          Formats.ADDED_TO_WHITELIST, nameToEdit, whitelistType);
        sendChatMessage(chatMessage, sender, sendingServer);
      }
      else
      {
        whitelistStorage.removeFromRangeBanWhitelist(nameToEdit);

        String chatMessage = formatMap.format(
          Formats.REMOVED_FROM_WHITELIST, nameToEdit, whitelistType);
        sendChatMessage(chatMessage, sender, sendingServer);
      }
    }
  }

  private void onWhitelistToggleChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String sender = in.readUTF();
    boolean enable = in.readBoolean();

    if (enable)
    {
      whitelistEnabled = true;
      sendChatMessage(
        formatMap.format(Formats.WHITELIST_TOGGLED, "enabled"), sender, sendingServer);
    }
    else
    {
      whitelistEnabled = false;
      sendChatMessage(
        formatMap.format(Formats.WHITELIST_TOGGLED, "disabled"), sender, sendingServer);
    }
  }

  private void sendChatMessage(String message, String playerName, String serverName)
  {
    api.sendChatMessages(Collections.singletonList(message), playerName, serverName);
  }
}
