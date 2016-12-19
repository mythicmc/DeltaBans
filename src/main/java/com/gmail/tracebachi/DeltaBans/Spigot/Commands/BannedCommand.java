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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.*;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class BannedCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public BannedCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("banned").setExecutor(this);
        plugin.getCommand("banned").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("banned").setExecutor(null);
        plugin.getCommand("banned").setTabCompleter(null);
    }

    @Override
    public void shutdown()
    {
        unregister();
        plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String s, String[] args)
    {
        String lastArg = args[args.length - 1];
        return DeltaRedisApi.instance().matchStartOfPlayerName(lastArg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length == 0)
        {
            sender.sendMessage(formatUsage("/banned <name|ip>"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.CheckBan"))
        {
            sender.sendMessage(formatNoPerm("DeltaBans.CheckBan"));
            return true;
        }

        boolean hasExtraPerm = sender.hasPermission("DeltaBans.CheckBan.IncludeIp");
        boolean isIp = DeltaBansUtils.isIp(args[0]);

        if(isIp && !hasExtraPerm)
        {
            sender.sendMessage(formatNoPerm("DeltaBans.CheckBan.IncludeIp"));
            return true;
        }

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.BANNED,
            sender.getName(),
            args[0],
            isIp ? "1" : "0",
            hasExtraPerm ? "1" : "0");
        return true;
    }
}
