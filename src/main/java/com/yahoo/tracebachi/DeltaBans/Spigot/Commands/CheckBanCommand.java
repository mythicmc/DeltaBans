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
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class CheckBanCommand implements CommandExecutor
{
    private static final String CHECK_BAN_CHANNEL = "DB-CheckBan";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    private DeltaRedisApi deltaRedisApi;

    public CheckBanCommand(DeltaRedisApi deltaRedisApi)
    {
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        boolean checkBanFull = sender.hasPermission("DeltaBans.CheckBan.Full");
        boolean checkBanLimited = sender.hasPermission("DeltaBans.CheckBan.Limited");

        if(!checkBanFull && !checkBanLimited)
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length == 0)
        {
            if(checkBanFull)
            {
                sender.sendMessage(Prefixes.INFO + "/checkban <name|ip>");
            }
            else
            {
                sender.sendMessage(Prefixes.INFO + "/checkban <name>");
            }
            return true;
        }

        String senderName = sender.getName();
        String argument = args[0];
        boolean isName = !IP_PATTERN.matcher(argument).matches();

        if(!isName && !checkBanFull)
        {
            sender.sendMessage(Prefixes.FAILURE + "You are not allowed to check IP bans.");
            return true;
        }

        String banAsMessage = buildCheckMessage(senderName, argument, isName, checkBanFull);
        deltaRedisApi.publish(Channels.BUNGEECORD, CHECK_BAN_CHANNEL, banAsMessage);
        return true;
    }

    private String buildCheckMessage(String banner, String argument, boolean isName, boolean includeIp)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(argument);
        out.writeBoolean(isName);
        out.writeBoolean(includeIp);

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
