package com.gmail.tracebachi.DeltaBans.Spigot;

import com.gmail.tracebachi.SockExchange.Messages.ReceivedMessage;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataInput;
import java.util.Iterator;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

class AnnouncementListener implements Registerable {
    private static final String SEE_SILENT_PERM = "DeltaBans.SeeSilent";
    private final DeltaBansPlugin plugin;
    private final SockExchangeApi api;
    private final String silentAnnouncementPrefix;
    private final Consumer<ReceivedMessage> announcementChannelListener;

    public AnnouncementListener(DeltaBansPlugin plugin, SockExchangeApi api, String silentAnnouncementPrefix) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(api, "api");
        Preconditions.checkNotNull(silentAnnouncementPrefix, "silentAnnouncementPrefix");
        this.plugin = plugin;
        this.api = api;
        this.silentAnnouncementPrefix = silentAnnouncementPrefix;
        this.announcementChannelListener = this::onAnnouncementChannelRequest;
    }

    public void register() {
        this.api.getMessageNotifier().register("DeltaBansAnnouncements", this.announcementChannelListener);
    }

    public void unregister() {
        this.api.getMessageNotifier().unregister("DeltaBansAnnouncements", this.announcementChannelListener);
    }

    private void onAnnouncementChannelRequest(ReceivedMessage receivedMessage) {
        ByteArrayDataInput in = receivedMessage.getDataInput();
        boolean isSilent = in.readBoolean();
        String announcement = in.readUTF();
        if (isSilent) {
            announcement = this.silentAnnouncementPrefix + announcement;
        }

        Iterator var5 = this.plugin.getServer().getOnlinePlayers().iterator();

        while(true) {
            Player player;
            do {
                if (!var5.hasNext()) {
                    this.plugin.getServer().getConsoleSender().sendMessage(announcement);
                    return;
                }

                player = (Player)var5.next();
            } while(isSilent && !player.hasPermission("DeltaBans.SeeSilent"));

            player.sendMessage(announcement);
        }
    }
}
