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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.Events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class WarningListener implements Listener, Registerable, Shutdownable
{
    private WarningStorage warningStorage;
    private ScheduledTask scheduledTask;
    private DeltaBans plugin;

    public WarningListener(WarningStorage warningStorage, DeltaBans plugin)
    {
        this.plugin = plugin;
        this.warningStorage = warningStorage;
    }

    @Override
    public void register()
    {
        ProxyServer proxy = plugin.getProxy();
        proxy.getPluginManager().registerListener(plugin, this);

        scheduledTask = proxy.getScheduler().schedule(plugin,
            warningStorage::removeExpired,
            10, // Delay
            10, // Period
            TimeUnit.MINUTES);
    }

    @Override
    public void unregister()
    {
        ProxyServer proxy = plugin.getProxy();
        proxy.getPluginManager().unregisterListener(this);
        proxy.getScheduler().cancel(scheduledTask);
    }

    @Override
    public void shutdown()
    {
        unregister();
        warningStorage = null;
        scheduledTask = null;
        plugin = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        DeltaRedisApi api = DeltaRedisApi.instance();
        String channel = event.getChannel();
        List<String> messageParts = event.getMessageParts();

        if(channel.equals(DeltaBansChannels.WARN))
        {
            String warner = messageParts.get(0);
            String name = messageParts.get(1);
            String message = messageParts.get(2);
            boolean isSilent = messageParts.get(3).equals("1");

            int count = warningStorage.addWarning(new WarningEntry(name, warner, message));

            api.publish(
                event.getSendingServer(),
                DeltaBansChannels.WARN,
                warner,
                name,
                message,
                Integer.toHexString(count));

            String announcement = format(
                "DeltaBans.WarnAnnouncement",
                warner,
                name,
                message);

            if(isSilent)
            {
                api.sendServerAnnouncement(
                    Servers.SPIGOT,
                    format("DeltaBans.SilentPrefix") + announcement,
                    "DeltaBans.SeeSilent");
            }
            else
            {
                api.sendServerAnnouncement(
                    Servers.SPIGOT,
                    announcement,
                    "");
            }
        }
        else if(channel.equalsIgnoreCase(DeltaBansChannels.UNWARN))
        {
            String warner = messageParts.get(0);
            String name = messageParts.get(1);
            int amount = Integer.parseInt(messageParts.get(2), 16);
            boolean isSilent = messageParts.get(3).equals("1");

            int amountActuallyRemoved = warningStorage.removeWarning(name, amount);

            String announcement = format(
                "DeltaBans.UnwarnAnnouncement",
                warner,
                String.valueOf(amountActuallyRemoved),
                name);

            if(isSilent)
            {
                api.sendServerAnnouncement(
                    Servers.SPIGOT,
                    format("DeltaBans.SilentPrefix") + announcement,
                    "DeltaBans.SeeSilent");
            }
            else
            {
                api.sendServerAnnouncement(
                    Servers.SPIGOT,
                    announcement,
                    "");
            }
        }
    }
}
