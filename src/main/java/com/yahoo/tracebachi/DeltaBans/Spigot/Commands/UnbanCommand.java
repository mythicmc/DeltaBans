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
public class UnbanCommand extends DeltaBansCommand
{
    private DeltaRedisApi deltaRedisApi;

    public UnbanCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("unban", "DeltaBans.Ban", plugin);
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
        boolean isSilent = DeltaBansPlugin.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansPlugin.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/unban <name|ip>");
            return;
        }

        String banner = sender.getName();
        String banee = args[0];
        boolean isIp = DeltaBansPlugin.isIp(banee);

        if(banee.equals(banner))
        {
            sender.sendMessage(Prefixes.FAILURE + "You are already unbanned.");
            return;
        }

        String channelMessage = buildChannelMessage(banner, banee, isIp, isSilent);
        deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.UNBAN, channelMessage);
    }

    private String buildChannelMessage(String sender, String banee, boolean isIp, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(sender);
        out.writeUTF(banee);
        out.writeBoolean(isIp);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
