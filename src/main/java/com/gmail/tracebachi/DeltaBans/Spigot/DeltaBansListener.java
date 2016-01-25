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
package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/17/15.
 */
public class DeltaBansListener implements Listener
{
    private static final Pattern NAME_PATTERN = Pattern.compile("\\{name\\}");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\{message\\}");

    private DeltaBans plugin;
    private HashMap<Integer, List<String>> warnCommandMap = new HashMap<>();

    public DeltaBansListener(DeltaBans plugin)
    {
        this.plugin = plugin;
        ConfigurationSection warningSection = plugin.getConfig().getConfigurationSection("WarningCommands");

        if(warningSection != null)
        {
            for(String key : warningSection.getKeys(false))
            {
                Integer warningAmount = parseInt(key);
                if(warningAmount != null && warningAmount > 0)
                {
                    warnCommandMap.put(warningAmount, warningSection.getStringList(key));
                    plugin.info("Added warning commands for " + key + " warnings.");
                }
                else
                {
                    plugin.info(key + " is not a valid warning amount.");
                }
            }
        }
    }

    public void shutdown()
    {
        warnCommandMap.clear();
        warnCommandMap = null;
        plugin = null;
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(DeltaBansChannels.ANNOUNCE))
        {
            String message = event.getMessage();
            boolean isSilent = message.startsWith("!");

            if(isSilent)
            {
                message = message.substring(1);
                for(Player player : Bukkit.getOnlinePlayers())
                {
                    if(player.hasPermission("DeltaBans.SeeSilent"))
                    {
                        player.sendMessage("[Silent] " + message);
                    }
                }
            }
            else
            {
                for(Player player : Bukkit.getOnlinePlayers())
                {
                    player.sendMessage(message);
                }
            }
        }
        else if(event.getChannel().equals(DeltaBansChannels.WARN))
        {
            byte[] messageBytes = event.getMessage().getBytes(StandardCharsets.UTF_8);
            ByteArrayDataInput in = ByteStreams.newDataInput(messageBytes);
            String senderName = in.readUTF();
            String receiver = in.readUTF();
            String message = in.readUTF();
            Integer amount = Integer.parseInt(in.readUTF(), 16);

            Player player = Bukkit.getPlayer(senderName);
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
        for(String command : warnCommandMap.getOrDefault(warnAmount, Collections.emptyList()))
        {
            String namedReplaced = NAME_PATTERN.matcher(command).replaceAll(receiver);
            String messageReplaced = MESSAGE_PATTERN.matcher(namedReplaced).replaceAll(message);

            plugin.info(sender.getName() + " ran warn command /" + messageReplaced);
            try
            {
                Bukkit.dispatchCommand(sender, messageReplaced);
            }
            catch(Exception ignored) {}
        }
    }

    private Integer parseInt(String src)
    {
        try
        {
            return Integer.parseInt(src);
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }
}
