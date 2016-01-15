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
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class RangeBanCommand extends DeltaBansCommand
{
    private static final Pattern DASH_PATTERN = Pattern.compile("-");

    private String defaultRangeBanMessage;
    private DeltaRedisApi deltaRedisApi;

    public RangeBanCommand(String defaultRangeBanMessage, DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("rangeban", "DeltaBans.RangeBan", plugin);
        this.defaultRangeBanMessage = defaultRangeBanMessage;
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void onShutdown()
    {
        this.deltaRedisApi = null;
        this.defaultRangeBanMessage = null;
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/rangeban <ip>-<ip> [message]");
            return;
        }

        String banner = sender.getName();
        String message = defaultRangeBanMessage;
        String[] splitIpRange = DASH_PATTERN.split(args[0]);

        if(splitIpRange.length != 2)
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(args[0]) + " is not a valid IP range.");
            return;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[0]))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(splitIpRange[0]) + " is not a valid IP.");
            return;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[1]))
        {
            sender.sendMessage(Prefixes.FAILURE + Prefixes.input(splitIpRange[1]) + " is not a valid IP.");
            return;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        long firstAsLong = DeltaBansUtils.convertIpToLong(splitIpRange[0]);
        long secondAsLong = DeltaBansUtils.convertIpToLong(splitIpRange[1]);

        if(firstAsLong == secondAsLong)
        {
            sender.sendMessage(Prefixes.FAILURE + "Use an IP ban instead.");
        }
        else if(firstAsLong > secondAsLong)
        {
            String channelMessage = buildChannelMessage(banner, message,
                splitIpRange[1], splitIpRange[0], isSilent);
            deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.RANGE_BAN, channelMessage);
        }
        else
        {
            String channelMessage = buildChannelMessage(banner, message,
                splitIpRange[0], splitIpRange[1], isSilent);
            deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.RANGE_BAN, channelMessage);
        }
    }

    private String buildChannelMessage(String name, String message, String start, String end, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(name);
        out.writeUTF(message);
        out.writeUTF(start);
        out.writeUTF(end);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
