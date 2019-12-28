package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage.AddResult;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

public class BanListener implements Listener, Registerable {
    private final String HIDDEN_IP = ChatColor.translateAlternateColorCodes('&', "&rx&6.&rx&6.&rx&6.&rx");
    private final DeltaBansPlugin plugin;
    private final BanStorage banStorage;
    private final RangeBanStorage rangeBanStorage;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> banChannelListener;
    private final Consumer<ReceivedMessage> unbanChannelListener;
    private final Consumer<ReceivedMessage> rangeBanChannelListener;
    private final Consumer<ReceivedMessage> rangeUnbanChannelListener;

    public BanListener(DeltaBansPlugin plugin, BanStorage banStorage, RangeBanStorage rangeBanStorage, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(banStorage, "banStorage");
        Preconditions.checkNotNull(rangeBanStorage, "rangeBanStorage");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.banStorage = banStorage;
        this.rangeBanStorage = rangeBanStorage;
        this.api = api;
        this.formatMap = formatMap;
        this.banChannelListener = this::onBanChannelRequest;
        this.unbanChannelListener = this::onUnbanChannelRequest;
        this.rangeBanChannelListener = this::onRangeBanChannelRequest;
        this.rangeUnbanChannelListener = this::onRangeUnbanChannelRequest;
    }

    public void register() {
        this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.register("Ban", this.banChannelListener);
        messageNotifier.register("Unban", this.unbanChannelListener);
        messageNotifier.register("RangeBan", this.rangeBanChannelListener);
        messageNotifier.register("RangeUnban", this.rangeUnbanChannelListener);
    }

    public void unregister() {
        this.plugin.getProxy().getPluginManager().unregisterListener(this);
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.unregister("Ban", this.banChannelListener);
        messageNotifier.unregister("Unban", this.unbanChannelListener);
        messageNotifier.unregister("RangeBan", this.rangeBanChannelListener);
        messageNotifier.unregister("RangeUnban", this.rangeUnbanChannelListener);
    }

    private void onBanChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String name = in.readUTF();
        String ip = in.readUTF();
        String banner = in.readUTF();
        String message = in.readUTF();
        Long duration = in.readBoolean() ? in.readLong() : null;
        boolean isSilent = in.readBoolean();
        name = name.equals("") ? null : name;
        ip = ip.equals("") ? null : ip;
        message = message.equals("") ? this.formatMap.format("DefaultMessage/Ban", new Object[0]) : message;
        BanEntry entry = new BanEntry(name, ip, banner, message, duration);
        AddResult addResult = this.banStorage.addBanEntry(entry);
        String chatMessage;
        if (addResult == AddResult.EXISTING_NAME_BAN) {
            chatMessage = this.formatMap.format("BanAlreadyExists", new Object[]{name, "-"});
            this.sendChatMessage(chatMessage, banner, sendingServer);
        } else if (addResult == AddResult.EXISTING_IP_BAN) {
            chatMessage = this.formatMap.format("BanAlreadyExists", new Object[]{"-", ip});
            this.sendChatMessage(chatMessage, banner, sendingServer);
        } else if (addResult == AddResult.EXISTING_NAME_AND_IP_BAN) {
            chatMessage = this.formatMap.format("BanAlreadyExists", new Object[]{name, ip});
            this.sendChatMessage(chatMessage, banner, sendingServer);
        } else if (addResult == AddResult.SUCCESS) {
            this.kickOffProxy(name, ip, this.getKickMessage(entry));
            this.announce(this.formatBanAnnouncement(entry, isSilent), isSilent);
        }

    }

    private void onUnbanChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String unbanner = in.readUTF();
        String nameOrIpToUnban = in.readUTF();
        boolean isIp = in.readBoolean();
        boolean isSilent = in.readBoolean();
        List entries;
        if (isIp) {
            entries = this.banStorage.removeUsingIp(nameOrIpToUnban);
        } else {
            entries = this.banStorage.removeUsingName(nameOrIpToUnban);
        }

        String chatMessage;
        if (entries != null && entries.size() != 0) {
            chatMessage = this.formatUnbanAnnouncement(unbanner, nameOrIpToUnban, isIp, isSilent);
            this.announce(chatMessage, isSilent);
        } else {
            chatMessage = this.formatMap.format("BanNotFound", new Object[]{nameOrIpToUnban});
            this.sendChatMessage(chatMessage, unbanner, sendingServer);
        }
    }

    private void onRangeBanChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String banner = in.readUTF();
        String message = in.readUTF();
        String startIp = in.readUTF();
        String endIp = in.readUTF();
        boolean isSilent = in.readBoolean();
        message = message.equals("") ? this.formatMap.format("DefaultMessage/RangeBan", new Object[0]) : message;
        RangeBanEntry entry = new RangeBanEntry(banner, message, startIp, endIp);
        this.rangeBanStorage.add(entry);
        long startAddressLong = entry.getStartAddressLong();
        long endAddressLong = entry.getEndAddressLong();
        this.kickOffProxy(startAddressLong, endAddressLong, entry.getMessage());
        String rangeBanRange;
        if (isSilent) {
            rangeBanRange = startIp + "-" + endIp;
        } else {
            rangeBanRange = this.HIDDEN_IP + "-" + this.HIDDEN_IP;
        }

        this.announce(this.formatMap.format("Announce/RangeBan", new Object[]{banner, rangeBanRange, message}), isSilent);
    }

    private void onRangeUnbanChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String unbanner = in.readUTF();
        String ip = in.readUTF();
        boolean isSilent = in.readBoolean();
        int count = this.rangeBanStorage.removeIpRangeBan(ip);
        String announcement = this.formatMap.format("Announce/RangeUnban", new Object[]{unbanner, String.valueOf(count), ip});
        this.announce(announcement, isSilent);
    }

    private void kickOffProxy(String name, String ip, String message) {
        BaseComponent[] componentMessage = TextComponent.fromLegacyText(message);
        if (name != null) {
            ProxiedPlayer proxiedPlayer = ProxyServer.getInstance().getPlayer(name);
            if (proxiedPlayer != null) {
                proxiedPlayer.disconnect((BaseComponent[])componentMessage.clone());
            }
        }

        if (ip != null) {
            Iterator var7 = ProxyServer.getInstance().getPlayers().iterator();

            while(var7.hasNext()) {
                ProxiedPlayer player = (ProxiedPlayer)var7.next();
                if (player.getAddress().getAddress().getHostAddress().equals(ip)) {
                    player.disconnect((BaseComponent[])componentMessage.clone());
                }
            }
        }

    }

    private void kickOffProxy(long startIp, long endIp, String message) {
        BaseComponent[] kickMessage = TextComponent.fromLegacyText(message);
        Iterator var7 = ProxyServer.getInstance().getPlayers().iterator();

        while(var7.hasNext()) {
            ProxiedPlayer player = (ProxiedPlayer)var7.next();
            long ipAsLong = DeltaBansUtils.convertIpToLong(player.getAddress().getAddress().getHostAddress());
            if (ipAsLong >= startIp && ipAsLong <= endIp) {
                player.disconnect((BaseComponent[])kickMessage.clone());
            }
        }

    }

    private String formatBanAnnouncement(BanEntry entry, boolean isSilent) {
        String banner = entry.getBanner();
        String banMessage = entry.getMessage();
        String formattedDuration = DeltaBansUtils.formatDuration(entry.getDuration());
        String bannedNameOrIp;
        if (!entry.hasName() && !isSilent) {
            bannedNameOrIp = this.HIDDEN_IP;
        } else {
            bannedNameOrIp = entry.hasName() ? entry.getName() : entry.getIp();
        }

        return this.formatMap.format("Announce/Ban", new Object[]{banner, bannedNameOrIp, banMessage, formattedDuration});
    }

    private String formatUnbanAnnouncement(String sender, String nameOrIpToUnban, boolean isIp, boolean isSilent) {
        return isIp && !isSilent ? this.formatMap.format("Announce/Unban", new Object[]{sender, this.HIDDEN_IP}) : this.formatMap.format("Announce/Unban", new Object[]{sender, nameOrIpToUnban});
    }

    private void announce(String announcement, boolean isSilent) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
        out.writeBoolean(isSilent);
        out.writeUTF(announcement);
        this.api.sendToServers("DeltaBansAnnouncements", out.toByteArray());
    }

    private String getKickMessage(BanEntry entry) {
        String message = entry.getMessage();
        String banner = entry.getBanner();
        if (entry.hasDuration()) {
            long currentTime = System.currentTimeMillis();
            long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;
            String formattedDuration = DeltaBansUtils.formatDuration(remainingTime);
            return this.formatMap.format("TemporaryBanMessage", new Object[]{message, banner, formattedDuration});
        } else {
            return this.formatMap.format("PermanentBanMessage", new Object[]{message, banner});
        }
    }

    private void sendChatMessage(String message, String playerName, String serverName) {
        this.api.sendChatMessages(Collections.singletonList(message), playerName, serverName);
    }
}
