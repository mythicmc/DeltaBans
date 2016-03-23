/*
 * This file is part of DeltaBans.
 *
 * DeltaBans is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DeltaBans is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DeltaBans.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Bungee.Listeners;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBans;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class BanListener implements Listener, Registerable, Shutdownable
{
    private final String HIDDEN_IP = ChatColor.translateAlternateColorCodes('&',
        "&k255&r&6.&k255&r&6.&k255&r&6.&k255&r");

    private BanStorage banStorage;
    private RangeBanStorage rangeBanStorage;
    private Set<String> rangeBanWhitelist;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBans plugin;

    public BanListener(DeltaRedisApi deltaRedisApi, DeltaBans plugin)
    {
        this.banStorage = plugin.getBanStorage();
        this.rangeBanStorage = plugin.getRangeBanStorage();
        this.rangeBanWhitelist = plugin.getRangeBanWhitelist();
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    @Override
    public void register()
    {
        plugin.getProxy().getPluginManager().registerListener(plugin, this);
    }

    @Override
    public void unregister()
    {
        plugin.getProxy().getPluginManager().unregisterListener(this);
    }

    @Override
    public void shutdown()
    {
        unregister();
        this.rangeBanWhitelist = null;
        this.banStorage = null;
        this.rangeBanStorage = null;
        this.deltaRedisApi = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent event)
    {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName().toLowerCase();
        String address = pending.getAddress().getAddress().getHostAddress();
        RangeBanEntry rangeBanEntry = rangeBanStorage.getIpRangeBan(address);

        if(rangeBanEntry != null)
        {
            if(!rangeBanWhitelist.contains(playerName.toLowerCase()))
            {
                event.setCancelReason(getKickMessage(rangeBanEntry));
                event.setCancelled(true);
                return;
            }
        }

        BanEntry nameEntry = banStorage.getNameBanEntry(playerName);

        if(nameEntry != null)
        {
            if(nameEntry.isDurationComplete())
            {
                banStorage.removeUsingName(nameEntry.getName());
            }
            else
            {
                event.setCancelReason(getKickMessage(nameEntry));
                event.setCancelled(true);
                return;
            }
        }

        Set<BanEntry> ipBanEntries = banStorage.getIpBanEntries(address);

        if(ipBanEntries != null)
        {
            BanEntry entryWithDuration = null;
            BanEntry ipBanEntry = null;

            for(BanEntry entry : ipBanEntries)
            {
                if(entry.hasDuration() && !entry.isDurationComplete())
                {
                    entryWithDuration = entry;
                }
                else
                {
                    ipBanEntry = entry;
                }
            }

            if(ipBanEntry == null && entryWithDuration == null)
            {
                banStorage.removeUsingIp(address);
            }
            else if(ipBanEntry != null)
            {
                event.setCancelReason(getKickMessage(ipBanEntry));
                event.setCancelled(true);
            }
            else
            {
                event.setCancelReason(getKickMessage(entryWithDuration));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        byte[] messageBytes = event.getMessage().getBytes(StandardCharsets.UTF_8);
        ByteArrayDataInput in = ByteStreams.newDataInput(messageBytes);

        if(channel.equals(DeltaBansChannels.BAN))
        {
            String banner = in.readUTF();
            String banMessage = in.readUTF();
            String ip = in.readUTF();
            Long duration = Long.valueOf(in.readUTF(), 16);
            boolean isSilent = in.readBoolean();
            boolean hasName = in.readBoolean();
            String name = null;

            if(duration == 0) duration = null;
            if(hasName) name = in.readUTF();

            // If an IP is being banned which is already banned, but also has no name
            boolean isIpBanned = banStorage.isIpBanned(ip);

            if(!hasName && isIpBanned)
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), banner,
                    Settings.format("AlreadyIpBanned", ip));
            }
            else if(hasName && isIpBanned && banStorage.isNameBanned(name))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), banner,
                    Settings.format("AlreadyIpBanned", name));
            }
            else
            {
                BanEntry entry = new BanEntry(name, ip, banner, banMessage, duration);
                banStorage.add(entry);
                kickOffProxy(name, ip, getKickMessage(entry));

                String announcement = formatBanAnnouncement(
                    banner,
                    (hasName) ? name : ip,
                    !hasName,
                    banMessage,
                    entry.getDuration(),
                    isSilent);

                announce(announcement, isSilent);
            }
        }
        else if(channel.equals(DeltaBansChannels.UNBAN))
        {
            String sender = in.readUTF();
            String banee = in.readUTF();
            boolean isIp = in.readBoolean();
            boolean isSilent = in.readBoolean();

            if(isIp)
            {
                Set<BanEntry> entries = banStorage.removeUsingIp(banee);

                if(entries == null || entries.size() == 0)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender,
                        Settings.format("BanNotFound", banee));
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, true, isSilent);

                    announce(announcement, isSilent);
                }
            }
            else
            {
                BanEntry entry = banStorage.removeUsingName(banee.toLowerCase());

                if(entry == null)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender,
                        Settings.format("BanNotFound", banee));
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, false, isSilent);

                    announce(announcement, isSilent);
                }
            }
        }
        else if(channel.equals(DeltaBansChannels.NAME_BAN))
        {
            String banner = in.readUTF();
            String banMessage = in.readUTF();
            String name = in.readUTF().toLowerCase();
            boolean isSilent = in.readBoolean();

            if(banStorage.isNameBanned(name))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), banner,
                    Settings.format("AlreadyIpBanned", name));
            }
            else
            {
                BanEntry entry = new BanEntry(name, null, banner, banMessage, null);
                String announcement = formatBanAnnouncement(banner, name, false, banMessage, null, isSilent);

                banStorage.add(entry);
                kickOffProxy(name, null, getKickMessage(entry));

                announce(announcement, isSilent);
            }
        }
        else if(channel.equals(DeltaBansChannels.RANGE_BAN))
        {
            String banner = in.readUTF();
            String message = in.readUTF();
            String start = in.readUTF();
            String end = in.readUTF();
            boolean isSilent = in.readBoolean();

            RangeBanEntry entry = new RangeBanEntry(banner, message, start, end);
            rangeBanStorage.add(entry);

            BaseComponent[] kickMessage = TextComponent.fromLegacyText(getKickMessage(entry));

            for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
            {
                long ipAsLong = DeltaBansUtils.convertIpToLong(
                    player.getAddress().getAddress().getHostAddress());

                if(ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
                {
                    player.disconnect(kickMessage.clone());
                }
            }

            String announcement = Settings.format("RangeBanAnnouncement",
                banner, start + "-" + end, message);

            announce(announcement, isSilent);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_UNBAN))
        {
            String banner = in.readUTF();
            String ip = in.readUTF();
            boolean isSilent = in.readBoolean();
            int count = rangeBanStorage.removeIpRangeBan(ip);

            String announcement = Settings.format("RangeUnbanAnnouncement",
                banner, String.valueOf(count), ip);

            announce(announcement, isSilent);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_WHITELIST))
        {
            String senderName = in.readUTF();
            String nameToUpdate = in.readUTF();
            boolean isAdd = in.readBoolean();

            if(isAdd)
            {
                if(rangeBanWhitelist.add(nameToUpdate.toLowerCase()))
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                        Settings.format("AddedToRangeBanWhitelist", nameToUpdate));
                }
                else
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                        Settings.format("AlreadyInRangeBanWhitelist", nameToUpdate));
                }
            }
            else
            {
                if(rangeBanWhitelist.remove(nameToUpdate.toLowerCase()))
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                        Settings.format("RemovedFromRangeBanWhitelist", nameToUpdate));
                }
                else
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                        Settings.format("NotInRangeBanWhitelist", nameToUpdate));
                }
            }
        }
    }

    private void kickOffProxy(String name, String ip, String message)
    {
        BaseComponent[] componentMessage = TextComponent.fromLegacyText(message);

        if(name != null)
        {
            ProxiedPlayer proxiedPlayer = BungeeCord.getInstance().getPlayer(name);

            if(proxiedPlayer != null)
            {
                proxiedPlayer.disconnect(componentMessage.clone());
            }
        }

        if(ip != null)
        {
            for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
            {
                if(player.getAddress().getAddress().getHostAddress().equals(ip))
                {
                    player.disconnect(componentMessage.clone());
                }
            }
        }
    }

    private String formatBanAnnouncement(String banner, String banee, boolean isIp, String message,
        Long duration, boolean isSilent)
    {
        if(duration == null)
        {
            if(isIp && !isSilent)
            {
                return Settings.format("BanAnnouncement", banner, HIDDEN_IP, message);
            }
            else
            {
                return Settings.format("BanAnnouncement", banner, banee, message);
            }
        }
        else
        {
            if(isIp && !isSilent)
            {
                return Settings.format("TempBanAnnouncement", banner, HIDDEN_IP,
                    DeltaBansUtils.formatDuration(duration), message);
            }
            else
            {
                return Settings.format("TempBanAnnouncement", banner, banee,
                    DeltaBansUtils.formatDuration(duration), message);
            }
        }
    }

    private String formatUnbanAnnouncement(String sender, String banee, boolean isIp, boolean isSilent)
    {
        if(isIp && !isSilent)
        {
            return Settings.format("UnbanAnnouncement", sender, HIDDEN_IP);
        }
        else
        {
            return Settings.format("UnbanAnnouncement", sender, banee);
        }
    }

    private void announce(String announcement, boolean isSilent)
    {
        if(isSilent)
        {
            deltaRedisApi.sendAnnouncementToServer(Servers.SPIGOT,
                Settings.format("SilentPrefix") + announcement,
                "DeltaBans.SeeSilent");
        }
        else
        {
            deltaRedisApi.sendAnnouncementToServer(Servers.SPIGOT,
                announcement,
                "");
        }
    }

    private String getKickMessage(BanEntry entry)
    {
        String result;

        if(entry.hasDuration())
        {
            long currentTime = System.currentTimeMillis();
            long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;

            result = Settings.format("TemporaryBanMessage",
                entry.getMessage(),
                entry.getBanner(),
                DeltaBansUtils.formatDuration(remainingTime));
        }
        else
        {
            result = Settings.format("PermanentBanMessage",
                entry.getMessage(),
                entry.getBanner());
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String getKickMessage(RangeBanEntry entry)
    {
        String result = Settings.format("RangeBanMessage",
            entry.getMessage(),
            entry.getBanner());

        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
