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
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class NameBanCommand extends DeltaBansCommand
{
    private String defaultBanMessage;
    private DeltaRedisApi deltaRedisApi;

    public NameBanCommand(String defaultBanMessage, DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("nameban", "DeltaBans.Ban", plugin);
        this.defaultBanMessage = defaultBanMessage;
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void onShutdown()
    {
        this.deltaRedisApi = null;
        this.defaultBanMessage = null;
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
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/nameban <name|ip> [message]");
            return;
        }

        String banner = sender.getName();
        String banee = args[0];

        if(banner.equalsIgnoreCase(banee))
        {
            sender.sendMessage(Prefixes.FAILURE + "Why are you trying to ban yourself?");
            return;
        }

        if(DeltaBansUtils.isIp(banee))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only names can be banned with /nameban");
            return;
        }

        String message = defaultBanMessage;
        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        String channelMessage = buildChannelMessage(banner, message, banee, isSilent);
        deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.NAME_BAN, channelMessage);
    }

    private String buildChannelMessage(String banner, String banMessage, String name, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(banMessage);
        out.writeUTF(name);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
