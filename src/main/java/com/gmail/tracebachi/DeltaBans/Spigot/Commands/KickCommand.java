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
import com.gmail.tracebachi.DeltaRedis.Shared.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
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

import java.util.Arrays;
import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent.DELTA_PATTERN;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class KickCommand implements TabExecutor, Registerable, Shutdownable
{
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public KickCommand(DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getCommand("kick").setExecutor(this);
        plugin.getCommand("kick").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("kick").setExecutor(null);
        plugin.getCommand("kick").setTabCompleter(null);
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
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/kick <name> [message]");
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Kick"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have the " +
                Prefixes.input("DeltaBans.Kick") + " permission.");
            return true;
        }

        String kicker = sender.getName();
        String nameToKick = args[0];
        Settings settings = plugin.getSettings();
        String message = settings.format("DefaultKickMessage");

        if(kicker.equalsIgnoreCase(nameToKick))
        {
            sender.sendMessage(Prefixes.FAILURE + "Why are you trying to kick yourself?");
            return true;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        Player toKick = Bukkit.getPlayer(nameToKick);

        if(toKick != null)
        {
            String kickPlayer = settings.format("KickMessageToPlayer", kicker, nameToKick, message);

            toKick.kickPlayer(kickPlayer);
            announceKick(settings, kicker, nameToKick, message, isSilent);

            return true;
        }

        final String finalMessage = message;

        deltaRedisApi.findPlayer(nameToKick, cachedPlayer ->
        {
            if(cachedPlayer != null)
            {
                deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.KICK,
                    kicker, nameToKick, finalMessage, isSilent ? "1" : "0");
            }
            else
            {
                sendMessage(kicker, Prefixes.FAILURE + Prefixes.input(nameToKick) +
                    " is not online.");
            }
        });

        return true;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onDeltaRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaBansChannels.KICK))
        {
            Settings settings = plugin.getSettings();
            String[] split = DELTA_PATTERN.split(event.getMessage(), 4);
            String kicker = split[0];
            String nameToKick = split[1];
            String message = split[2];
            String isSilent = split[3];
            Player toKick = Bukkit.getPlayer(nameToKick);

            if(toKick != null)
            {
                String kickPlayer = settings.format("KickMessageToPlayer", kicker, nameToKick, message);
                toKick.kickPlayer(kickPlayer);
            }

            announceKick(settings, kicker, nameToKick, message, isSilent.equals("1"));
        }
    }

    private void announceKick(Settings settings, String kicker, String nameToKick,
        String message, boolean isSilent)
    {
        String kickAnnounce = settings.format("KickMessageToAnnounce", kicker, nameToKick, message);

        deltaRedisApi.sendAnnouncementToServer(Servers.SPIGOT, kickAnnounce,
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
            Player player = Bukkit.getPlayer(senderName);

            if(player != null)
            {
                player.sendMessage(message);
            }
        }
    }
}
