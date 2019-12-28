package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CheckBanListener implements Registerable {
    private final BanStorage banStorage;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> checkBanChannelListener;

    public CheckBanListener(BanStorage banStorage, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(banStorage, "banStorage");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.banStorage = banStorage;
        this.api = api;
        this.formatMap = formatMap;
        this.checkBanChannelListener = this::onCheckBanChannelRequest;
    }

    public void register() {
        this.api.getMessageNotifier().register("CheckBan", this.checkBanChannelListener);
    }

    public void unregister() {
        this.api.getMessageNotifier().unregister("CheckBan", this.checkBanChannelListener);
    }

    private void onCheckBanChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String sender = in.readUTF();
        String nameOrIpToCheck = in.readUTF();
        boolean isIp = in.readBoolean();
        List<String> chatMessages = new ArrayList(4);
        BanEntry banEntry;
        if (isIp) {
            banEntry = this.banStorage.getBanEntry((String)null, nameOrIpToCheck);
        } else {
            banEntry = this.banStorage.getBanEntry(nameOrIpToCheck, (String)null);
        }

        if (banEntry == null) {
            chatMessages.add(this.formatMap.format("BanNotFound", new Object[]{nameOrIpToCheck}));
        } else {
            String name = banEntry.hasName() ? banEntry.getName() : "-";
            String banner = banEntry.getBanner();
            String message = banEntry.getMessage();
            String formattedDuration = DeltaBansUtils.formatDuration(banEntry.getDuration());
            chatMessages.add(this.formatMap.format("BanInfoLine", new Object[]{"Name", name}));
            chatMessages.add(this.formatMap.format("BanInfoLine", new Object[]{"Banner", banner}));
            chatMessages.add(this.formatMap.format("BanInfoLine", new Object[]{"Message", message}));
            chatMessages.add(this.formatMap.format("BanInfoLine", new Object[]{"Duration", formattedDuration}));
        }

        this.api.sendChatMessages(chatMessages, sender, sendingServer);
    }
}
