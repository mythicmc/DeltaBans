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
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Settings;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.DeltaBans.DeltaBansChannels;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.gmail.tracebachi.DeltaRedis.Bungee.Events.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
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
import static com.gmail.tracebachi.DeltaRedis.Shared.ChatMessageHelper.formatPlayerOffline;


/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class GeneralListener implements Listener, Registerable, Shutdownable
{
    private boolean whitelistEnabled;
    private BanStorage banStorage;
    private WarningStorage warningStorage;
    private WhitelistStorage whitelistStorage;
    private DeltaBans plugin;

    public GeneralListener(DeltaBans plugin)
    {
        this.plugin = plugin;
        this.banStorage = plugin.getBanStorage();
        this.warningStorage = plugin.getWarningStorage();
        this.whitelistStorage = plugin.getWhitelistStorage();
        this.whitelistEnabled = Settings.shouldStartWithWhitelistEnabled();
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
        banStorage = null;
        warningStorage = null;
        whitelistStorage = null;
        plugin = null;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(LoginEvent event)
    {
        PendingConnection pending = event.getConnection();
        String playerName = pending.getName();

        if(whitelistEnabled && !whitelistStorage.isOnNormalWhitelist(playerName))
        {
            event.setCancelReason(format("DeltaBans.WhitelistMessage"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        DeltaRedisApi api = DeltaRedisApi.instance();
        String channel = event.getChannel();
        List<String> messageParts = event.getMessageParts();

        if(event.getChannel().equals(DeltaBansChannels.KICK))
        {
            String kicker = messageParts.get(0);
            String nameToKick = messageParts.get(1);
            String message = messageParts.get(2);
            boolean isSilent = messageParts.get(3).equals("1");
            ProxiedPlayer playerToKick = plugin.getProxy().getPlayer(nameToKick);

            if(playerToKick != null)
            {
                String kickMessage = format(
                    "DeltaBans.KickMessageToPlayer",
                    kicker,
                    nameToKick,
                    message);
                BaseComponent[] textComponent = TextComponent.fromLegacyText(kickMessage);
                playerToKick.disconnect(textComponent);

                String announcement = format(
                    "DeltaBans.KickMessageToAnnounce",
                    kicker,
                    nameToKick,
                    message);
                announce(announcement, isSilent);
            }
            else
            {
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    kicker,
                    formatPlayerOffline(nameToKick));
            }
        }
        else if(channel.equals(DeltaBansChannels.BANNED))
        {
            String sender = messageParts.get(0);
            String argument = messageParts.get(1);
            boolean isIp = messageParts.get(2).equals("1");
            boolean hasExtra = messageParts.get(3).equals("1");
            StringBuilder builder = new StringBuilder();

            if(isIp)
            {
                BanEntry entry = banStorage.getBanEntry(null, argument);
                builder.append(getBanInfoFor(entry, hasExtra));
            }
            else
            {
                BanEntry entry = banStorage.getBanEntry(argument, null);
                String banInfoString = getBanInfoFor(entry, hasExtra);
                String warningInfoString = getWarningInfoFor(argument);
                builder.append(banInfoString).append("\n").append(warningInfoString);
            }

            api.sendMessageToPlayer(
                event.getSendingServer(),
                sender,
                builder.toString());
        }
        else if(channel.equals(DeltaBansChannels.WHITELIST_TOGGLE))
        {
            String sender = messageParts.get(0);
            boolean enable = messageParts.get(1).equals("1");

            if(enable)
            {
                whitelistEnabled = true;
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    sender,
                    format("DeltaBans.WhitelistToggle", "enabled"));
            }
            else
            {
                whitelistEnabled = false;
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    sender,
                    format("DeltaBans.WhitelistToggle", "disabled"));
            }
        }
        else if(channel.equals(DeltaBansChannels.WHITELIST_EDIT))
        {
            String sender = messageParts.get(0);
            boolean isAdd = messageParts.get(1).equals("1");
            String name = messageParts.get(2);

            if(isAdd)
            {
                if(whitelistStorage.addToNormalWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.AddedToWhitelist", name, "normal"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.AlreadyInWhitelist", name, "normal"));
                }
            }
            else
            {
                if(whitelistStorage.removeFromNormalWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.RemovedFromWhitelist", name, "normal"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.NotInWhitelist", name, "normal"));
                }
            }
        }
        else if(channel.equals(DeltaBansChannels.RANGEBAN_WHITELIST_EDIT))
        {
            String sender = messageParts.get(0);
            boolean isAdd = messageParts.get(1).equals("1");
            String name = messageParts.get(2);

            if(isAdd)
            {
                if(whitelistStorage.addToRangeBanWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.AddedToWhitelist", name, "rangeban"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.AlreadyInWhitelist", name, "rangeban"));
                }
            }
            else
            {
                if(whitelistStorage.removeFromRangeBanWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.RemovedFromWhitelist", name, "rangeban"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        format("DeltaBans.NotInWhitelist", name, "rangeban"));
                }
            }
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

    private String getBanInfoFor(BanEntry entry, boolean hasExtra)
    {
        if(entry == null)
        {
            return format("DeltaBans.NotFound");
        }

        StringBuilder builder = new StringBuilder();

        if(hasExtra)
        {
            builder.append(format(
                "DeltaBans.BanInfo.LineFormat",
                "IP", entry.getIp()));
            builder.append("\n");
        }

        builder.append(format(
            "DeltaBans.BanInfo.LineFormat",
            "Name", entry.getName()));
        builder.append("\n");
        builder.append(format(
            "DeltaBans.BanInfo.LineFormat",
            "Banner", entry.getBanner()));
        builder.append("\n");
        builder.append(format(
            "DeltaBans.BanInfo.LineFormat",
            "Ban Message", entry.getMessage()));
        builder.append("\n");
        builder.append(format(
            "DeltaBans.BanInfo.LineFormat",
            "Duration", DeltaBansUtils.formatDuration(entry.getDuration())));

        return builder.toString();
    }

    private String getWarningInfoFor(String name)
    {
        List<WarningEntry> warnings = warningStorage.getWarnings(name);

        if(warnings == null || warnings.size() == 0)
        {
            return format("DeltaBans.WarningInfo.NotFound");
        }

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < warnings.size(); i++)
        {
            WarningEntry entry = warnings.get(i);
            builder.append(format(
                "DeltaBans.WarningInfo.LineFormat",
                entry.getWarner(),
                entry.getMessage()));

            if(i != warnings.size() - 1)
            {
                builder.append("\n");
            }
        }

        return builder.toString();
    }
}
