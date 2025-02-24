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

import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.Velocity.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Velocity.Events.KickEvent;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Collections;
import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class KickListener implements Registerable
{
  private final DeltaBansPlugin plugin;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;
  private final Consumer<ReceivedMessage> kickChannelListener;

  public KickListener(
          DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.api = api;
    this.formatMap = formatMap;
    this.kickChannelListener = this::onKickChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.KICK, kickChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.KICK, kickChannelListener);
  }

  private void onKickChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    String sendingServer = in.readUTF();
    String kicker = in.readUTF();
    String nameToKick = in.readUTF();
    String message = in.readUTF();
    boolean isSilent = in.readBoolean();

    message = message.equals("") ? formatMap.format(Formats.DEFAULT_MESSAGE_KICK) : message;

    Player playerToKick = plugin.getServer().getPlayer(nameToKick).orElse(null);

    if (playerToKick == null)
    {
      String chatMessage = formatMap.format(Formats.PLAYER_OFFLINE, nameToKick);
      api.sendChatMessages(Collections.singletonList(chatMessage), kicker, sendingServer);
      return;
    }

    // Kick with message
    String kickMessage = formatMap.format(Formats.KICK_MESSAGE, kicker, nameToKick, message);
    playerToKick.disconnect(LegacyComponentSerializer.legacySection().deserialize(kickMessage));

    // Announce kick
    String announcement = formatMap.format(Formats.ANNOUNCE_KICK, kicker, nameToKick, message);
    announce(announcement, isSilent);

    // fire event
    plugin.getServer().getEventManager().fire(new KickEvent(kicker, nameToKick, message, isSilent));
  }

  private void announce(String announcement, boolean isSilent)
  {
    ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
    out.writeBoolean(isSilent);
    out.writeUTF(announcement);

    api.sendToServers(Channels.ANNOUNCEMENT, out.toByteArray());
  }
}
