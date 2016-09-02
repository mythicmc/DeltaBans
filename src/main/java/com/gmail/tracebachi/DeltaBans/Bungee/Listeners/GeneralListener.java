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
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisMessageEvent;
import com.gmail.tracebachi.DeltaRedis.Shared.Registerable;
import com.gmail.tracebachi.DeltaRedis.Shared.Servers;
import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;
import com.gmail.tracebachi.DeltaRedis.Shared.SplitPatterns;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.List;

import static com.gmail.tracebachi.DeltaRedis.Shared.Prefixes.INFO;
import static com.gmail.tracebachi.DeltaRedis.Shared.Prefixes.input;

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
        plugin.debug("GeneralListener registered.");
    }

    @Override
    public void unregister()
    {
        plugin.getProxy().getPluginManager().unregisterListener(this);
        plugin.debug("GeneralListener unregistered.");
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
            event.setCancelReason(Settings.format("WhitelistMessage"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        DeltaRedisApi api = DeltaRedisApi.instance();
        String channel = event.getChannel();

        if(event.getChannel().equals(DeltaBansChannels.KICK))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String kicker = splitMessage[0];
            String nameToKick = splitMessage[1];
            String message = splitMessage[2];
            boolean isSilent = splitMessage[3].equals("1");
            ProxiedPlayer playerToKick = plugin.getProxy().getPlayer(nameToKick);

            if(playerToKick != null)
            {
                String kickMessage = Settings.format("KickMessageToPlayer",
                    kicker,
                    nameToKick,
                    message);
                BaseComponent[] textComponent = TextComponent.fromLegacyText(kickMessage);
                playerToKick.disconnect(textComponent);

                String announcement = Settings.format("KickMessageToAnnounce",
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
                    Settings.format("NotOnline", nameToKick));
            }
        }
        else if(channel.equals(DeltaBansChannels.BANNED))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 4);
            String sender = splitMessage[0];
            String argument = splitMessage[1];
            boolean isIp = splitMessage[2].equals("1");
            boolean hasExtra = splitMessage[3].equals("1");
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
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 2);
            String sender = splitMessage[0];
            boolean enable = splitMessage[1].equals("1");

            if(enable)
            {
                whitelistEnabled = true;
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    sender,
                    Settings.format("WhitelistToggle", "enabled"));
            }
            else
            {
                whitelistEnabled = false;
                api.sendMessageToPlayer(
                    event.getSendingServer(),
                    sender,
                    Settings.format("WhitelistToggle", "disabled"));
            }
        }
        else if(channel.equals(DeltaBansChannels.WHITELIST_EDIT))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 3);
            String sender = splitMessage[0];
            boolean isAdd = splitMessage[1].equals("1");
            String name = splitMessage[2];

            if(isAdd)
            {
                if(whitelistStorage.addToNormalWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("AddedToWhitelist", name, "normal"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("AlreadyInWhitelist", name, "normal"));
                }
            }
            else
            {
                if(whitelistStorage.removeFromNormalWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("RemovedFromWhitelist", name, "normal"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("NotInWhitelist", name, "normal"));
                }
            }
        }
        else if(channel.equals(DeltaBansChannels.RANGEBAN_WHITELIST_EDIT))
        {
            String[] splitMessage = SplitPatterns.DELTA.split(event.getMessage(), 3);
            String sender = splitMessage[0];
            boolean isAdd = splitMessage[1].equals("1");
            String name = splitMessage[2];

            if(isAdd)
            {
                if(whitelistStorage.addToRangeBanWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("AddedToWhitelist", name, "rangeban"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("AlreadyInWhitelist", name, "rangeban"));
                }
            }
            else
            {
                if(whitelistStorage.removeFromRangeBanWhitelist(name))
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("RemovedFromWhitelist", name, "rangeban"));
                }
                else
                {
                    api.sendMessageToPlayer(
                        event.getSendingServer(),
                        sender,
                        Settings.format("NotInWhitelist", name, "rangeban"));
                }
            }
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

    private String getBanInfoFor(BanEntry entry, boolean hasExtra)
    {
        if(entry == null)
        {
            return INFO + "No ban found.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(INFO).append("Ban for ");

        if(hasExtra)
        {
            builder
                .append(input(entry.getName()))
                .append(" , ")
                .append(input(entry.getIp()))
                .append("\n");
        }
        else
        {
            builder
                .append(input(entry.getName()))
                .append("\n");
        }

        builder
            .append("  Banner: ")
            .append(input(entry.getBanner()))
            .append("\n");
        builder
            .append("  Ban Message: ")
            .append(input(entry.getMessage()))
            .append("\n");
        builder
            .append("  Duration: ")
            .append(input(DeltaBansUtils.formatDuration(entry.getDuration())));

        return builder.toString();
    }

    private String getWarningInfoFor(String name)
    {
        List<WarningEntry> warnings = warningStorage.getWarnings(name);

        if(warnings == null || warnings.size() == 0)
        {
            return INFO + "No warnings found.";
        }

        StringBuilder builder = new StringBuilder(INFO + "Warnings for " + input(name));

        for(WarningEntry entry : warnings)
        {
            builder
                .append("\n")
                .append(" - ")
                .append(entry.getMessage())
                .append(" (")
                .append(entry.getWarner())
                .append(")");
        }

        return builder.toString();
    }
}
