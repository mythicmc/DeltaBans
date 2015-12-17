package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
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
public class UnbanCommand implements CommandExecutor
{
    private static final String UNBAN_CHANNEL = "DB-Unban";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    private DeltaRedisApi deltaRedisApi;
    private DeltaBansPlugin plugin;

    public UnbanCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    public void shutdown()
    {
        this.deltaRedisApi = null;
        this.plugin = null;
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
            sender.sendMessage(Prefixes.INFO + "/unban <name|ip>");
            return true;
        }

        String banner = sender.getName();
        String banee = args[0];
        boolean isName = !IP_PATTERN.matcher(banee).matches();

        if(args[0].equals(banner))
        {
            sender.sendMessage(Prefixes.FAILURE + "You are already unbanned.");
            return true;
        }

        deltaRedisApi.publish(Channels.BUNGEECORD, UNBAN_CHANNEL,
            buildUnbanMessage(sender.getName(), banee, isName));
        return true;
    }

    private String buildUnbanMessage(String sender, String banee, boolean isName)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(sender);
        out.writeUTF(banee);
        out.writeBoolean(isName);

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
