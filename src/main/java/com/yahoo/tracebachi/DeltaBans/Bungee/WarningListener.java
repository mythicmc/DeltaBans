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
package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Bungee.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class WarningListener implements Listener
{
    private static final String ADD_WARN_CHANNEL = "DB-AddWarning";
    private static final String CHECK_WARN_CHANNEL = "DB-CheckWarning";
    private static final String PARDON_WARN_CHANNEL = "DB-PardonWarning";
    private static final String ANNOUNCE = "DB-Announce";

    private WarningStorage warningStorage;
    private DeltaRedisApi deltaRedisApi;

    public WarningListener(DeltaRedisApi deltaRedisApi, WarningStorage warningStorage)
    {
        this.warningStorage = warningStorage;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.warningStorage = null;
        this.deltaRedisApi = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getMessage().getBytes(StandardCharsets.UTF_8));

        if(channel.equals(ADD_WARN_CHANNEL))
        {
            String warner = in.readUTF();
            String name = in.readUTF();
            String message = in.readUTF();
            int count = warningStorage.add(name, new WarningEntry(message));

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF(name);
            out.writeUTF(Integer.toHexString(count));

            deltaRedisApi.publish(event.getSender(), ADD_WARN_CHANNEL,
                new String(out.toByteArray(), StandardCharsets.UTF_8));

            deltaRedisApi.publish(Channels.SPIGOT, ANNOUNCE,
                formatWarnAnnouncement(warner, name, message));
        }
        else if(channel.equalsIgnoreCase(PARDON_WARN_CHANNEL))
        {
            String pardoner = in.readUTF();
            String name = in.readUTF();
            boolean all = in.readBoolean();

            if(all)
            {
                warningStorage.removeAll(name);
                deltaRedisApi.sendMessageToPlayer(event.getSender(), pardoner,
                    Prefixes.SUCCESS + "Pardoned all warnings for " + name);
            }
            else
            {
                warningStorage.removeOne(name);
                deltaRedisApi.sendMessageToPlayer(event.getSender(), pardoner,
                    Prefixes.SUCCESS + "Pardoned one warning for " + name);
            }
        }
        else if(channel.equalsIgnoreCase(CHECK_WARN_CHANNEL))
        {
            String checker = in.readUTF();
            String name = in.readUTF();
            List<WarningEntry> warnings = warningStorage.getWarnings(name);
            StringBuilder builder = new StringBuilder(Prefixes.INFO + "Warnings for " +
                Prefixes.input(name));

            for(WarningEntry entry : warnings)
            {
                builder.append("\n");
                builder.append(" - ").append(entry.getMessage());
            }

            deltaRedisApi.sendMessageToPlayer(event.getSender(), checker, builder.toString());
        }
    }

    private String formatWarnAnnouncement(String warner, String name, String message)
    {
        return ChatColor.GOLD + warner +
            ChatColor.WHITE + " warned " +
            ChatColor.GOLD + name +
            ChatColor.WHITE + " for " +
            ChatColor.GOLD + message;
    }
}
