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

import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBans;
import com.gmail.tracebachi.DeltaBans.Spigot.Settings;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class RangeBanCommand implements CommandExecutor, Registerable, Shutdownable
{
    private static final Pattern DASH_PATTERN = Pattern.compile("-");

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

        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("RangeBanUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.RangeBan"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.RangeBan"));
            return true;
        }

        String banner = sender.getName();
        String[] splitIpRange = DASH_PATTERN.split(args[0]);
        String message = Settings.format("DefaultRangeBanMessage");

        if(splitIpRange.length != 2)
        {
            sender.sendMessage(Settings.format("InvalidRange", args[0]));
            return true;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[0]))
        {
            sender.sendMessage(Settings.format("InvalidIp", splitIpRange[0]));
            return true;
        }

        if(!DeltaBansUtils.isIp(splitIpRange[1]))
        {
            sender.sendMessage(Settings.format("InvalidIp", splitIpRange[1]));
            return true;
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
            sender.sendMessage(Settings.format("InvalidRange", args[0]));
            return true;
        }

        DeltaRedisApi api = DeltaRedisApi.instance();

        if(firstAsLong > secondAsLong)
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.RANGE_BAN,
                banner,
                message,
                splitIpRange[1],
                splitIpRange[0],
                isSilent ? "1" : "0");
        }
        else
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.RANGE_BAN,
                banner,
                message,
                splitIpRange[0],
                splitIpRange[1],
                isSilent ? "1" : "0");
        }

        return true;
    }
}
