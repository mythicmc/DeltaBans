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
public class BannedCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public BannedCommand(DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
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
        if(args.length == 0)
        {
            sender.sendMessage(Settings.format("BannedUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.CheckBan"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.CheckBan"));
            return true;
        }

        boolean hasExtraPerm = sender.hasPermission("DeltaBans.CheckBan.Extra");
        boolean isIp = DeltaBansUtils.isIp(args[0]);

        if(isIp && !hasExtraPerm)
        {
            sender.sendMessage(Settings.format("CheckIpNotAllowed"));
            return true;
        }

        String senderName = sender.getName();
        String channelMessage = buildMessage(senderName, args[0], isIp, hasExtraPerm);

        deltaRedisApi.publish(Servers.BUNGEECORD, DeltaBansChannels.BANNED, channelMessage);
        return true;
    }

    private String buildMessage(String banner, String argument, boolean isIp, boolean hasExtra)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(argument);
        out.writeBoolean(isIp);
        out.writeBoolean(hasExtra);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
