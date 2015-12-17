package com.yahoo.tracebachi.DeltaBans.Spigot.Commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
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
public class TempBanCommand implements CommandExecutor
{
    private static final String BAN_CHANNEL = "DB-Ban";
    private static final Pattern IP_PATTERN = Pattern.compile(
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])"
    );

    private DeltaRedisApi deltaRedisApi;
    private DeltaBansPlugin plugin;

    public TempBanCommand(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
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

        if(args.length <= 1)
        {
            sender.sendMessage(Prefixes.INFO + "/ban <name|ip> <duration> <message>");
            return true;
        }

        String banner = sender.getName();
        String ip = args[0];
        String name = null;
        String message = "You have been BANNED from this server!";
        long duration = getDuration(args[1]);

        if(args[0].equals(banner))
        {
            sender.sendMessage(Prefixes.FAILURE + "Banning yourself is not a great idea.");
            return true;
        }

        if(duration == 0)
        {
            sender.sendMessage(Prefixes.FAILURE + "Invalid duration. Try something like 1s, 2m, 3h, 4d.");
            return true;
        }

        if(args.length > 2)
        {
           message = ChatColor.translateAlternateColorCodes('&',
               String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
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

        String banAsMessage = buildBanMessage(banner, message, ip, duration, name);
        deltaRedisApi.publish(Channels.BUNGEECORD, BAN_CHANNEL, banAsMessage);
        return true;
    }

    private String buildBanMessage(String banner, String banMessage, String ip, long duration, String name)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(banner);
        out.writeUTF(banMessage);
        out.writeUTF(ip);
        out.writeUTF(Long.toHexString(duration * 1000));
        out.writeBoolean(name != null);

        if(name != null)
        {
            out.writeUTF(name);
        }

        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    private long getDuration(String input)
    {
        int multiplier = 1;
        boolean hasEndingChar = true;

        switch(input.charAt(input.length() - 1))
        {
            case 's':
                multiplier = 1;
                break;
            case 'm':
                multiplier = 60;
                break;
            case 'h':
                multiplier = 60 * 60;
                break;
            case 'd':
                multiplier = 60 * 60 * 24;
                break;
            default:
                hasEndingChar = false;
                break;
        }

        if(hasEndingChar)
        {
            input = input.substring(0, input.length() - 1);
        }

        try
        {
            int value = Integer.parseInt(input) * multiplier;
            return (value > 0) ? value : 0;
        }
        catch(NumberFormatException ex)
        {
            return 0;
        }
    }
}
