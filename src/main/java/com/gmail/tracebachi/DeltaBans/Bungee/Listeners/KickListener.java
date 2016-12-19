/*
 * This file is part of DeltaBans.
 *
 * DeltaBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaBans.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBans;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansChannels;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.Events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatPlayerOffline;


/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class KickListener implements Listener, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public KickListener(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void unregister()
    {
        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaBansChannels.KICK))
        {
            List<String> messageParts = event.getMessageParts();
            String kicker = messageParts.get(0);
            String nameToKick = messageParts.get(1);
            String message = messageParts.get(2);
            boolean isSilent = messageParts.get(3).equals("1");

            handleKickDeltaRedisMessageEvent(
                event.getSendingServer(),
                kicker,
                nameToKick,
                message,
                isSilent);
        }
    }

    private void handleKickDeltaRedisMessageEvent(String sendingServer, String kicker,
                                                  String nameToKick, String message,
                                                  boolean isSilent)
    {
        if(message == null)
        {
            message = format("DeltaBans.DefaultMessage.Kick");
        }

        ProxiedPlayer playerToKick = plugin.getProxy().getPlayer(nameToKick);

        if(playerToKick != null)
        {
            String kickMessage = format(
                "DeltaBans.KickMessageToPlayer",
                kicker,
                nameToKick,
                message);
            BaseComponent[] textComponent = TextComponent.fromLegacyText(kickMessage);
            playerToKick.disconnect(textComponent);

            String announcement = format(
                "DeltaBans.KickMessageToAnnounce",
                kicker,
                nameToKick,
                message);
            announce(announcement, isSilent);
        }
        else
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                kicker,
                formatPlayerOffline(nameToKick));
        }
    }

    private void announce(String announcement, boolean isSilent)
    {
        if(isSilent)
        {
            DeltaRedisApi.instance().sendServerAnnouncement(
                Servers.SPIGOT,
                format("DeltaBans.SilentPrefix") + announcement,
                "DeltaBans.SeeSilent");
        }
        else
        {
            DeltaRedisApi.instance().sendServerAnnouncement(
                Servers.SPIGOT,
                announcement,
                "");
        }
    }
}
