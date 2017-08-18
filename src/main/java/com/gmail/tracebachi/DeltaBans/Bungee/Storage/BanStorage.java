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
package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;

import java.util.List;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public interface BanStorage extends LoadAndSaveable
{
  BanEntry getBanEntry(String name, String ip);

  AddResult addBanEntry(BanEntry ban);

  List<BanEntry> removeUsingIp(String ip);

  List<BanEntry> removeUsingName(String name);

  enum AddResult
  {
    SUCCESS,
    EXISTING_NAME_BAN,
    EXISTING_IP_BAN,
    EXISTING_NAME_AND_IP_BAN
  }
}
