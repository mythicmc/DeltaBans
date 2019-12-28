package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessageNotifier;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import java.util.Collections;
import java.util.function.Consumer;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class WhitelistListener implements Listener, Registerable {
    private final DeltaBansPlugin plugin;
    private final WhitelistStorage whitelistStorage;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> whitelistEditChannelListener;
    private final Consumer<ReceivedMessage> whitelistToggleChannelListener;
    private boolean whitelistEnabled;

    public WhitelistListener(DeltaBansPlugin plugin, WhitelistStorage whitelistStorage, SockExchangeApi api, MessageFormatMap formatMap, boolean shouldStartWithWhitelist) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(whitelistStorage, "whitelistStorage");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.plugin = plugin;
        this.whitelistStorage = whitelistStorage;
        this.api = api;
        this.formatMap = formatMap;
        this.whitelistEditChannelListener = this::onWhitelistEditChannelRequest;
        this.whitelistToggleChannelListener = this::onWhitelistToggleChannelRequest;
        this.whitelistEnabled = shouldStartWithWhitelist;
    }

    public void register() {
        this.plugin.getProxy().getPluginManager().registerListener(this.plugin, this);
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.register("WhitelistEdit", this.whitelistEditChannelListener);
        messageNotifier.register("WhitelistToggle", this.whitelistToggleChannelListener);
    }

    public void unregister() {
        this.plugin.getProxy().getPluginManager().unregisterListener(this);
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.unregister("WhitelistEdit", this.whitelistEditChannelListener);
        messageNotifier.unregister("WhitelistToggle", this.whitelistToggleChannelListener);
    }

    @EventHandler(
            priority = -32
    )
    public void onLogin(LoginEvent event) {
        if (!event.isCancelled()) {
            PendingConnection pending = event.getConnection();
            String playerName = pending.getName();
            if (this.whitelistEnabled && !this.whitelistStorage.isOnNormalWhitelist(playerName)) {
                event.setCancelReason(this.formatMap.format("ServerInWhitelistMode", new Object[0]));
                event.setCancelled(true);
            }

        }
    }

    private void onWhitelistEditChannelRequest(ReceivedMessage message) {
        ByteArrayDataInput in = message.getDataInput();
        String sendingServer = in.readUTF();
        String sender = in.readUTF();
        String whitelistType = in.readUTF().toLowerCase();
        String nameToEdit = in.readUTF();
        boolean isAdd = in.readBoolean();
        String chatMessage;
        if (whitelistType.equals("normal")) {
            if (isAdd) {
                this.whitelistStorage.addToNormalWhitelist(nameToEdit);
                chatMessage = this.formatMap.format("AddedToWhitelist", new Object[]{nameToEdit, whitelistType});
                this.sendChatMessage(chatMessage, sender, sendingServer);
            } else {
                this.whitelistStorage.removeFromNormalWhitelist(nameToEdit);
                chatMessage = this.formatMap.format("RemovedFromWhitelist", new Object[]{nameToEdit, whitelistType});
                this.sendChatMessage(chatMessage, sender, sendingServer);
            }
        } else if (whitelistType.equals("rangeban")) {
            if (isAdd) {
                this.whitelistStorage.addToRangeBanWhitelist(nameToEdit);
                chatMessage = this.formatMap.format("AddedToWhitelist", new Object[]{nameToEdit, whitelistType});
                this.sendChatMessage(chatMessage, sender, sendingServer);
            } else {
                this.whitelistStorage.removeFromRangeBanWhitelist(nameToEdit);
                chatMessage = this.formatMap.format("RemovedFromWhitelist", new Object[]{nameToEdit, whitelistType});
                this.sendChatMessage(chatMessage, sender, sendingServer);
            }
        }

    }

    private void onWhitelistToggleChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String sender = in.readUTF();
        boolean enable = in.readBoolean();
        if (enable) {
            this.whitelistEnabled = true;
            this.sendChatMessage(this.formatMap.format("WhitelistToggled", new Object[]{"enabled"}), sender, sendingServer);
        } else {
            this.whitelistEnabled = false;
            this.sendChatMessage(this.formatMap.format("WhitelistToggled", new Object[]{"disabled"}), sender, sendingServer);
        }

    }

    private void sendChatMessage(String message, String playerName, String serverName) {
        this.api.sendChatMessages(Collections.singletonList(message), playerName, serverName);
    }
}
