package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
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
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class WarningListener implements Registerable {
    private static final Pattern NAME_PATTERN = Pattern.compile("\\{name}");
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("\\{message}");
    private final WarningStorage warningStorage;
    private final Map<Integer, List<String>> warningCommandsMap;
    private final SockExchangeApi api;
    private final MessageFormatMap formatMap;
    private final Consumer<ReceivedMessage> warnChannelListener;
    private final Consumer<ReceivedMessage> unwarnChannelListener;

    public WarningListener(WarningStorage warningStorage, Map<Integer, List<String>> warningCommandsMap, SockExchangeApi api, MessageFormatMap formatMap) {
        Preconditions.checkNotNull(warningStorage, "warningStorage");
        Preconditions.checkNotNull(warningCommandsMap, "warningCommandsMap");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(formatMap, "formatMap");
        this.warningStorage = warningStorage;
        this.warningCommandsMap = warningCommandsMap;
        this.api = api;
        this.formatMap = formatMap;
        this.warnChannelListener = this::onWarnChannelRequest;
        this.unwarnChannelListener = this::onUnwarnChannelRequest;
    }

    public void register() {
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.register("Warn", this.warnChannelListener);
        messageNotifier.register("Unwarn", this.unwarnChannelListener);
    }

    public void unregister() {
        ReceivedMessageNotifier messageNotifier = this.api.getMessageNotifier();
        messageNotifier.unregister("Warn", this.warnChannelListener);
        messageNotifier.unregister("Unwarn", this.unwarnChannelListener);
    }

    private void onWarnChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String warner = in.readUTF();
        String nameToWarn = in.readUTF();
        String message = in.readUTF();
        boolean isSilent = in.readBoolean();
        message = message.equals("") ? this.formatMap.format("DefaultMessage/Warn", new Object[0]) : message;
        WarningEntry warningEntry = new WarningEntry(nameToWarn, warner, message);
        int count = this.warningStorage.addWarning(warningEntry);
        String announcement = this.formatMap.format("Announce/Warn", new Object[]{warner, nameToWarn, message});
        this.announce(announcement, isSilent);
        List<String> commandFormats = (List)this.warningCommandsMap.getOrDefault(count, Collections.emptyList());
        byte[] bytes = this.getBytesForCommandsToRunForWarning(nameToWarn, message, commandFormats);
        receivedMessage.respond(bytes);
    }

    private void onUnwarnChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        String sendingServer = in.readUTF();
        String warner = in.readUTF();
        String nameToUnwarn = in.readUTF();
        int amountToUnwarn = in.readInt();
        boolean isSilent = in.readBoolean();
        int removeCount = this.warningStorage.removeWarnings(nameToUnwarn, amountToUnwarn);
        String chatMessage;
        if (removeCount == 0) {
            chatMessage = this.formatMap.format("WarningsNotFound", new Object[]{nameToUnwarn});
            this.api.sendChatMessages(Collections.singletonList(chatMessage), warner, sendingServer);
        } else {
            chatMessage = this.formatMap.format("Announce/Unwarn", new Object[]{warner, nameToUnwarn, String.valueOf(removeCount)});
            this.announce(chatMessage, isSilent);
        }

    }

    private void announce(String announcement, boolean isSilent) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(announcement.length() * 2);
        out.writeBoolean(isSilent);
        out.writeUTF(announcement);
        this.api.sendToServers("DeltaBansAnnouncements", out.toByteArray());
    }

    private byte[] getBytesForCommandsToRunForWarning(String nameToWarn, String message, List<String> commandFormats) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput(512);
        out.writeInt(commandFormats.size());
        Iterator var5 = commandFormats.iterator();

        while(var5.hasNext()) {
            String commandFormat = (String)var5.next();
            String withNameReplaced = NAME_PATTERN.matcher(commandFormat).replaceAll(nameToWarn);
            String withMessageReplaced = MESSAGE_PATTERN.matcher(withNameReplaced).replaceAll(message);
            out.writeUTF(withMessageReplaced);
        }

        return out.toByteArray();
    }
}
