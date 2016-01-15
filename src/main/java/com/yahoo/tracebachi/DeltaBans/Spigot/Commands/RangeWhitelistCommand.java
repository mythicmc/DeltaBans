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
package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.DeltaBansChannels;
import com.yahoo.tracebachi.DeltaBans.DeltaBansUtils;
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class RangeWhitelistCommand extends DeltaBansCommand
{
    private DeltaRedisApi deltaRedisApi;

    public RangeWhitelistCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("rangewhitelist", "DeltaBans.RangeBan", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void onShutdown()
    {
        this.deltaRedisApi = null;
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 2)
        {
            sender.sendMessage(Prefixes.INFO + "/rangewhitelist <add|remove> <name>");
            return;
        }

        String nameToUpdate = args[1];

        if(args[0].equalsIgnoreCase("add"))
        {
            String channelMessage = buildChannelMessage(sender.getName(), nameToUpdate, true);
            deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.RANGE_WHITELIST, channelMessage);
        }
        else if(args[0].equalsIgnoreCase("remove"))
        {
            String channelMessage = buildChannelMessage(sender.getName(), nameToUpdate, false);
            deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.RANGE_WHITELIST, channelMessage);
        }
        else
        {
            sender.sendMessage(Prefixes.INFO + "/rangewhitelist <add|remove> <name>");
            return;
        }
    }

    private String buildChannelMessage(String senderName, String nameToUpdate, boolean isAdd)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(senderName);
        out.writeUTF(nameToUpdate);
        out.writeBoolean(isAdd);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
