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
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBans;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatNoPerm;
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatUsage;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class WhitelistCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public WhitelistCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("whitelist").setExecutor(this);
        plugin.getCommand("whitelist").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("whitelist").setExecutor(null);
        plugin.getCommand("whitelist").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length < 1)
        {
            sender.sendMessage(formatUsage("/whitelist <on|off|add|remove> <name>"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Whitelist"))
        {
            sender.sendMessage(formatNoPerm("DeltaBans.Whitelist"));
            return true;
        }

        DeltaRedisApi api = DeltaRedisApi.instance();

        if(args[0].equalsIgnoreCase("on"))
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.WHITELIST_TOGGLE,
                sender.getName(),
                "1");
        }
        else if(args[0].equalsIgnoreCase("off"))
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.WHITELIST_TOGGLE,
                sender.getName(),
                "0");
        }
        else if(args.length > 1 && args[0].equalsIgnoreCase("add"))
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.WHITELIST_EDIT,
                sender.getName(),
                "1",
                args[1]);
        }
        else if(args.length > 1 && args[0].equalsIgnoreCase("remove"))
        {
            api.publish(
                Servers.BUNGEECORD,
                DeltaBansChannels.WHITELIST_EDIT,
                sender.getName(),
                "0",
                args[1]);
        }
        else
        {
            sender.sendMessage(formatUsage("/whitelist <on|off|add|remove> <name>"));
        }

        return true;
    }
}
