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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.Shared.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.Events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
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

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.format;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class BanListener implements Listener, Registerable, Shutdownable
{
    private final String HIDDEN_IP = ChatColor.translateAlternateColorCodes(
        '&',
        "&k255&r&6.&k255&r&6.&k255&r&6.&k255&r");

    private BanStorage banStorage;
    private RangeBanStorage rangeBanStorage;
    private WhitelistStorage whitelistStorage;
    private DeltaBans plugin;

    public BanListener(DeltaBans plugin)
    {
        this.plugin = plugin;
        this.banStorage = plugin.getBanStorage();
        this.rangeBanStorage = plugin.getRangeBanStorage();
        this.whitelistStorage = plugin.getWhitelistStorage();
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
        this.whitelistStorage = null;
        this.rangeBanStorage = null;
        this.banStorage = null;
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
            if(!whitelistStorage.isOnRangeBanWhitelist(playerName))
            {
                event.setCancelReason(getKickMessage(rangeBanEntry));
                event.setCancelled(true);

                logDeniedLoginAttempt(
                    playerName,
                    address,
                    rangeBanEntry.getMessage(),
                    rangeBanEntry.getBanner());
                return;
            }
        }

        BanEntry banEntry = banStorage.getBanEntry(playerName, address);

        if(banEntry != null)
        {
            event.setCancelReason(getKickMessage(banEntry));
            event.setCancelled(true);

            logDeniedLoginAttempt(
                playerName,
                address,
                banEntry.getMessage(),
                banEntry.getBanner());
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        List<String> messageParts = event.getMessageParts();

        if(channel.equals(DeltaBansChannels.BAN))
        {
            String name = messageParts.get(0);
            String ip = messageParts.get(1);
            String banner = messageParts.get(2);
            String message = messageParts.get(3);
            Long duration = parseLongFromHexString(messageParts.get(4));
            boolean isSilent = messageParts.get(5).equals("1");

            handleBanDeltaRedisMessageEvent(
                event.getSendingServer(),
                name,
                ip,
                banner,
                message,
                duration,
                isSilent);
        }
        else if(channel.equals(DeltaBansChannels.UNBAN))
        {
            String sender = messageParts.get(0);
            String banee = messageParts.get(1);
            boolean isIp = messageParts.get(2).equals("1");
            boolean isSilent = messageParts.get(3).equals("1");

            handleUnbanDeltaRedisMessageEvent(
                event.getSendingServer(),
                sender,
                banee,
                isIp,
                isSilent);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_BAN))
        {
            String banner = messageParts.get(0);
            String message = messageParts.get(1);
            String start = messageParts.get(2);
            String end = messageParts.get(3);
            boolean isSilent = messageParts.get(4).equals("1");

            handleRangeBanDeltaRedisMessageEvent(
                banner,
                message,
                start,
                end,
                isSilent);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_UNBAN))
        {
            String banner = messageParts.get(0);
            String ip = messageParts.get(1);
            boolean isSilent = messageParts.get(2).equals("1");

            handleRangeUnbanDeltaRedisMessageEvent(
                banner,
                ip,
                isSilent);
        }
    }

    private void handleBanDeltaRedisMessageEvent(String sendingServer, String name, String ip,
                                                 String banner, String message, Long duration,
                                                 boolean isSilent)
    {
        name = name.equals("") ? null : name;
        ip = ip.equals("") ? null : name;
        message = message == null ? format("DeltaBans.DefaultMessage.Ban") : message;

        BanEntry entry = new BanEntry(name, ip, banner, message, duration);
        BanStorage.AddResult addResult = banStorage.addBanEntry(entry);

        if(addResult == BanStorage.AddResult.NAME_ALREADY_BANNED)
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                banner,
                format("DeltaBans.AlreadyBanned", name));
        }
        else if(addResult == BanStorage.AddResult.IP_ALREADY_BANNED)
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                banner,
                format("DeltaBans.AlreadyBanned", ip));
        }
        else if(addResult == BanStorage.AddResult.NAME_AND_IP_ALREADY_BANNED)
        {
            DeltaRedisApi.instance().sendMessageToPlayer(
                sendingServer,
                banner,
                format("DeltaBans.NameAndIpAlreadyBanned", name, ip));
        }
        else if(addResult == BanStorage.AddResult.SUCCESS)
        {
            kickOffProxy(name, ip, getKickMessage(entry));

            String announcement = formatBanAnnouncement(entry, isSilent);
            announce(announcement, isSilent);
        }
    }

    private void handleUnbanDeltaRedisMessageEvent(String sendingServer, String sender,
                                                   String banee, boolean isIp, boolean isSilent)
    {
        if(isIp)
        {
            List<BanEntry> entries = banStorage.removeUsingIp(banee);
            if(entries == null || entries.size() == 0)
            {
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.BanNotFound", banee));
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
                DeltaRedisApi.instance().sendMessageToPlayer(
                    sendingServer,
                    sender,
                    format("DeltaBans.BanNotFound", banee));
            }
            else
            {
                String announcement = formatUnbanAnnouncement(sender, banee, false, isSilent);
                announce(announcement, isSilent);
            }
        }
    }

    private void handleRangeBanDeltaRedisMessageEvent(String banner, String message, String start,
                                                      String end, boolean isSilent)
    {
        if(message == null)
        {
            message = format("DeltaBans.DefaultMessage.RangeBan");
        }

        RangeBanEntry entry = new RangeBanEntry(banner, message, start, end);
        rangeBanStorage.add(entry);

        kickOffProxy(
            entry.getStartAddressLong(),
            entry.getEndAddressLong(),
            entry.getMessage());

        String announcement = format(
            "DeltaBans.Announcement.RangeBan",
            banner,
            start + "-" + end,
            message);
        announce(announcement, isSilent);
    }

    private void handleRangeUnbanDeltaRedisMessageEvent(String banner, String ip, boolean isSilent)
    {
        int count = rangeBanStorage.removeIpRangeBan(ip);
        String announcement = format(
            "DeltaBans.Announcement.RangeUnban",
            banner,
            String.valueOf(count),
            ip);
        announce(announcement, isSilent);
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

    private void kickOffProxy(long startIp, long endIp, String message)
    {
        BaseComponent[] kickMessage = TextComponent.fromLegacyText(message);

        for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
        {
            long ipAsLong = DeltaBansUtils.convertIpToLong(
                player.getAddress().getAddress().getHostAddress());

            if(ipAsLong >= startIp && ipAsLong <= endIp)
            {
                player.disconnect(kickMessage.clone());
            }
        }
    }

    private String formatBanAnnouncement(BanEntry entry, boolean isSilent)
    {
        if(entry.hasName() || isSilent)
        {
            return format(
                "DeltaBans.Announcement.Ban",
                entry.getBanner(),
                (entry.hasName()) ? entry.getName() : entry.getIp(),
                entry.getMessage(),
                DeltaBansUtils.formatDuration(entry.getDuration()));
        }
        else
        {
            return format(
                "DeltaBans.Announcement.Ban",
                entry.getBanner(),
                HIDDEN_IP,
                entry.getMessage(),
                DeltaBansUtils.formatDuration(entry.getDuration()));
        }
    }

    private String formatUnbanAnnouncement(String sender, String banee,
                                           boolean isIp, boolean isSilent)
    {
        if(isIp && !isSilent)
        {
            return format("DeltaBans.Announcement.Unban", sender, HIDDEN_IP);
        }
        else
        {
            return format("DeltaBans.Announcement.Unban", sender, banee);
        }
    }

    private void announce(String announcement, boolean isSilent)
    {
        if(isSilent)
        {
            DeltaRedisApi.instance().sendServerAnnouncement(
                Servers.SPIGOT,
                format("DeltaBans.SilentPrefix") + announcement,
                "DeltaBans.SeeSilent");
        }
        else
        {
            DeltaRedisApi.instance().sendServerAnnouncement(
                Servers.SPIGOT,
                announcement,
                "");
        }
    }

    private String getKickMessage(BanEntry entry)
    {
        if(entry.hasDuration())
        {
            long currentTime = System.currentTimeMillis();
            long remainingTime = entry.getCreatedAt() + entry.getDuration() - currentTime;

            return format(
                "DeltaBans.TemporaryBanMessage",
                entry.getMessage(),
                entry.getBanner(),
                DeltaBansUtils.formatDuration(remainingTime));
        }
        else
        {
            return format(
                "DeltaBans.PermanentBanMessage",
                entry.getMessage(),
                entry.getBanner());
        }
    }

    private String getKickMessage(RangeBanEntry entry)
    {
        return format(
            "DeltaBans.RangeBanMessage",
            entry.getMessage(),
            entry.getBanner());
    }

    private void logDeniedLoginAttempt(String name, String ip, String reason, String banner)
    {
        plugin.info("[LoginAttempt] " + name + " @ " + ip + " for \"" + reason + "\" by " + banner);
    }

    private Long parseLongFromHexString(String input)
    {
        try
        {
            if(input.equals("")) { return null; }

            return Long.parseLong(input, 16);
        }
        catch(NumberFormatException e)
        {
            return null;
        }
    }
}
