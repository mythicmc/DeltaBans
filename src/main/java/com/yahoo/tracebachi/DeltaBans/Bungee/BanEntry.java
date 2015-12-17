package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BanEntry
{
    private final String name;
    private final String ip;
    private final String banner;
    private final String message;
    private final Long duration;
    private final long createdAt;

    public BanEntry(String name, String ip, String banner, String message, Long duration, long createdAt)
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

        this.name = name;
        this.ip = ip;
        this.banner = banner;
        this.message = message;
        this.duration = duration;
        this.createdAt = createdAt;
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

        if((name == null && ip == null) || banner == null || createdAt == null)
        {
            throw new IllegalArgumentException("Ban object is not properly formatted.");
        }

        return new BanEntry(
            name == null ? null : name.getAsString(),
            ip == null ? null : ip.getAsString(),
            banner.getAsString(),
            message == null ? null : message.getAsString(),
            duration == null ? null : duration.getAsLong(),
            createdAt.getAsLong()
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
