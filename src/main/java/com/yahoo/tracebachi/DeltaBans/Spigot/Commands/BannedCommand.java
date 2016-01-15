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
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BannedCommand extends DeltaBansCommand
{
    private DeltaRedisApi deltaRedisApi;

    public BannedCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("banned", "DeltaBans.CheckBan", plugin);
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void onShutdown()
    {
        this.deltaRedisApi = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean hasExtraPerm = sender.hasPermission("DeltaBans.CheckBan.Extra");

        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/banned <name|ip>");
            return;
        }

        String senderName = sender.getName();
        String argument = args[0];
        boolean isIp = DeltaBansUtils.isIp(argument);

        if(isIp && !hasExtraPerm)
        {
            sender.sendMessage(Prefixes.FAILURE + "You are not allowed to check IP bans.");
        }
        else
        {
            String channelMessage = buildChannelMessage(senderName, argument, isIp, hasExtraPerm);
            deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.BANNED, channelMessage);
        }
    }

    private String buildChannelMessage(String banner, String argument, boolean isIp, boolean hasExtra)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(argument);
        out.writeBoolean(isIp);
        out.writeBoolean(hasExtra);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
