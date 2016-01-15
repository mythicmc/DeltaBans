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
import com.yahoo.tracebachi.DeltaBans.DeltaBansChannels;
import com.yahoo.tracebachi.DeltaBans.DeltaBansUtils;
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class WarnCommand extends DeltaBansCommand
{
    private String defaultWarningMessage;
    private DeltaRedisApi deltaRedisApi;

    public WarnCommand(String defaultWarningMessage, DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        super("warn", "DeltaBans.Warn", plugin);
        this.defaultWarningMessage = defaultWarningMessage;
        this.deltaRedisApi = deltaRedisApi;
    }

    @Override
    public void onShutdown()
    {
        this.defaultWarningMessage = null;
        this.deltaRedisApi = null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
    {
        String lastArg = args[args.length - 1];
        return deltaRedisApi.matchStartOfName(lastArg);
    }

    @Override
    public void runCommand(CommandSender sender, Command command, String label, String[] args)
    {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if(isSilent)
        {
            args = DeltaBansUtils.filterSilent(args);
        }

        if(args.length < 1)
        {
            sender.sendMessage(Prefixes.INFO + "/warn <name> [message]");
            return;
        }

        String warner = sender.getName();
        String name = args[0];
        String message = defaultWarningMessage;

        if(name.equalsIgnoreCase(warner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Warning yourself? You have been warned! :)");
            return;
        }

        if(args.length > 1)
        {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            message = ChatColor.translateAlternateColorCodes('&', message);
        }

        String channelMessage = buildChannelMessage(warner, name, message, isSilent);
        deltaRedisApi.publish(Channels.BUNGEECORD, DeltaBansChannels.WARN, channelMessage);
    }

    private String buildChannelMessage(String warner, String name, String message, boolean isSilent)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(warner);
        out.writeUTF(name);
        out.writeUTF(message);
        out.writeBoolean(isSilent);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
