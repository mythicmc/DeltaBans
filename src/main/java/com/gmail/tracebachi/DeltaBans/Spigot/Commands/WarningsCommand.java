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
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class WarningsCommand implements CommandExecutor, Registerable
{
  private static final String COMMAND_NAME = "warnings";
  private static final String COMMAND_USAGE = "/warnings <name>";
  private static final String COMMAND_PERM = "DeltaBans.CheckWarnings";

  private final DeltaBansPlugin plugin;
  private final SockExchangeApi api;
  private final MessageFormatMap formatMap;

  public WarningsCommand(DeltaBansPlugin plugin, SockExchangeApi api, MessageFormatMap formatMap)
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
  }

  @Override
  public void unregister()
  {
    plugin.getCommand(COMMAND_NAME).setExecutor(null);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String s, String[] args)
  {
    boolean isSilent = DeltaBansUtils.isSilent(args);
    if (isSilent)
    {
      args = DeltaBansUtils.filterSilent(args);
    }

    if (args.length < 1)
    {
      sender.sendMessage(formatMap.format(Formats.USAGE, COMMAND_USAGE));
      return true;
    }

    if (!sender.hasPermission(COMMAND_PERM))
    {
      sender.sendMessage(formatMap.format(Formats.NO_PERM, COMMAND_PERM));
      return true;
    }

    String nameToCheck = args[0];
    ByteArrayDataOutput out = ByteStreams.newDataOutput(256);
    out.writeUTF(api.getServerName());
    out.writeUTF(sender.getName());
    out.writeUTF(nameToCheck);

    api.sendToBungee(Channels.CHECK_WARNINGS, out.toByteArray());
    return true;
  }
}
