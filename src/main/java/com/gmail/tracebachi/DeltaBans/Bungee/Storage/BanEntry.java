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
package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
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

    public BanEntry(String name, String ip, String banner, String message, Long duration, Long createdAt)
    {
        if(name == null && ip == null)
        {
            throw new IllegalArgumentException("Name and IP cannot both be null.");
        }

        if(banner == null)
        {
            throw new IllegalArgumentException("Every ban must have a banner.");
        }

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
            return (createdAt + duration - System.currentTimeMillis()) < 0;
        }
        return false;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public static BanEntry fromJson(JsonObject object)
    {
        JsonElement name = object.get("name");
        JsonElement ip = object.get("ip");
        JsonElement banner = object.get("banner");
        JsonElement message = object.get("message");
        JsonElement duration = object.get("duration");
        JsonElement createdAt = object.get("created_at");

        if((name == null && ip == null) || banner == null)
        {
            throw new IllegalArgumentException("Ban is not properly formatted:\n" +
                object.toString());
        }

        return new BanEntry(
            name == null ? null : name.getAsString(),
            ip == null ? null : ip.getAsString(),
            banner.getAsString(),
            message == null ? "Unspecified Reason" : message.getAsString(),
            duration == null ? null : duration.getAsLong(),
            createdAt == null ? null : createdAt.getAsLong()
        );
    }

    public JsonObject toJson()
    {
        JsonObject object = new JsonObject();
        object.addProperty("name", name);
        object.addProperty("ip", ip);
        object.addProperty("banner", banner);
        object.addProperty("message", message);
        object.addProperty("duration", duration);
        object.addProperty("created_at", createdAt);
        return object;
    }
}
