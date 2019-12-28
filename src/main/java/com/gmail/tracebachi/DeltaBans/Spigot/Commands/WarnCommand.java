package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Messages.ResponseMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;

public class WarnCommand implements TabExecutor, Registerable {
    private static final String COMMAND_NAME = "warn";
    private static final String COMMAND_USAGE = "/warn <name> [message]";
    private static final String COMMAND_PERM = "DeltaBans.Warn";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;

    public WarnCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
    }

    public void register() {
        this.plugin.getCommand("warn").setExecutor(this);
        this.plugin.getCommand("warn").setTabCompleter(this);
    }

    public void unregister() {
        this.plugin.getCommand("warn").setExecutor((CommandExecutor)null);
        this.plugin.getCommand("warn").setTabCompleter((TabCompleter)null);
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
            sender.sendMessage(this.formatMap.format("Usage", new Object[]{"/warn <name> [message]"}));
            return true;
        } else if (!sender.hasPermission("DeltaBans.Warn")) {
            sender.sendMessage(this.formatMap.format("NoPerm", new Object[]{"DeltaBans.Warn"}));
            return true;
        } else {
            String warner = sender.getName();
            String nameToWarn = args[0];
            if (nameToWarn.equalsIgnoreCase(warner)) {
                sender.sendMessage(this.formatMap.format("NotAllowedOnSelf", new Object[]{"warn"}));
                return true;
            } else {
                String message = "";
                if (args.length > 1) {
                    message = String.join(" ", (CharSequence[])Arrays.copyOfRange(args, 1, args.length));
                    message = ChatColor.translateAlternateColorCodes('&', message);
                }

                ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
                out.writeUTF(warner);
                out.writeUTF(nameToWarn);
                out.writeUTF(message);
                out.writeBoolean(isSilent);
                this.api.sendToBungee("Warn", out.toByteArray(), (resp) -> {
                    this.plugin.executeSync(() -> {
                        this.onResponseToAddingWarn(warner, resp);
                    });
                }, TimeUnit.SECONDS.toMillis(10L));
                return true;
            }
        }
    }

    private void onResponseToAddingWarn(String warner, ResponseMessage responseMessage) {
        if (responseMessage.getResponseStatus().isOk()) {
            CommandSender commandSender = null;
            boolean isConsole = warner.equalsIgnoreCase("console");
            if (!isConsole) {
                commandSender = this.plugin.getServer().getPlayerExact(warner);
            }

            if (commandSender == null) {
                commandSender = this.plugin.getServer().getConsoleSender();
            }

            boolean wasOp = ((CommandSender)commandSender).isOp();
            if (!isConsole && !wasOp) {
                ((CommandSender)commandSender).setOp(true);
            }

            ByteArrayDataInput in = responseMessage.getDataInput();
            int commandCount = in.readInt();

            for(int i = 0; i < commandCount; ++i) {
                try {
                    String command = in.readUTF();
                    this.plugin.getLogger().info(warner + " issued warn command /" + command);
                    this.plugin.getServer().dispatchCommand((CommandSender)commandSender, command);
                } catch (Exception var10) {
                    var10.printStackTrace();
                }
            }

            if (!isConsole && !wasOp) {
                ((CommandSender)commandSender).setOp(false);
            }

        }
    }
}
