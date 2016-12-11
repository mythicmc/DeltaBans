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
package com.gmail.tracebachi.DeltaBans.Bungee.Entries;

import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public final class BanEntry
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

    public BanEntry(String name, String ip, String banner, String message, Long duration, Long createdAt)
    {
        Preconditions.checkArgument(name != null || ip != null, "Name and IP were both null.");
        Preconditions.checkNotNull(banner, "Banner was null.");

        if(duration != null && duration <= 0)
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
        if(duration != null)
        {
            return (duration < System.currentTimeMillis() - createdAt);
        }
        return false;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    @Override
    public String toString()
    {
        return name + "," + ip + "," + banner + "," + message + "," + duration + "," + createdAt;
    }
}
