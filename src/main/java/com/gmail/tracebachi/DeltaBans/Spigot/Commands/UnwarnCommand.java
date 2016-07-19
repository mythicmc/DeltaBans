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
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class UnwarnCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public UnwarnCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("unwarn").setExecutor(this);
        plugin.getCommand("unwarn").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("unwarn").setExecutor(null);
        plugin.getCommand("unwarn").setTabCompleter(null);
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

        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("UnwarnUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Warn"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.Warn"));
            return true;
        }

        String warner = sender.getName();
        String name = args[0];
        Integer amount = 1;

        if(args.length >= 2)
        {
            amount = parseInt(args[1]);
            amount = (amount == null) ? 1 : amount;
        }

        String channelMessage = buildMessage(warner, name, amount, isSilent);

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.UNWARN,
            channelMessage);

        return true;
    }

    private String buildMessage(String warner, String name, Integer amount, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(warner);
        out.writeUTF(name);
        out.writeUTF(Integer.toHexString(amount));
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private Integer parseInt(String source)
    {
        try
        {
            return Integer.parseInt(source);
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }
}
