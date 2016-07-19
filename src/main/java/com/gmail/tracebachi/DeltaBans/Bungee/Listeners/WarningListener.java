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
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class WarningListener implements Listener, Registerable, Shutdownable
{
    private WarningStorage warningStorage;
    private DeltaBans plugin;

    public WarningListener(WarningStorage warningStorage, DeltaBans plugin)
    {
        this.warningStorage = warningStorage;
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
        warningStorage = null;
        plugin = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        DeltaRedisApi api = DeltaRedisApi.instance();
        String channel = event.getChannel();
        byte[] messageBytes = event.getMessage().getBytes(StandardCharsets.UTF_8);
        ByteArrayDataInput in = ByteStreams.newDataInput(messageBytes);

        if(channel.equals(DeltaBansChannels.WARN))
        {
            String warner = in.readUTF();
            String name = in.readUTF();
            String message = in.readUTF();
            boolean isSilent = in.readBoolean();
            int count = warningStorage.add(name, new WarningEntry(warner, message));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(warner);
            out.writeUTF(name);
            out.writeUTF(message);
            out.writeUTF(Integer.toHexString(count));

            api.publish(
                event.getSendingServer(),
                DeltaBansChannels.WARN,
                new String(out.toByteArray(), StandardCharsets.UTF_8));

            String announcement = Settings.format("WarnAnnouncement", warner, name, message);

            if(isSilent)
            {
                api.sendAnnouncementToServer(
                    Servers.SPIGOT,
                    Settings.format("SilentPrefix") + announcement,
                    "DeltaBans.SeeSilent");
            }
            else
            {
                api.sendAnnouncementToServer(
                    Servers.SPIGOT,
                    announcement,
                    "");
            }
        }
        else if(channel.equalsIgnoreCase(DeltaBansChannels.UNWARN))
        {
            String warner = in.readUTF();
            String name = in.readUTF();
            int amount = Integer.parseInt(in.readUTF(), 16);
            boolean isSilent = in.readBoolean();

            amount = warningStorage.remove(name, amount);

            String announcement = Settings.format("RemovedWarnings",
                warner, String.valueOf(amount), name);

            if(isSilent)
            {
                api.sendAnnouncementToServer(
                    Servers.SPIGOT,
                    Settings.format("SilentPrefix") + announcement,
                    "DeltaBans.SeeSilent");
            }
            else
            {
                api.sendAnnouncementToServer(
                    Servers.SPIGOT,
                    announcement,
                    "");
            }
        }
    }
}
