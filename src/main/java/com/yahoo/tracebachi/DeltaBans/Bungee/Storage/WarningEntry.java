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
package com.yahoo.tracebachi.DeltaBans.Bungee.Storage;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class WarningEntry
{
    private final String message;
    private final long createdAt;

    public WarningEntry(String message)
    {
        this(message, System.currentTimeMillis());
    }

    public WarningEntry(String message, long createdAt)
    {
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getMessage()
    {
        return message;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public static WarningEntry fromJson(JsonObject object)
    {
        JsonElement message = object.get("message");
        JsonElement createdAt = object.get("created_at");

        if(message == null || createdAt == null)
        {
            throw new IllegalArgumentException("Warning is not properly formatted:\n" +
                object.toString());
        }

        return new WarningEntry(message.getAsString(), createdAt.getAsLong());
    }

    public JsonObject toJson()
    {
        JsonObject object = new JsonObject();
        object.addProperty("message", message);
        object.addProperty("created_at", createdAt);
        return object;
    }
}
