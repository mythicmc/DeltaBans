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
package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
class AnnouncementListener implements Registerable
{
  private static final String SEE_SILENT_PERM = "DeltaBans.SeeSilent";

  private final DeltaBansPlugin plugin;
  private final SockExchangeApi api;
  private final String silentAnnouncementPrefix;
  private final Consumer<ReceivedMessage> announcementChannelListener;

  public AnnouncementListener(DeltaBansPlugin plugin, SockExchangeApi api, String silentAnnouncementPrefix)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(silentAnnouncementPrefix, "silentAnnouncementPrefix");

    this.plugin = plugin;
    this.api = api;
    this.silentAnnouncementPrefix = silentAnnouncementPrefix;
    this.announcementChannelListener = this::onAnnouncementChannelRequest;
  }

  @Override
  public void register()
  {
    api.getMessageNotifier().register(Channels.ANNOUNCEMENT, announcementChannelListener);
  }

  @Override
  public void unregister()
  {
    api.getMessageNotifier().unregister(Channels.ANNOUNCEMENT, announcementChannelListener);
  }

  private void onAnnouncementChannelRequest(ReceivedMessage receivedMessage)
  {
    ByteArrayDataInput in = receivedMessage.getDataInput();
    boolean isSilent = in.readBoolean();
    String announcement = in.readUTF();

    if (isSilent)
    {
      announcement = silentAnnouncementPrefix + announcement;
    }

    for (Player player : plugin.getServer().getOnlinePlayers())
    {
      if (!isSilent || player.hasPermission(SEE_SILENT_PERM))
      {
        player.sendMessage(announcement);
      }
    }

    plugin.getServer().getConsoleSender().sendMessage(announcement);
  }
}
