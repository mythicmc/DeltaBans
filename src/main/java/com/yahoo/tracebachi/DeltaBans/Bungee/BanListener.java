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
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Bungee.Prefixes;
import com.yahoo.tracebachi.DeltaRedis.Shared.Redis.Channels;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.api.ChatColor;
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
    private static final String BAN_CHANNEL = "DB-Ban";
    private static final String UNBAN_CHANNEL = "DB-Unban";
    private static final String NAME_BAN_CHANNEL = "DB-NameBan";
    private static final String CHECK_BAN_CHANNEL = "DB-CheckBan";
    private static final String SAVE_BANS_CHANNEL = "DB-SaveBans";
    private static final String ANNOUNCE = "DB-Announce";

    private String permanentBanFormat;
    private String temporaryBanFormat;
    private BanStorage banStorage;
    private DeltaRedisApi deltaRedisApi;
    private DeltaBansPlugin plugin;

    public BanListener(DeltaRedisApi deltaRedisApi, DeltaBansPlugin plugin)
    {
        this.permanentBanFormat = plugin.getPermanentBanFormat();
        this.temporaryBanFormat = plugin.getTemporaryBanFormat();
        this.banStorage = plugin.getBanStorage();
        this.deltaRedisApi = deltaRedisApi;
        this.plugin = plugin;
    }

    public void shutdown()
    {
        this.banStorage = null;
        this.deltaRedisApi = null;
        this.plugin = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent event)
    {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName().toLowerCase();
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

        String address = pending.getAddress().getAddress().getHostAddress();
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

        if(channel.equals(BAN_CHANNEL))
        {
            String banner = in.readUTF();
            String banMessage = in.readUTF();
            String ip = in.readUTF();
            Long duration = Long.valueOf(in.readUTF(), 16);
            boolean hasName = in.readBoolean();
            String name = null;

            if(duration == 0)
            {
                duration = null;
            }

            if(hasName)
            {
                name = in.readUTF().toLowerCase();
            }

            // If an IP is being banned which is already banned, but also has no name
            if(!hasName && banStorage.isIpBanned(ip))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "IP is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, ip, banner,
                    banMessage, duration, System.currentTimeMillis());
                banStorage.add(entry);

                // Kick player off the proxy
                if(name != null)
                {
                    kickByName(name, getKickMessage(entry));
                }
                else
                {
                    kickByIp(ip, getKickMessage(entry));
                }

                String announcement = formatBanAnnouncement(banner, name, hasName,
                    banMessage, entry.hasDuration());
                deltaRedisApi.publish(Channels.SPIGOT, ANNOUNCE, announcement);
            }
        }
        else if(channel.equals(UNBAN_CHANNEL))
        {
            String sender = in.readUTF();
            String banee = in.readUTF();
            boolean isName = in.readBoolean();

            if(isName)
            {
                BanEntry entry = banStorage.removeUsingName(banee.toLowerCase());

                if(entry == null)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, true);
                    deltaRedisApi.publish(Channels.SPIGOT, ANNOUNCE, announcement);
                }
            }
            else
            {
                Set<BanEntry> entries = banStorage.removeUsingIp(banee);

                if(entries == null || entries.size() == 0)
                {
                    deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                        Prefixes.INFO + "Ban not found for " + banee);
                }
                else
                {
                    String announcement = formatUnbanAnnouncement(sender, banee, false);
                    deltaRedisApi.publish(Channels.SPIGOT, ANNOUNCE, announcement);
                }
            }
        }
        else if(channel.equals(NAME_BAN_CHANNEL))
        {
            String banner = in.readUTF();
            String banMessage = in.readUTF();
            String name = in.readUTF().toLowerCase();

            if(banStorage.isNameBanned(name))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "Name is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, null, banner,
                    banMessage, null, System.currentTimeMillis());
                banStorage.add(entry);

                // Kick player off the proxy
                kickByName(name, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, true, banMessage, false);
                deltaRedisApi.publish(Channels.SPIGOT, ANNOUNCE, announcement);
            }
        }
        else if(channel.equals(CHECK_BAN_CHANNEL))
        {
            String sender = in.readUTF();
            String argument = in.readUTF();
            boolean isName = in.readBoolean();
            boolean includeIp = in.readBoolean();

            if(isName)
            {
                BanEntry entry = banStorage.getNameBanEntry(argument.toLowerCase());
                sendCheckBanInfo(event.getSender(), sender, entry, includeIp);
            }
            else
            {
                Set<BanEntry> entries = banStorage.getIpBanEntries(argument);
                for(BanEntry entry : entries)
                {
                    sendCheckBanInfo(event.getSender(), sender, entry, includeIp);
                }
            }
        }
        else if(channel.equals(SAVE_BANS_CHANNEL))
        {
            String sender = event.getMessage();
            if(plugin.writeBansAndWarnings())
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                    Prefixes.SUCCESS + "Ban and warning files saved.");
            }
            else
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                    Prefixes.FAILURE + "Error saving files. More details in the BungeeCord console.");
            }
        }
    }

    private void kickByName(String name, String message)
    {
        ProxiedPlayer proxiedPlayer = BungeeCord.getInstance().getPlayer(name);
        if(proxiedPlayer != null)
        {
            proxiedPlayer.disconnect(TextComponent.fromLegacyText(message));
        }
    }

    private void kickByIp(String ip, String message)
    {
        for(ProxiedPlayer player : BungeeCord.getInstance().getPlayers())
        {
            if(player.getAddress().toString().equals(ip))
            {
                player.disconnect(TextComponent.fromLegacyText(message));
            }
        }
    }

    private String formatBanAnnouncement(String banner, String banee, boolean isName,
        String message, boolean isTemporary)
    {
        if(!isName)
        {
            banee = "255.255.255.255";
        }
        return ChatColor.GOLD + banner +
            ChatColor.WHITE + ((isTemporary) ? " temp-banned " : " banned ") +
            ChatColor.GOLD + banee +
            ChatColor.WHITE + " for " +
            ChatColor.GOLD + message;
    }

    private String formatUnbanAnnouncement(String sender, String banee, boolean isName)
    {
        if(!isName)
        {
            banee = "255.255.255.255";
        }
        return ChatColor.GOLD + sender +
            ChatColor.WHITE + " unbanned " +
            ChatColor.GOLD + banee;
    }

    private String getKickMessage(BanEntry entry)
    {
        String result;
        if(entry.hasDuration())
        {
            long remainingTime = entry.getCreatedAt() + entry.getDuration()
                - System.currentTimeMillis();
            result = String.format(temporaryBanFormat, entry.getMessage(),
                entry.getBanner(), formatDuration(remainingTime));
        }
        else
        {
            result = String.format(permanentBanFormat, entry.getMessage(),
                entry.getBanner());
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private void sendCheckBanInfo(String server, String sender, BanEntry entry, boolean includeIp)
    {
        if(entry == null)
        {
            deltaRedisApi.sendMessageToPlayer(server, sender,
                Prefixes.INFO + "Ban not found.");
            return;
        }

        StringBuilder builder = new StringBuilder(Prefixes.INFO + "Ban found\n");
        builder.append(Prefixes.INFO).append("Name: ");
        builder.append(Prefixes.input(entry.getName())).append("\n");

        if(includeIp)
        {
            builder.append(Prefixes.INFO).append("IP: ");
            builder.append(Prefixes.input(entry.getIp())).append("\n");
        }

        builder.append(Prefixes.INFO).append("Banner: ");
        builder.append(Prefixes.input(entry.getBanner())).append("\n");
        builder.append(Prefixes.INFO).append("Ban Message: ");
        builder.append(Prefixes.input(entry.getMessage())).append("\n");
        builder.append(Prefixes.INFO).append("Duration: ");
        builder.append(Prefixes.input(formatDuration(entry.getDuration())));

        deltaRedisApi.sendMessageToPlayer(server, sender, builder.toString());
    }

    private String formatDuration(Long input)
    {
        if(input == null)
        {
            return "Forever!";
        }

        long inputSeconds = input / 1000;
        long days = (inputSeconds / (24 * 60 * 60));
        long hours = (inputSeconds / (60 * 60)) % 24;
        long minutes = (inputSeconds / (60)) % 60;
        long seconds = inputSeconds % 60;

        return days + " days, " +
            hours + " hours, " +
            minutes + " minutes, " +
            seconds + " seconds";
    }
}
