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
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class KickCommand implements TabExecutor, Listener, Registerable, Shutdownable
{
    private DeltaBans plugin;

    public KickCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("kick").setExecutor(this);
        plugin.getCommand("kick").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("kick").setExecutor(null);
        plugin.getCommand("kick").setTabCompleter(null);
        HandlerList.unregisterAll(this);
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
        boolean isSilent = DeltaBansUtils.isSilent(args);

        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Settings.format("KickUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Kick"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.Kick"));
            return true;
        }

        String kicker = sender.getName();
        String nameToKick = args[0];
        String message = Settings.format("DefaultKickMessage");

        if(kicker.equalsIgnoreCase(nameToKick))
        {
            sender.sendMessage(Settings.format("KickSelf"));
            return true;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        final String finalMessage = message;

        DeltaRedisApi.instance().findPlayer(nameToKick, cachedPlayer ->
        {
            if(cachedPlayer == null)
            {
                sendMessage(kicker, Settings.format("NotOnline", nameToKick));
                return;
            }

            DeltaRedisApi.instance().publish(
                Servers.SPIGOT,
                DeltaBansChannels.KICK,
                kicker,
                nameToKick,
                finalMessage,
                isSilent ? "1" : "0");
        });

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaBansChannels.KICK))
        {
            String[] split = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String kicker = split[0];
            String nameToKick = split[1];
            String message = split[2];
            boolean isSilent = split[3].equals("1");
            Player toKick = Bukkit.getPlayerExact(nameToKick);

            if(toKick != null)
            {
                String kickMessageToPlayer = Settings.format(
                    "KickMessageToPlayer",
                    kicker,
                    nameToKick,
                    message);

                toKick.kickPlayer(kickMessageToPlayer);
                announceKick(kicker, nameToKick, message, isSilent);
            }
        }
    }

    private void announceKick(String kicker, String nameToKick, String message, boolean isSilent)
    {
        String kickAnnounce = Settings.format(
            "KickMessageToAnnounce",
            kicker,
            nameToKick,
            message);

        DeltaRedisApi.instance().sendAnnouncementToServer(
            Servers.SPIGOT,
            kickAnnounce,
            isSilent ? "DeltaBans.SeeSilent" : "");
    }

    private void sendMessage(String senderName, String message)
    {
        if(senderName.equals("CONSOLE"))
        {
            Bukkit.getConsoleSender().sendMessage(message);
        }
        else
        {
            Player player = Bukkit.getPlayerExact(senderName);

            if(player != null)
            {
                player.sendMessage(message);
            }
        }
    }
}
