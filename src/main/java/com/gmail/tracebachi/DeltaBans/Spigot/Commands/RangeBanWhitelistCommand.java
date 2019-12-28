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

public class RangeBanWhitelistCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "rangebanwhitelist";
    private static final String COMMAND_USAGE = "/rangebanwhitelist <add|remove> <name>";
    private static final String COMMAND_PERM = "DeltaBans.RangeBan";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public RangeBanWhitelistCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("rangebanwhitelist").setExecutor(this);
        this.plugin.getCommand("rangebanwhitelist").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("rangebanwhitelist").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("rangebanwhitelist").setTabCompleter((TabCompleter)null);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], this.api);
    }

    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        boolean isSilent = DeltaBansUtils.isSilent(args);
        if (isSilent) {
            args = DeltaBansUtils.filterSilent(args);
        }

        if (args.length < 2) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/rangebanwhitelist <add|remove> <name>"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.RangeBan")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.RangeBan"}));
            return true;
        } else {
            String nameToEdit = args[1];
            ByteArrayDataOutput out = ByteStreams.newDataOutput(128);
            out.writeUTF(this.api.getServerName());
            out.writeUTF(sender.getName());
            out.writeUTF("rangeban");
            out.writeUTF(nameToEdit);
            if (args[0].equalsIgnoreCase("add")) {
                out.writeBoolean(true);
            } else {
                if (!args[0].equalsIgnoreCase("remove")) {
                    sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/rangebanwhitelist <add|remove> <name>"}));
                    return true;
                }

                out.writeBoolean(false);
            }

            this.api.sendToBungee("WhitelistEdit", out.toByteArray());
            return true;
        }
    }
}
