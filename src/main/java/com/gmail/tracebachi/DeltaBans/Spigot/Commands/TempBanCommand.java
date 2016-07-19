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
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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
            sender.sendMessage(Settings.format("TempBanUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Ban"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.Ban"));
            return true;
        }

        String banner = sender.getName();
        String possibleIp = args[0];
        String name = null;
        String message = Settings.format("DefaultTempBanMessage");
        Long duration = getDuration(args[1]);

        if(banner.equalsIgnoreCase(possibleIp))
        {
            sender.sendMessage(Settings.format("BanSelf"));
            return true;
        }

        if(duration <= 0)
        {
            sender.sendMessage(Settings.format("InvalidDuration", args[1]));
            return true;
        }

        if(args.length > 2)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        if(!DeltaBansUtils.isIp(possibleIp))
        {
            name = possibleIp;
            possibleIp = plugin.getIpOfPlayer(name);

            if(possibleIp == null)
            {
                sender.sendMessage(Settings.format("NoIpFound", name));
                return true;
            }
        }

        String channelMessage = buildMessage(banner, message, possibleIp, duration, name, isSilent);

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.BAN,
            channelMessage);

        return true;
    }

    private String buildMessage(String banner, String banMessage, String ip, long duration,
        String name, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(banMessage);
        out.writeUTF(ip);
        out.writeUTF(Long.toHexString(duration * 1000));
        out.writeBoolean(isSilent);
        out.writeBoolean(name != null);

        if(name != null)
        {
            out.writeUTF(name);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
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
