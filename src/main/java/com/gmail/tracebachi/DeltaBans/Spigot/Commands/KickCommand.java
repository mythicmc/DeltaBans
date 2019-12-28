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

public class KickCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "kick";
    private static final String COMMAND_USAGE = "/kick <name> [message]";
    private static final String COMMAND_PERM = "DeltaBans.Kick";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public KickCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("kick").setExecutor(this);
        this.plugin.getCommand("kick").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("kick").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("kick").setTabCompleter((TabCompleter)null);
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
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/kick <name> [message]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Kick")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Kick"}));
            return true;
        } else {
            String kicker = sender.getName();
            String nameToKick = args[0];
            if (kicker.equalsIgnoreCase(nameToKick)) {
                sender.sendMessage(this.formatMap.format("NotAllowedOnSelf", new Object[]{"kick"}));
                return true;
            } else {
                String message = "";
                if (args.length > 1) {
                    message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length));
                    message = ChatColor.translateAlternateColorCodes('&', message);
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(this.api.getServerName());
                out.writeUTF(kicker);
                out.writeUTF(nameToKick);
                out.writeUTF(message);
                out.writeBoolean(isSilent);
                this.api.sendToBungee("Kick", out.toByteArray());
                return true;
            }
        }
    }
}
