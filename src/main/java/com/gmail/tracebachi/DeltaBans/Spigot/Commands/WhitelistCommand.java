package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;

public class WhitelistCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "whitelist";
    private static final String COMMAND_USAGE = "/whitelist <on|off|add|remove> <name>";
    private static final String COMMAND_PERM = "DeltaBans.Whitelist";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public WhitelistCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("whitelist").setExecutor(this);
        this.plugin.getCommand("whitelist").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("whitelist").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("whitelist").setTabCompleter((TabCompleter)null);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], this.api);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 1) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/whitelist <on|off|add|remove> <name>"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Whitelist")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Whitelist"}));
            return true;
        } else {
            String senderName = sender.getName();
            byte[] bytes;
            if (args[0].equalsIgnoreCase("on")) {
                bytes = this.getBytesForNormalWhitelistToggle(senderName, true);
                this.api.sendToBungee("WhitelistToggle", bytes);
            } else if (args[0].equalsIgnoreCase("off")) {
                bytes = this.getBytesForNormalWhitelistToggle(senderName, false);
                this.api.sendToBungee("WhitelistToggle", bytes);
            } else if (args.length > 1 && args[0].equalsIgnoreCase("add")) {
                bytes = this.getBytesForNormalWhitelistEdit(senderName, args[1], true);
                this.api.sendToBungee("WhitelistEdit", bytes);
            } else if (args.length > 1 && args[0].equalsIgnoreCase("remove")) {
                bytes = this.getBytesForNormalWhitelistEdit(senderName, args[1], false);
                this.api.sendToBungee("WhitelistEdit", bytes);
            } else {
                sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/whitelist <on|off|add|remove> <name>"}));
            }

            return true;
        }
    }

    private byte[] getBytesForNormalWhitelistToggle(String sender, boolean enabled) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(128);
        out.writeUTF(this.api.getServerName());
        out.writeUTF(sender);
        out.writeBoolean(enabled);
        return out.toByteArray();
    }

    private byte[] getBytesForNormalWhitelistEdit(String sender, String nameToEdit, boolean isAdd) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(128);
        out.writeUTF(this.api.getServerName());
        out.writeUTF(sender);
        out.writeUTF("normal");
        out.writeUTF(nameToEdit);
        out.writeBoolean(isAdd);
        return out.toByteArray();
    }
}
