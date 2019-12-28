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

public class TempBanCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "tempban";
    private static final String COMMAND_USAGE = "/tempban <name|ip> <duration> [message]";
    private static final String COMMAND_PERM = "DeltaBans.Ban";
    private static final String NAME_ONLY_FLAG = "-name";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public TempBanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("tempban").setExecutor(this);
        this.plugin.getCommand("tempban").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("tempban").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("tempban").setTabCompleter((TabCompleter)null);
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

        if (args.length < 2) {
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/tempban <name|ip> <duration> [message]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Ban")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Ban"}));
            return true;
        } else {
            String banner = sender.getName();
            String nameOrIp = args[0];
            if (banner.equalsIgnoreCase(nameOrIp)) {
                sender.sendMessage(this.formatMap.format("NotAllowedOnSelf", new Object[]{"tempban"}));
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

                Long durationInSeconds = this.getDurationInSeconds(args[1]);
                if (durationInSeconds <= 0L) {
                    sender.sendMessage(this.formatMap.format("InvalidDuration", new Object[]{args[1]}));
                    return true;
                } else {
                    String message = "";
                    if (args.length > 2) {
                        message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 2, args.length));
                        message = ChatColor.translateAlternateColorCodes('&', message);
                    }

                    ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                    out.writeUTF(this.api.getServerName());
                    out.writeUTF(name);
                    out.writeUTF(ip);
                    out.writeUTF(banner);
                    out.writeUTF(message);
                    out.writeBoolean(true);
                    out.writeLong(durationInSeconds * 1000L);
                    out.writeBoolean(isSilent);
                    this.api.sendToBungee("Ban", out.toByteArray());
                    return true;
                }
            }
        }
    }

    private long getDurationInSeconds(String input) {
        if (input != null && !input.isEmpty()) {
            int multiplier = 1;
            boolean hasEndingChar = true;
            switch(input.charAt(input.length() - 1)) {
                case 'd':
                    multiplier = 86400;
                    break;
                case 'h':
                    multiplier = 3600;
                    break;
                case 'm':
                    multiplier = 60;
                    break;
                case 's':
                    multiplier = 1;
                    break;
                case 'w':
                    multiplier = 604800;
                    break;
                default:
                    hasEndingChar = false;
            }

            if (hasEndingChar) {
                input = input.substring(0, input.length() - 1);
            }

            try {
                int value = Integer.parseInt(input) * multiplier;
                return value > 0 ? (long)value : 0L;
            } catch (NumberFormatException var5) {
                return 0L;
            }
        } else {
            return 0L;
        }
    }
}
