package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Spigot.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class NameBanCommand implements CommandExecutor
{
    private static final String NAME_BAN_CHANNEL = "DB-NameBan";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    private DeltaRedisApi deltaRedisApi;

    public NameBanCommand(DeltaRedisApi deltaRedisApi)
    {
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
    {
        if(!sender.hasPermission("DeltaBans.Ban"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/ban <name|ip> <message>");
            return true;
        }

        String banner = sender.getName();
        String banee = args[0];

        if(banee.equals(banner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Banning yourself is not a great idea.");
            return true;
        }

        if(IP_PATTERN.matcher(banee).matches())
        {
            sender.sendMessage(Prefixes.FAILURE + "This command is for banning specific " +
                "names. Use /ban <ip> instead.");
            return true;
        }

        String message = "You have been BANNED from this server!";
        if(args.length > 1)
        {
           message = ChatColor.translateAlternateColorCodes('&',
               String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        }

        String banAsMessage = buildBanMessage(banner, message, banee);
        deltaRedisApi.publish(Channels.BUNGEECORD, NAME_BAN_CHANNEL, banAsMessage);
        return true;
    }

    private String buildBanMessage(String banner, String banMessage, String name)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(banMessage);
        out.writeUTF(name);

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
