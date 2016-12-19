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
package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBans;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class RangeBanCommand implements CommandExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public RangeBanCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("rangeban").setExecutor(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("rangeban").setExecutor(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 2)
        {
            sender.sendMessage(formatUsage("/rangeban <start ip> <end ip> [message]"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.RangeBan"))
        {
            sender.sendMessage(formatNoPerm("DeltaBans.RangeBan"));
            return true;
        }

        if(!DeltaBansUtils.isIp(args[0]))
        {
            sender.sendMessage(format("DeltaBans.InvalidIp", args[0]));
            return true;
        }
        else if(!DeltaBansUtils.isIp(args[1]))
        {
            sender.sendMessage(format("DeltaBans.InvalidIp", args[1]));
            return true;
        }

        String message = null;
        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        long firstAsLong = DeltaBansUtils.convertIpToLong(args[0]);
        long secondAsLong = DeltaBansUtils.convertIpToLong(args[1]);
        String banner = sender.getName();
        DeltaRedisApi api = DeltaRedisApi.instance();

        if(firstAsLong == secondAsLong)
        {
            sender.sendMessage(format(
                "DeltaBans.InvalidIpRange",
                args[0],
                args[1]));
        }
        else if(firstAsLong > secondAsLong)
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.RANGE_BAN,
                banner,
                message,
                args[1],
                args[0],
                isSilent ? "1" : "0");
        }
        else
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.RANGE_BAN,
                banner,
                message,
                args[0],
                args[1],
                isSilent ? "1" : "0");
        }

        return true;
    }
}
