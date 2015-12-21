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

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class DeltaBanListener implements Listener
{
    private static final String BAN_CHANNEL = "DB-Ban";
    private static final String UNBAN_CHANNEL = "DB-Unban";
    private static final String NAME_BAN_CHANNEL = "DB-NameBan";
    private static final String CHECK_BAN_CHANNEL = "DB-CheckBan";
    private static final String BAN_ANNOUNCE = "DB-Announce";

    private String permanentBanFormat;
    private String temporaryBanFormat;
    private BanStorage storage;
    private DeltaRedisApi deltaRedisApi;

    public DeltaBanListener(String permanentBanFormat, String temporaryBanFormat,
        DeltaRedisApi deltaRedisApi, BanStorage storage)
    {
        this.permanentBanFormat = permanentBanFormat;
        this.temporaryBanFormat = temporaryBanFormat;
        this.storage = storage;
        this.deltaRedisApi = deltaRedisApi;
    }

    public void shutdown()
    {
        this.storage = null;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(LoginEvent event)
    {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName().toLowerCase();
        BanEntry nameEntry = storage.getNameBanEntry(playerName);

        if(nameEntry != null)
        {
            if(nameEntry.isDurationComplete())
            {
                storage.removeUsingName(nameEntry.getName());
            }
            else
            {
                event.setCancelReason(getKickMessage(nameEntry));
                event.setCancelled(true);
                return;
            }
        }

        String address = pending.getAddress().getAddress().getHostAddress();
        BanEntry ipEntry = storage.getIpBanEntry(address);

        if(ipEntry != null)
        {
            if(ipEntry.isDurationComplete())
            {
                storage.removeUsingIp(ipEntry.getIp());
            }
            else
            {
                event.setCancelReason(getKickMessage(ipEntry));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        String channel = event.getChannel();
        ByteArrayDataInput in = ByteStreams.newDataInput(event.getMessage().getBytes(StandardCharsets.UTF_8));

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

            if(storage.isIpBanned(ip))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "IP is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, ip, banner,
                    banMessage, duration, System.currentTimeMillis());
                storage.add(entry);

                // Kick player off the proxy
                if(name != null)
                {
                    kickByName(name, getKickMessage(entry));
                }
                else
                {
                    kickByIp(ip, getKickMessage(entry));
                }

                String announcement = formatBanAnnouncement(banner, name, hasName, banMessage);
                deltaRedisApi.publish(event.getSender(), BAN_ANNOUNCE,
                    banner + "/\\" + announcement);
            }
        }
        else if(channel.equals(UNBAN_CHANNEL))
        {
            String sender = in.readUTF();
            String banee = in.readUTF();
            boolean isName = in.readBoolean();
            BanEntry entry;

            if(isName)
            {
                entry = storage.removeUsingName(banee.toLowerCase());
            }
            else
            {
                entry = storage.removeUsingIp(banee);
            }

            if(entry == null)
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                    Prefixes.INFO + "Ban not found for " + banee);
            }
            else
            {
                String announcement = formatUnbanAnnouncement(sender, banee, isName);
                deltaRedisApi.publish(event.getSender(), BAN_ANNOUNCE,
                    sender + "/\\" + announcement);
            }
        }
        else if(channel.equals(NAME_BAN_CHANNEL))
        {
            String banner = in.readUTF();
            String banMessage = in.readUTF();
            String name = in.readUTF().toLowerCase();

            if(storage.isNameBanned(name))
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), banner,
                    Prefixes.FAILURE + "Name is already banned.");
            }
            else
            {
                BanEntry entry = new BanEntry(name, null, banner,
                    banMessage, null, System.currentTimeMillis());
                storage.add(entry);

                // Kick player off the proxy
                kickByName(name, getKickMessage(entry));

                String announcement = formatBanAnnouncement(banner, name, true, banMessage);
                deltaRedisApi.publish(event.getSender(), BAN_ANNOUNCE,
                    banner + "/\\" + announcement);
            }
        }
        else if(channel.equals(CHECK_BAN_CHANNEL))
        {
            String sender = in.readUTF();
            String argument = in.readUTF();
            boolean isName = in.readBoolean();
            BanEntry entry;

            if(isName)
            {
                entry = storage.getNameBanEntry(argument.toLowerCase());
            }
            else
            {
                entry = storage.getIpBanEntry(argument);
            }

            if(entry == null)
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                    Prefixes.FAILURE + "Ban not found.");
            }
            else
            {
                deltaRedisApi.sendMessageToPlayer(event.getSender(), sender,
                    Prefixes.SUCCESS + "Ban found\n" +
                    Prefixes.INFO + "Name: " + ChatColor.WHITE + entry.getName() + "\n" +
                    Prefixes.INFO + "IP: " + ChatColor.WHITE + entry.getIp() + "\n" +
                    Prefixes.INFO + "Banner: " + ChatColor.WHITE + entry.getBanner() + "\n" +
                    Prefixes.INFO + "Ban Message:" + ChatColor.WHITE + entry.getMessage() + "\n" +
                    Prefixes.INFO + "Duration: " + ChatColor.WHITE + entry.getDuration());
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

    private String formatBanAnnouncement(String banner, String banee, boolean isName, String message)
    {
        if(!isName)
        {
            banee = "255.255.255.255";
        }
        return ChatColor.GOLD + banner +
            ChatColor.WHITE + " banned " +
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
                entry.getBanner(), secondsToString(remainingTime));
        }
        else
        {
            result = String.format(permanentBanFormat, entry.getMessage(),
                entry.getBanner());
        }

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String secondsToString(long input)
    {
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
