package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Collections;
import java.util.function.Consumer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Listener;

public class KickListener implements Listener, Registerable {
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> kickChannelListener;

    public KickListener(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.api = api;
        this.formatMap = formatMap;
        this.kickChannelListener = this::onKickChannelRequest;
    }

    public void register() {
        this.api.getMessageNotifier().register("Kick", this.kickChannelListener);
    }

    public void unregister() {
        this.api.getMessageNotifier().unregister("Kick", this.kickChannelListener);
    }

    private void onKickChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String kicker = in.readUTF();
        String nameToKick = in.readUTF();
        String message = in.readUTF();
        boolean isSilent = in.readBoolean();
        message = message.equals("") ? this.formatMap.format("DefaultMessage/Kick", new Object[0]) : message;
        ProxiedPlayer playerToKick = this.plugin.getProxy().getPlayer(nameToKick);
        String kickMessage;
        if (playerToKick == null) {
            kickMessage = this.formatMap.format("PlayerOffline", new Object[]{nameToKick});
            this.api.sendChatMessages(Collections.singletonList(kickMessage), kicker, sendingServer);
        } else {
            kickMessage = this.formatMap.format("KickMessage", new Object[]{kicker, nameToKick, message});
            BaseComponent[] textComponent = TextComponent.fromLegacyText(kickMessage);
            playerToKick.disconnect(textComponent);
            String announcement = this.formatMap.format("Announce/Kick", new Object[]{kicker, nameToKick, message});
            this.announce(announcement, isSilent);
        }
    }

    private void announce(String announcement, boolean isSilent) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
        out.writeBoolean(isSilent);
        out.writeUTF(announcement);
        this.api.sendToServers("DeltaBansAnnouncements", out.toByteArray());
    }
}
