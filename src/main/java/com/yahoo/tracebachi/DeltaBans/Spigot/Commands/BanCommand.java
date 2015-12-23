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
package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BanCommand implements TabExecutor
{
    public static final String BAN_CHANNEL = "DB-Ban";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    private DeltaRedisApi deltaRedisApi;
    private DeltaBansPlugin plugin;

    public BanCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.plugin = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        List<String> result = new ArrayList<>();

        if(args.length != 0)
        {
            String partial = args[args.length - 1].toLowerCase();
            for(String name : deltaRedisApi.getCachedPlayers())
            {
                if(name.startsWith(partial))
                {
                    result.add(name);
                }
            }
        }
        return result;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaBans.Ban"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/ban <name|ip> <message>");
            return true;
        }

        String banner = sender.getName();
        String ip = args[0];
        String name = null;
        String message = "You have been BANNED from this server!";

        if(args[0].equals(banner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Banning yourself is not a great idea.");
            return true;
        }

        if(args.length > 1)
        {
           message = ChatColor.translateAlternateColorCodes('&',
               String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        }

        if(!IP_PATTERN.matcher(args[0]).matches())
        {
            try
            {
                name = args[0];
                ip = plugin.getIpOfPlayer(name);
            }
            catch(IllegalArgumentException ex)
            {
                sender.sendMessage(Prefixes.FAILURE + ex.getMessage());
                return true;
            }
        }

        String banAsMessage = buildBanMessage(banner, message, ip, name);
        deltaRedisApi.publish(Channels.BUNGEECORD, BAN_CHANNEL, banAsMessage);
        return true;
    }

    private String buildBanMessage(String banner, String banMessage, String ip, String name)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(banMessage);
        out.writeUTF(ip);
        out.writeUTF(Long.toHexString(0));
        out.writeBoolean(name != null);

        if(name != null)
        {
            out.writeUTF(name);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
