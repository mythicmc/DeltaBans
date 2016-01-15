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
package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.yahoo.tracebachi.DeltaBans.Bungee.Storage.BanEntry;
import com.yahoo.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.yahoo.tracebachi.DeltaBans.Bungee.Storage.RangeBanEntry;
import com.yahoo.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.yahoo.tracebachi.DeltaBans.DeltaBansChannels;
import com.yahoo.tracebachi.DeltaBans.DeltaBansUtils;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Bungee.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
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
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BanListener implements Listener
{
    private final String HIDDEN_IP = "\u00A7k255\u00A7r.\u00A7k255\u00A7r.\u00A7k255\u00A7r.\u00A7k255\u00A7r";

    private String permanentBanFormat;
    private String temporaryBanFormat;
    private String rangeBanFormat;
    private BanStorage banStorage;
    private RangeBanStorage rangeBanStorage;
    private DeltaRedisApi deltaRedisApi;

    public BanListener(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        this.permanentBanFormat = plugin.getPermanentBanFormat();
        this.temporaryBanFormat = plugin.getTemporaryBanFormat();
        this.rangeBanFormat = plugin.getRangeBanFormat();
        this.banStorage = plugin.getBanStorage();
        this.rangeBanStorage = plugin.getRangeBanStorage();
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.permanentBanFormat = null;
        this.temporaryBanFormat = null;
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
            event.setCancelReason(getKickMessage(rangeBanEntry));
            event.setCancelled(true);
            return;
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

            for(BanEntry entry : ipBanEntries)
            {
                if(!entry.isDurationComplete())
                {
                    entryWithDuration = entry;
                }
            }

            if(entryWithDuration == null)
            {
                banStorage.removeUsingIp(address);
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
            if(!hasName && banStorage.isIpBanned(ip))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "IP is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, ip, banner, banMessage, duration);
                banStorage.add(entry);
                kickOffProxy(name, ip, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, hasName,
                    banMessage, entry.hasDuration(), isSilent);
                deltaRedisApi.publish(Channels.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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
                    deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, true, isSilent);
                    deltaRedisApi.publish(Channels.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
                }
            }
            else
            {
                BanEntry entry = banStorage.removeUsingName(banee.toLowerCase());

                if(entry == null)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, false, isSilent);
                    deltaRedisApi.publish(Channels.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "Name is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, null, banner, banMessage, null);
                banStorage.add(entry);
                kickOffProxy(name, null, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, false, banMessage, false, isSilent);
                deltaRedisApi.publish(Channels.SPIGOT, DeltaBansChannels.ANNOUNCE, announcement);
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
                proxiedPlayer.disconnect(componentMessage);
            }
        }

        if(ip != null)
        {
            for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
            {
                if(player.getAddress().toString().equals(ip))
                {
                    player.disconnect(componentMessage);
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

    private String formatUnbanAnnouncement(String sender, String banee, boolean isIp, boolean isSilent)
    {
        return ((isSilent) ? "!" : "") +
            ChatColor.GOLD + sender +
            ChatColor.WHITE + " unbanned " +
            ChatColor.GOLD + ((isIp) ? HIDDEN_IP : banee);
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
