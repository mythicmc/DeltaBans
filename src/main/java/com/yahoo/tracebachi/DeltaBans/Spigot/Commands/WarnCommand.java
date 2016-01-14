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
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansListener;
import com.yahoo.tracebachi.DeltaBans.Spigot.PendingWarn;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class WarnCommand implements TabExecutor
{
    private static final String ADD_WARN_CHANNEL = "DB-AddWarning";

    private DeltaBansListener listener;
    private DeltaRedisApi deltaRedisApi;

    public WarnCommand(DeltaBansListener listener, DeltaRedisApi deltaRedisApi)
    {
        this.listener = listener;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.listener = null;
        this.deltaRedisApi = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
    {
        if(args.length != 0)
        {
            String lastArg = args[args.length - 1];
            return deltaRedisApi.matchStartOfName(lastArg);
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaBans.Warn"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/warn <name> <message>");
            return true;
        }

        String warner = sender.getName();
        String name = args[0];
        String message = "You have been warned!";

        if(name.equalsIgnoreCase(warner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Warning yourself? You have been warned! :)");
            return true;
        }

        if(args.length > 1)
        {
           message = ChatColor.translateAlternateColorCodes('&',
               String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        }

        PendingWarn pendingWarn = new PendingWarn(sender.getName(), message);
        if(listener.addPendingWarn(name, pendingWarn))
        {
            String addWarnAsMessage = buildAddWarnMessage(warner, name, message);
            deltaRedisApi.publish(Channels.BUNGEECORD, ADD_WARN_CHANNEL, addWarnAsMessage);
        }
        else
        {
            sender.sendMessage(Prefixes.FAILURE + "Looks like there is a pending warning for " +
                Prefixes.input(name));
        }
        return true;
    }

    private String buildAddWarnMessage(String warner, String name, String message)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(warner);
        out.writeUTF(name);
        out.writeUTF(message);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
