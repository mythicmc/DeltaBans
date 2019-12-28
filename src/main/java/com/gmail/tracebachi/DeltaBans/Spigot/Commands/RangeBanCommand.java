package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Arrays;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RangeBanCommand implements CommandExecutor, Registerable {
    private static final String COMMAND_NAME = "rangeban";
    private static final String COMMAND_USAGE = "/rangeban <start ip> <end ip> [message]";
    private static final String COMMAND_PERM = "DeltaBans.RangeBan";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public RangeBanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("rangeban").setExecutor(this);
    }

    public void unregister() {
        this.plugin.getCommand("rangeban").setExecutor((CommandExecutor)null);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 2) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/rangeban <start ip> <end ip> [message]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.RangeBan")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.RangeBan"}));
            return true;
        } else {
            String startIpString = args[0];
            String endIpString = args[1];
            if (!DeltaBansUtils.isIp(startIpString)) {
                sender.sendMessage(this.formatMap.format("InvalidIp", new Object[]{startIpString}));
                return true;
            } else if (!DeltaBansUtils.isIp(endIpString)) {
                sender.sendMessage(this.formatMap.format("InvalidIp", new Object[]{endIpString}));
                return true;
            } else {
                String message = "";
                if (args.length > 2) {
                    message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
                    message = ChatColor.translateAlternateColorCodes('&', message);
                }

                long firstAsLong = DeltaBansUtils.convertIpToLong(startIpString);
                long secondAsLong = DeltaBansUtils.convertIpToLong(endIpString);
                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(sender.getName());
                out.writeUTF(message);
                if (firstAsLong > secondAsLong) {
                    out.writeUTF(startIpString);
                    out.writeUTF(endIpString);
                } else {
                    out.writeUTF(startIpString);
                    out.writeUTF(endIpString);
                }

                out.writeBoolean(isSilent);
                this.api.sendToBungee("RangeBan", out.toByteArray());
                return true;
            }
        }
    }
}
