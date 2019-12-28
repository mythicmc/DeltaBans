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

public class BannedCommand implements CommandExecutor, Registerable {
    private static final String COMMAND_NAME = "banned";
    private static final String COMMAND_USAGE = "/banned <name>";
    private static final String COMMAND_PERM = "DeltaBans.CheckBan";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public BannedCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("banned").setExecutor(this);
    }

    public void unregister() {
        this.plugin.getCommand("banned").setExecutor((CommandExecutor)null);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 1) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/banned <name>"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.CheckBan")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.CheckBan"}));
            return true;
        } else {
            String nameOrIpToCheck = args[0];
            ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
            out.writeUTF(this.api.getServerName());
            out.writeUTF(sender.getName());
            out.writeUTF(nameOrIpToCheck);
            out.writeBoolean(DeltaBansUtils.isIp(nameOrIpToCheck));
            this.api.sendToBungee("CheckBan", out.toByteArray());
            return true;
        }
    }
}
