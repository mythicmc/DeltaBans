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

public class UnbanCommand implements CommandExecutor, Registerable {
    private static final String COMMAND_NAME = "unban";
    private static final String COMMAND_USAGE = "/unban <name|ip>";
    private static final String COMMAND_PERM = "DeltaBans.Ban";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public UnbanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("unban").setExecutor(this);
    }

    public void unregister() {
        this.plugin.getCommand("unban").setExecutor((CommandExecutor)null);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 1) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/unban <name|ip>"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Ban")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Ban"}));
            return true;
        } else {
            String senderName = sender.getName();
            String nameOrIpToUnban = args[0];
            if (nameOrIpToUnban.equals(senderName)) {
                sender.sendMessage(this.formatMap.format("NotAllowedOnSelf", new Object[]{"unban"}));
                return true;
            } else {
                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(this.api.getServerName());
                out.writeUTF(senderName);
                out.writeUTF(nameOrIpToUnban);
                out.writeBoolean(DeltaBansUtils.isIp(nameOrIpToUnban));
                out.writeBoolean(isSilent);
                this.api.sendToBungee("Unban", out.toByteArray());
                return true;
            }
        }
    }
}
