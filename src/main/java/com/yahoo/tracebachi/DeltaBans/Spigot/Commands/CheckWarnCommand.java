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
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class CheckWarnCommand implements TabExecutor
{
    private static final String CHECK_WARN_CHANNEL = "DB-CheckWarning";

    private DeltaRedisApi deltaRedisApi;

    public CheckWarnCommand(DeltaRedisApi deltaRedisApi)
    {
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
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
        if(!sender.hasPermission("DeltaBans.CheckWarn"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/checkwarn <name>");
            return true;
        }

        String checker = sender.getName();
        String name = args[0];

        String checkWarnAsMessage = buildCheckWarnMessage(checker, name);
        deltaRedisApi.publish(Channels.BUNGEECORD, CHECK_WARN_CHANNEL, checkWarnAsMessage);
        return true;
    }

    private String buildCheckWarnMessage(String warner, String name)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(warner);
        out.writeUTF(name);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
