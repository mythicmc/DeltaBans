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
package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Bungee.Prefixes;
import com.gmail.tracebachi.DeltaRedis.Shared.Redis.Servers;
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
public class BanListener implements Listener
{
    private final String HIDDEN_IP = "\u00A7k255\u00A7r\u00A76.\u00A7k255\u00A7r\u00A76.\u00A7k255\u00A7r\u00A76.\u00A7k255\u00A7r";

    private String permanentBanFormat;
    private String temporaryBanFormat;
    private String rangeBanFormat;
    private BanStorage banStorage;
    private RangeBanStorage rangeBanStorage;
    private Set<String> rangeBanWhitelist;
    private DeltaRedisApi deltaRedisApi;

    public BanListener(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        this.permanentBanFormat = plugin.getPermanentBanFormat();
        this.temporaryBanFormat = plugin.getTemporaryBanFormat();
        this.rangeBanFormat = plugin.getRangeBanFormat();
        this.banStorage = plugin.getBanStorage();
        this.rangeBanStorage = plugin.getRangeBanStorage();
        this.rangeBanWhitelist = plugin.getRangeBanWhitelist();
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.permanentBanFormat = null;
        this.temporaryBanFormat = null;
        this.rangeBanWhitelist = null;
        this.rangeBanFormat = null;
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
                    Prefixes.FAILURE + "IP is already banned.");
            }
            else if(hasName && isIpBanned && banStorage.isNameBanned(name))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), banner,
                    Prefixes.FAILURE + "Name and IP is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, ip, banner, banMessage, duration);
                banStorage.add(entry);
                kickOffProxy(name, ip, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, !hasName,
                    banMessage, entry.hasDuration(), isSilent);
                deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, true, isSilent);
                    deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
                }
            }
            else
            {
                BanEntry entry = banStorage.removeUsingName(banee.toLowerCase());

                if(entry == null)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), sender,
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, false, isSilent);
                    deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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
                    Prefixes.FAILURE + "Name is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, null, banner, banMessage, null);
                banStorage.add(entry);
                kickOffProxy(name, null, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, false, banMessage, false, isSilent);
                deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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

            String announcement = formatRangeBanAnnouncement(banner, start + "-" + end, message, isSilent);
            deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_UNBAN))
        {
            String banner = in.readUTF();
            String ip = in.readUTF();
            boolean isSilent = in.readBoolean();
            int count = rangeBanStorage.removeIpRangeBan(ip);

            String announcement = formatRangeUnbanAnnouncement(banner, ip, count, isSilent);
            deltaRedisApi.publish(Servers.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_WHITELIST))
        {
            String senderName = in.readUTF();
            String nameToUpdate = in.readUTF();
            boolean isAdd = in.readBoolean();

            if(isAdd)
            {
                rangeBanWhitelist.add(nameToUpdate.toLowerCase());
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                    Prefixes.SUCCESS + Prefixes.input(nameToUpdate) + " added to range whitelist.");
            }
            else
            {
                rangeBanWhitelist.remove(nameToUpdate.toLowerCase());
                deltaRedisApi.sendMessageToPlayer(event.getSendingServer(), senderName,
                    Prefixes.SUCCESS + Prefixes.input(nameToUpdate) + " removed from range whitelist.");
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
        boolean isTemporary, boolean isSilent)
    {
        return ((isSilent) ? "!" : "") +
            ChatColor.GOLD + banner +
            ChatColor.WHITE + ((isTemporary) ? " temp-banned " : " banned ") +
            ChatColor.GOLD + ((isIp) ? HIDDEN_IP : banee) +
            ChatColor.WHITE + " for " +
            ChatColor.GOLD + message;
    }

    private String formatRangeBanAnnouncement(String banner, String range, String message, boolean isSilent)
    {
        return ((isSilent) ? "!" : "") +
            ChatColor.GOLD + banner +
            ChatColor.WHITE + " range-banned " +
            ChatColor.GOLD + range +
            ChatColor.WHITE + " for " +
            ChatColor.GOLD + message;
    }

    private String formatUnbanAnnouncement(String sender, String banee, boolean isIp, boolean isSilent)
    {
        return ((isSilent) ? "!" : "") +
            ChatColor.GOLD + sender +
            ChatColor.WHITE + " unbanned " +
            ChatColor.GOLD + ((isIp) ? HIDDEN_IP : banee);
    }

    private String formatRangeUnbanAnnouncement(String sender, String ip, int count, boolean isSilent)
    {
        return ((isSilent) ? "!" : "") +
            ChatColor.GOLD + sender +
            ChatColor.WHITE + " unbanned " +
            ChatColor.GOLD + count +
            ChatColor.WHITE + " IP ranges overlapping with " +
            ChatColor.GOLD + ip;
    }

    private String getKickMessage(BanEntry entry)
    {
        String result;

        if(entry.hasDuration())
        {
            long currentTime = System.currentTimeMillis();
            long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;

            result = String.format(temporaryBanFormat, entry.getMessage(), entry.getBanner(),
                DeltaBansUtils.formatDuration(remainingTime));
        }
        else
        {
            result = String.format(permanentBanFormat, entry.getMessage(), entry.getBanner());
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String getKickMessage(RangeBanEntry entry)
    {
        String result = String.format(rangeBanFormat, entry.getMessage(), entry.getBanner());
        return ChatColor.translateAlternateColorCodes('&', result);
    }
}
