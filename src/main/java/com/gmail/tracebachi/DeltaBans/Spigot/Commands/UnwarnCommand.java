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

public class UnwarnCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "unwarn";
    private static final String COMMAND_USAGE = "/unwarn <name> [amount]";
    private static final String COMMAND_PERM = "DeltaBans.Warn";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public UnwarnCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("unwarn").setExecutor(this);
        this.plugin.getCommand("unwarn").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("unwarn").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("unwarn").setTabCompleter((TabCompleter)null);
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
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/unwarn <name> [amount]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Warn")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Warn"}));
            return true;
        } else {
            Integer amount = 1;
            if (args.length >= 2) {
                amount = this.parseInt(args[1]);
                amount = amount == null ? 1 : amount;
            }

            String nameToUnwarn = args[0];
            ByteArrayDataOutput out = ByteStreams.newDataOutput(128);
            out.writeUTF(this.api.getServerName());
            out.writeUTF(sender.getName());
            out.writeUTF(nameToUnwarn);
            out.writeInt(amount);
            out.writeBoolean(isSilent);
            this.api.sendToBungee("Unwarn", out.toByteArray());
            return true;
        }
    }

    private Integer parseInt(String source) {
        try {
            return Integer.parseInt(source);
        } catch (NumberFormatException var3) {
            return null;
        }
    }
}
