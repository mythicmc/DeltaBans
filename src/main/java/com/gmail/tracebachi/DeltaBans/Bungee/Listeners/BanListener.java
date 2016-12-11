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
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
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
        plugin.debug("BanListener registered.");
    }

    @Override
    public void unregister()
    {
        plugin.getProxy().getPluginManager().unregisterListener(this);
        plugin.debug("BanListener unregistered.");
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
        DeltaRedisApi api = DeltaRedisApi.instance();
        String channel = event.getChannel();

        if(channel.equals(DeltaBansChannels.BAN))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 6);
            String name = splitMessage[0].equals("") ? null : splitMessage[0];
            String ip = splitMessage[1].equals("") ? null : splitMessage[1];
            String banner = splitMessage[2];
            String message = splitMessage[3];
            Long duration = splitMessage[4].equals("") ? null : Long.valueOf(splitMessage[4], 16);
            boolean isSilent = splitMessage[5].equals("1");

            BanEntry entry = new BanEntry(name, ip, banner, message, duration);
            BanStorage.AddResult addResult = banStorage.addBanEntry(entry);

            if(addResult == BanStorage.AddResult.NAME_ALREADY_BANNED)
            {
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    banner,
                    Settings.format("NameAlreadyBanned", name));
            }
            else if(addResult == BanStorage.AddResult.IP_ALREADY_BANNED)
            {
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    banner,
                    Settings.format("IpAlreadyBanned", ip));
            }
            else if(addResult == BanStorage.AddResult.NAME_AND_IP_ALREADY_BANNED)
            {
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    banner,
                    Settings.format("NameAndIpAlreadyBanned", name, ip));
            }
            else if(addResult == BanStorage.AddResult.SUCCESS)
            {
                kickOffProxy(name, ip, getKickMessage(entry));

                String announcement = formatBanAnnouncement(entry, isSilent);
                announce(announcement, isSilent);
            }
        }
        else if(channel.equals(DeltaBansChannels.UNBAN))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String sender = splitMessage[0];
            String banee = splitMessage[1];
            boolean isIp = splitMessage[2].equals("1");
            boolean isSilent = splitMessage[3].equals("1");

            if(isIp)
            {
                List<BanEntry> entries = banStorage.removeUsingIp(banee);

                if(entries == null || entries.size() == 0)
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
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
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("BanNotFound", banee));
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, false, isSilent);
                    announce(announcement, isSilent);
                }
            }
        }
        else if(channel.equals(DeltaBansChannels.RANGE_BAN))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 5);
            String banner = splitMessage[0];
            String message = splitMessage[1];
            String start = splitMessage[2];
            String end = splitMessage[3];
            boolean isSilent = splitMessage[4].equals("1");

            RangeBanEntry entry = new RangeBanEntry(banner, message, start, end);
            rangeBanStorage.add(entry);
            kickOffProxy(
                entry.getStartAddressLong(),
                entry.getEndAddressLong(),
                entry.getMessage());

            String announcement = Settings.format(
                "RangeBanAnnouncement",
                banner,
                start + "-" + end,
                message);
            announce(announcement, isSilent);
        }
        else if(channel.equals(DeltaBansChannels.RANGE_UNBAN))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 3);
            String banner = splitMessage[0];
            String ip = splitMessage[1];
            boolean isSilent = splitMessage[2].equals("1");
            int count = rangeBanStorage.removeIpRangeBan(ip);

            String announcement = Settings.format(
                "RangeUnbanAnnouncement",
                banner,
                String.valueOf(count),
                ip);
            announce(announcement, isSilent);
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
        if(!entry.hasDuration())
        {
            if(!entry.hasName() && !isSilent)
            {
                return Settings.format(
                    "BanAnnouncement",
                    entry.getBanner(),
                    HIDDEN_IP,
                    entry.getMessage());
            }
            else
            {
                return Settings.format(
                    "BanAnnouncement",
                    entry.getBanner(),
                    (entry.hasName()) ? entry.getName() : entry.getIp(),
                    entry.getMessage());
            }
        }
        else
        {
            if(!entry.hasName() && !isSilent)
            {
                return Settings.format(
                    "BanAnnouncement",
                    entry.getBanner(),
                    HIDDEN_IP,
                    DeltaBansUtils.formatDuration(entry.getDuration()),
                    entry.getMessage());
            }
            else
            {
                return Settings.format(
                    "BanAnnouncement",
                    entry.getBanner(),
                    (entry.hasName()) ? entry.getName() : entry.getIp(),
                    DeltaBansUtils.formatDuration(entry.getDuration()),
                    entry.getMessage());
            }
        }
    }

    private String formatUnbanAnnouncement(String sender, String banee,
                                           boolean isIp, boolean isSilent)
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
            DeltaRedisApi.instance().sendAnnouncementToServer(
                Servers.SPIGOT,
                Settings.format("SilentPrefix") + announcement,
                "DeltaBans.SeeSilent");
        }
        else
        {
            DeltaRedisApi.instance().sendAnnouncementToServer(
                Servers.SPIGOT,
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

            result = Settings.format(
                "TemporaryBanMessage",
                entry.getMessage(),
                entry.getBanner(),
                DeltaBansUtils.formatDuration(remainingTime));
        }
        else
        {
            result = Settings.format(
                "PermanentBanMessage",
                entry.getMessage(),
                entry.getBanner());
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String getKickMessage(RangeBanEntry entry)
    {
        String result = Settings.format(
            "RangeBanMessage",
            entry.getMessage(),
            entry.getBanner());

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private void logDeniedLoginAttempt(String name, String ip, String reason, String banner)
    {
        plugin.info("[LoginAttempt] " + name + " @ " + ip + " for \"" + reason + "\" by " + banner);
    }
}
