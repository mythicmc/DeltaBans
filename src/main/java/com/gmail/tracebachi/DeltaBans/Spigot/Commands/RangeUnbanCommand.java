package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RangeUnbanCommand implements CommandExecutor, Registerable {
    private static final String COMMAND_NAME = "rangeunban";
    private static final String COMMAND_USAGE = "/rangeunban <ip>";
    private static final String COMMAND_PERM = "DeltaBans.RangeBan";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public RangeUnbanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("rangeunban").setExecutor(this);
    }

    public void unregister() {
        this.plugin.getCommand("rangeunban").setExecutor((CommandExecutor)null);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 1) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/rangeunban <ip>"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.RangeBan")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.RangeBan"}));
            return true;
        } else {
            String ip = args[0];
            if (!DeltaBansUtils.isIp(ip)) {
                sender.sendMessage(this.formatMap.format("InvalidIp", new Object[]{ip}));
                return true;
            } else {
                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(sender.getName());
                out.writeUTF(ip);
                out.writeBoolean(isSilent);
                this.api.sendToBungee("RangeUnban", out.toByteArray());
                return true;
            }
        }
    }
}
