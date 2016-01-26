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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
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
public class NameBanCommand implements TabExecutor, Registerable, Shutdownable
{
    private String defaultBanMessage;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public NameBanCommand(String defaultBanMessage, DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.defaultBanMessage = defaultBanMessage;
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("nameban").setExecutor(this);
        plugin.getCommand("nameban").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("nameban").setExecutor(null);
        plugin.getCommand("nameban").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        defaultBanMessage = null;
        deltaRedisApi = null;
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfPlayerName(lastArg);
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
            sender.sendMessage(Prefixes.INFO + "/nameban <name|ip> [message]");
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Ban"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaBans.Ban") + " permission.");
            return true;
        }

        String banner = sender.getName();
        String banee = args[0];

        if(banner.equalsIgnoreCase(banee))
        {
            sender.sendMessage(Prefixes.FAILURE + "Why are you trying to ban yourself?");
            return true;
        }

        if(DeltaBansUtils.isIp(banee))
        {
            sender.sendMessage(Prefixes.FAILURE + "Only names can be banned with /nameban");
            return true;
        }

        String message = defaultBanMessage;
        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        String channelMessage = buildChannelMessage(banner, message, banee, isSilent);
        deltaRedisApi.publish(Servers.BUNGEECORD, DeltaBansChannels.NAME_BAN, channelMessage);
        return true;
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
