package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.SockExchange.Bungee.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class CheckWarningsListener implements Registerable {
    private final WarningStorage warningStorage;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> checkWarningsChannelListener;

    public CheckWarningsListener(WarningStorage warningStorage, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(warningStorage, "warningStorage");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.warningStorage = warningStorage;
        this.api = api;
        this.formatMap = formatMap;
        this.checkWarningsChannelListener = this::onCheckWarningsChannelRequest;
    }

    public void register() {
        this.api.getMessageNotifier().register("CheckWarnings", this.checkWarningsChannelListener);
    }

    public void unregister() {
        this.api.getMessageNotifier().unregister("CheckWarnings", this.checkWarningsChannelListener);
    }

    private void onCheckWarningsChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String sender = in.readUTF();
        String nameToCheck = in.readUTF();
        List<String> chatMessages = new ArrayList(4);
        List<WarningEntry> warningEntryList = this.warningStorage.getWarnings(nameToCheck);
        if (warningEntryList != null && !warningEntryList.isEmpty()) {
            Iterator var8 = warningEntryList.iterator();

            while(var8.hasNext()) {
                WarningEntry warningEntry = (WarningEntry)var8.next();
                String warner = warningEntry.getWarner();
                String message = warningEntry.getMessage();
                chatMessages.add(this.formatMap.format("WarningInfoLine", new Object[]{warner, message}));
            }
        } else {
            chatMessages.add(this.formatMap.format("WarningsNotFound", new Object[]{nameToCheck}));
        }

        this.api.sendChatMessages(chatMessages, sender, sendingServer);
    }
}
