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
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class TempBanCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public TempBanCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("tempban").setExecutor(this);
        plugin.getCommand("tempban").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("tempban").setExecutor(null);
        plugin.getCommand("tempban").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
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
            sender.sendMessage(formatUsage("/tempban <name|ip> <duration> [message]"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Ban"))
        {
            sender.sendMessage(formatNoPerm("DeltaBans.Ban"));
            return true;
        }

        String banner = sender.getName();
        String ip = args[0];
        if(banner.equalsIgnoreCase(ip))
        {
            sender.sendMessage(format("DeltaBans.NotAllowedOnSelf", "tempban"));
            return true;
        }

        Long duration = getDuration(args[1]);
        if(duration <= 0)
        {
            sender.sendMessage(format("DeltaBans.InvalidDuration", args[1]));
            return true;
        }

        String name = null;
        if(!DeltaBansUtils.isIp(ip))
        {
            name = ip;
            ip = plugin.getIpOfPlayer(name);

            if(ip == null)
            {
                sender.sendMessage(format("DeltaBans.NoIpFound", name));
                return true;
            }
        }

        String message = null;
        if(args.length > 2)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.BAN,
            name == null ? "" : name,
            ip == null ? "" : ip,
            banner,
            message == null ? "" : message,
            Long.toHexString(duration * 1000),
            isSilent ? "1" : "0");
        return true;
    }

    private long getDuration(String input)
    {
        int multiplier = 1;
        boolean hasEndingChar = true;

        switch(input.charAt(input.length() - 1))
        {
            case 's':
                multiplier = 1;
                break;
            case 'm':
                multiplier = 60;
                break;
            case 'h':
                multiplier = 60 * 60;
                break;
            case 'd':
                multiplier = 60 * 60 * 24;
                break;
            case 'w':
                multiplier = 60 * 60 * 24 * 7;
                break;
            default:
                hasEndingChar = false;
                break;
        }

        if(hasEndingChar)
        {
            input = input.substring(0, input.length() - 1);
        }

        try
        {
            int value = Integer.parseInt(input) * multiplier;
            return (value > 0) ? value : 0;
        }
        catch(NumberFormatException ex)
        {
            return 0;
        }
    }
}
