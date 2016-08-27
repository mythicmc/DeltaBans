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
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class WarnCommand implements TabExecutor, Listener, Registerable, Shutdownable
{
    private static final Pattern NAME_PATTERN = Pattern.compile("\\{name\\}");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\{message\\}");

    private DeltaBans plugin;

    public WarnCommand(DeltaBans plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getCommand("warn").setExecutor(this);
        plugin.getCommand("warn").setTabCompleter(this);
    }

    @Override
    public void unregister()
    {
        plugin.getCommand("warn").setExecutor(null);
        plugin.getCommand("warn").setTabCompleter(null);
        HandlerList.unregisterAll(this);
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
            sender.sendMessage(Settings.format("WarnUsage"));
            return true;
        }

        if(!sender.hasPermission("DeltaBans.Warn"))
        {
            sender.sendMessage(Settings.format("NoPermission", "DeltaBans.Warn"));
            return true;
        }

        String warner = sender.getName();
        String name = args[0];
        String message = Settings.format("DefaultWarnMessage");

        if(name.equalsIgnoreCase(warner))
        {
            sender.sendMessage(Settings.format("WarnSelf"));
            return true;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        DeltaRedisApi.instance().publish(
            Servers.BUNGEECORD,
            DeltaBansChannels.WARN,
            warner,
            name,
            message,
            isSilent ? "1" : "0");
        return true;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaBansChannels.WARN))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String senderName = splitMessage[0];
            String receiver = splitMessage[1];
            String message = splitMessage[2];
            Integer amount = Integer.parseInt(splitMessage[3], 16);

            Player player = Bukkit.getPlayerExact(senderName);

            if(player != null && player.isOnline())
            {
                boolean wasOp = player.isOp();
                player.setOp(true);
                dispatchWarn(player, receiver, message, amount);
                player.setOp(wasOp);
            }
            else
            {
                dispatchWarn(Bukkit.getConsoleSender(), receiver, message, amount);
            }
        }
    }

    private void dispatchWarn(CommandSender sender, String receiver, String message, Integer warnAmount)
    {
        for(String command : Settings.getWarningCommands(warnAmount))
        {
            String namedReplaced = NAME_PATTERN.matcher(command).replaceAll(receiver);
            String messageReplaced = MESSAGE_PATTERN.matcher(namedReplaced).replaceAll(message);

            plugin.info(sender.getName() + " issued warn command /" + messageReplaced);

            try
            {
                Bukkit.dispatchCommand(sender, messageReplaced);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
