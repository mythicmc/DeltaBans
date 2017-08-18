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
package com.gmail.tracebachi.DeltaBans.Bungee.Entries;

import com.google.common.base.Preconditions;

/**
 * @author GeeItsZee (tracebachi@gmail.com)
 */
public class BanEntry
{
  private final String name;
  private final String ip;
  private final String banner;
  private final String message;
  private final Long duration;
  private final long createdAt;

  public BanEntry(String name, String ip, String banner, String message, Long duration)
  {
    this(name, ip, banner, message, duration, System.currentTimeMillis());
  }

  public BanEntry(
    String name, String ip, String banner, String message, Long duration, Long createdAt)
  {
    Preconditions.checkNotNull(banner, "banner");
    Preconditions.checkArgument(name != null || ip != null, "name and ip are null");

    if (duration != null && duration <= 0)
    {
      duration = null;
    }

    this.name = (name == null) ? null : name.toLowerCase();
    this.ip = ip;
    this.banner = banner;
    this.message = message;
    this.duration = duration;
    this.createdAt = (createdAt == null) ? System.currentTimeMillis() : createdAt;
  }

  public String getName()
  {
    return name;
  }

  public boolean hasName()
  {
    return name != null;
  }

  public String getIp()
  {
    return ip;
  }

  public boolean hasIp()
  {
    return ip != null;
  }

  public String getBanner()
  {
    return banner;
  }

  public String getMessage()
  {
    return message;
  }

  public Long getDuration()
  {
    return duration;
  }

  public boolean hasDuration()
  {
    return duration != null;
  }

  public boolean isDurationComplete()
  {
    if (duration != null)
    {
      return (duration < System.currentTimeMillis() - createdAt);
    }
    return false;
  }

  public long getCreatedAt()
  {
    return createdAt;
  }
}
