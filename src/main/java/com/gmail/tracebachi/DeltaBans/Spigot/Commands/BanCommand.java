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
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;

public class BanCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "ban";
    private static final String COMMAND_USAGE = "/ban <name|ip> [message]";
    private static final String COMMAND_PERM = "DeltaBans.Ban";
    private static final String NAME_ONLY_FLAG = "-name";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public BanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("ban").setExecutor(this);
        this.plugin.getCommand("ban").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("ban").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("ban").setTabCompleter((TabCompleter)null);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], this.api);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        boolean isNameOnly = DeltaBansUtils.hasFlag(args, "-name");
        if (isNameOnly) {
            args = DeltaBansUtils.filterFlag(args, "-name");
        }

        if (args.length < 1) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/ban <name|ip> [message]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Ban")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Ban"}));
            return true;
        } else {
            String banner = sender.getName();
            String nameOrIp = args[0];
            if (banner.equalsIgnoreCase(nameOrIp)) {
                sender.sendMessage(this.formatMap.format("NotAllowedOnSelf", new Object[]{"ban"}));
                return true;
            } else {
                String name = "";
                String ip = nameOrIp;
                if (isNameOnly) {
                    name = nameOrIp;
                    ip = "";
                } else if (!DeltaBansUtils.isIp(nameOrIp)) {
                    name = nameOrIp;
                    ip = this.plugin.getIpOfPlayer(nameOrIp);
                    if (ip.equals("")) {
                        sender.sendMessage(this.formatMap.format("NoIpFound", new Object[]{nameOrIp}));
                    }
                }

                String message = "";
                if (args.length > 1) {
                    message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length));
                    message = ChatColor.translateAlternateColorCodes('&', message);
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(this.api.getServerName());
                out.writeUTF(name);
                out.writeUTF(ip);
                out.writeUTF(banner);
                out.writeUTF(message);
                out.writeBoolean(false);
                out.writeBoolean(isSilent);
                this.api.sendToBungee("Ban", out.toByteArray());
                return true;
            }
        }
    }
}
