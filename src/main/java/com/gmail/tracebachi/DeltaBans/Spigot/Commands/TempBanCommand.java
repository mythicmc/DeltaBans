/*
 * DeltaBans - Ban and warning plugin for BungeeCord and Spigot servers
 * Copyright (C) 2017 tracebachi@gmail.com (GeeItsZee)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmail.tracebachi.DeltaBans.Spigot.Commands;

import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Channels;
import com.gmail.tracebachi.DeltaBans.DeltaBansConstants.Formats;
import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Spigot.DeltaBansPlugin;
import com.gmail.tracebachi.SockExchange.Spigot.SockExchangeApi;
import com.gmail.tracebachi.SockExchange.Utilities.MessageFormatMap;
import com.gmail.tracebachi.SockExchange.Utilities.Registerable;
import com.google.common.base.Preconditions;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class TempBanCommand implements TabExecutor, Registerable
{
  private static final String COMMAND_NAME = "tempban";
  private static final String COMMAND_USAGE = "/tempban <name|ip> <duration> [message]";
  private static final String COMMAND_PERM = "DeltaBans.Ban";
  private static final String NAME_ONLY_FLAG = "-name";

  private final DeltaBansPlugin plugin;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;

  public TempBanCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap)
  {
    Preconditions.checkNotNull(plugin, "plugin");
    Preconditions.checkNotNull(api, "api");
    Preconditions.checkNotNull(formatMap, "formatMap");

    this.plugin = plugin;
    this.api = api;
    this.formatMap = formatMap;
  }

  @Override
  public void register()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(this);
    plugin.getCommand(COMMAND_NAME).setTabCompleter(this);
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);
    plugin.getCommand(COMMAND_NAME).setTabCompleter(null);
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args)
  {
    return TabCompleteNameHelper.getNamesThatStartsWith(args[args.length - 1], api);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    boolean isSilent = DeltaBansUtils.isSilent(args);
    if (isSilent)
    {
      args = DeltaBansUtils.filterSilent(args);
    }

    boolean isNameOnly = DeltaBansUtils.hasFlag(args, NAME_ONLY_FLAG);
    if (isNameOnly)
    {
      args = DeltaBansUtils.filterFlag(args, NAME_ONLY_FLAG);
    }

    if (args.length < 2)
    {
      sender.sendMessage(formatMap.format(Formats.USAGE, COMMAND_USAGE));
      return true;
    }

    if (!sender.hasPermission(COMMAND_PERM))
    {
      sender.sendMessage(formatMap.format(Formats.NO_PERM, COMMAND_PERM));
      return true;
    }

    String banner = sender.getName();
    String nameOrIp = args[0];

    if (banner.equalsIgnoreCase(nameOrIp))
    {
      sender.sendMessage(formatMap.format(Formats.NOT_ALLOWED_ON_SELF, COMMAND_NAME));
      return true;
    }

    String name = "";
    String ip = nameOrIp;

    // If a name-only ban is requested, assume the input to be the name and clear the IP.
    if (isNameOnly)
    {
      name = nameOrIp;
      ip = "";
    }
    // If the nameOrIp is not an IP, assume the input is a name and try to look up the IP.
    else if (!DeltaBansUtils.isIp(nameOrIp))
    {
      name = nameOrIp;
      ip = plugin.getIpOfPlayer(name);

      // Tell the sender if the IP could not be found
      if (ip.equals(""))
      {
        sender.sendMessage(formatMap.format(Formats.NO_IP_FOUND, name));
      }
    }

    Long durationInSeconds = getDurationInSeconds(args[1]);

    if (durationInSeconds <= 0)
    {
      sender.sendMessage(formatMap.format(Formats.INVALID_DURATION, args[1]));
      return true;
    }

    String message = "";

    // Build the ban message from the rest of the arguments
    if (args.length > 2)
    {
      message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
      message = ChatColor.translateAlternateColorCodes('&', message);
    }

    ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
    out.writeUTF(api.getServerName());
    out.writeUTF(name);
    out.writeUTF(ip);
    out.writeUTF(banner);
    out.writeUTF(message);
    out.writeBoolean(true); // Duration = Temporary
    out.writeLong(durationInSeconds * 1000);
    out.writeBoolean(isSilent);

    api.sendToBungee(Channels.BAN, out.toByteArray());
    return true;
  }

  private long getDurationInSeconds(String input)
  {
    if (input == null || input.isEmpty())
    {
      return 0;
    }

    int multiplier = 1;
    boolean hasEndingChar = true;

    switch (input.charAt(input.length() - 1))
    {
      case 's':
        multiplier = 1;
        break;
      case 'm':
        multiplier = 60;
        break;
      case 'h':
        multiplier = 60 * 60;
        break;
      case 'd':
        multiplier = 60 * 60 * 24;
        break;
      case 'w':
        multiplier = 60 * 60 * 24 * 7;
        break;
      default:
        hasEndingChar = false;
        break;
    }

    if (hasEndingChar)
    {
      input = input.substring(0, input.length() - 1);
    }

    try
    {
      int value = Integer.parseInt(input) * multiplier;
      return (value > 0) ? value : 0;
    }
    catch (NumberFormatException ex)
    {
      return 0;
    }
  }
}
