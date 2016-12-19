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
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansChannels;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.Events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/18/16.
 */
public class WhitelistListener implements Listener, Registerable, Shutdownable
{
    private boolean whitelistEnabled;
    private WhitelistStorage whitelistStorage;
    private DeltaBans plugin;

    public WhitelistListener(DeltaBans plugin)
    {
        this.plugin = plugin;
        this.whitelistStorage = plugin.getWhitelistStorage();
        this.whitelistEnabled = Settings.shouldStartWithWhitelistEnabled();
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
        whitelistStorage = null;
        plugin = null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(LoginEvent event)
    {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName();

        if(whitelistEnabled && !whitelistStorage.isOnNormalWhitelist(playerName))
        {
            event.setCancelReason(format("DeltaBans.WhitelistMessage"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        List<String> messageParts = event.getMessageParts();

        if(channel.equals(DeltaBansChannels.WHITELIST_TOGGLE))
        {
            String sender = messageParts.get(0);
            boolean enable = messageParts.get(1).equals("1");

            handleWhitelistToggleDeltaRedisMessageEvent(
                event.getSendingServer(),
                sender,
                enable);
        }
        else if(channel.equals(DeltaBansChannels.WHITELIST_EDIT))
        {
            String sender = messageParts.get(0);
            String name = messageParts.get(1);
            boolean isAdd = messageParts.get(2).equals("1");

            handleWhitelistEditDeltaRedisMessageEvent(
                event.getSendingServer(),
                sender,
                isAdd,
                name);
        }
        else if(channel.equals(DeltaBansChannels.RANGEBAN_WHITELIST_EDIT))
        {
            String sender = messageParts.get(0);
            String name = messageParts.get(1);
            boolean isAdd = messageParts.get(2).equals("1");

            handleRangeBanWhitelistEditDeltaRedisMessageEvent(
                event.getSendingServer(),
                sender,
                isAdd,
                name);
        }
    }

    private void handleRangeBanWhitelistEditDeltaRedisMessageEvent(String sendingServer,
                                                                   String sender, boolean isAdd,
                                                                   String name)
    {
        if(isAdd)
        {
            if(whitelistStorage.addToRangeBanWhitelist(name))
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.AddedToWhitelist", name, "rangeban"));
            }
            else
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.AlreadyInWhitelist", name, "rangeban"));
            }
        }
        else
        {
            if(whitelistStorage.removeFromRangeBanWhitelist(name))
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.RemovedFromWhitelist", name, "rangeban"));
            }
            else
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.NotInWhitelist", name, "rangeban"));
            }
        }
    }

    private void handleWhitelistEditDeltaRedisMessageEvent(String sendingServer, String sender,
                                                           boolean isAdd, String name)
    {
        if(isAdd)
        {
            if(whitelistStorage.addToNormalWhitelist(name))
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.AddedToWhitelist", name, "normal"));
            }
            else
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.AlreadyInWhitelist", name, "normal"));
            }
        }
        else
        {
            if(whitelistStorage.removeFromNormalWhitelist(name))
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.RemovedFromWhitelist", name, "normal"));
            }
            else
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.NotInWhitelist", name, "normal"));
            }
        }
    }

    private void handleWhitelistToggleDeltaRedisMessageEvent(String sendingServer, String sender,
                                                             boolean enable)
    {
        if(enable)
        {
            whitelistEnabled = true;
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                sender,
                format("DeltaBans.WhitelistToggle", "enabled"));
        }
        else
        {
            whitelistEnabled = false;
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                sender,
                format("DeltaBans.WhitelistToggle", "disabled"));
        }
    }
}
