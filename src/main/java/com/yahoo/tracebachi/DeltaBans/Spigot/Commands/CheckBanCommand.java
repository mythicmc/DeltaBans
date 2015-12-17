package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Spigot.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisApi;
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
        if(!sender.hasPermission("DeltaBans.CheckBan"))
        {
            sender.sendMessage(Prefixes.FAILURE + "You do not have permission to use this command.");
            return true;
        }

        if(args.length == 0)
        {
            sender.sendMessage(Prefixes.INFO + "/checkban <name|ip>");
            return true;
        }

        String senderName = sender.getName();
        String argument = args[0];
        boolean isName = !IP_PATTERN.matcher(argument).matches();

        String banAsMessage = buildCheckMessage(senderName, argument, isName);
        deltaRedisApi.publish(Channels.BUNGEECORD, CHECK_BAN_CHANNEL, banAsMessage);
        return true;
    }

    private String buildCheckMessage(String banner, String argument, boolean isName)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(argument);
        out.writeBoolean(isName);

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
