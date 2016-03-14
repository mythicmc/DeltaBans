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

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.google.common.base.Preconditions;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class RangeBanEntry
{
    private final String banner;
    private final String message;
    private final String startAddress;
    private final long startAddressLong;
    private final String endAddress;
    private final long endAddressLong;
    private final long createdAt;

    public RangeBanEntry(String banner, String message, String startAddress, String endAddress)
    {
        this(banner, message, startAddress, endAddress, System.currentTimeMillis());
    }

    public RangeBanEntry(String banner, String message, String startAddress, String endAddress, long createdAt)
    {
        Preconditions.checkNotNull(banner, "Banner cannot be null");
        Preconditions.checkNotNull(message, "Message cannot be null");
        Preconditions.checkNotNull(startAddress, "Start address cannot be null");
        Preconditions.checkNotNull(endAddress, "End address cannot be null");
        Preconditions.checkArgument(DeltaBansUtils.isIp(startAddress), "Start address is not an IP.");
        Preconditions.checkArgument(DeltaBansUtils.isIp(endAddress), "End address is not an IP.");

        this.banner = banner;
        this.message = message;
        this.startAddress = startAddress;
        this.startAddressLong = DeltaBansUtils.convertIpToLong(startAddress);
        this.endAddress = endAddress;
        this.endAddressLong = DeltaBansUtils.convertIpToLong(endAddress);
        this.createdAt = createdAt;
    }

    public String getBanner()
    {
        return banner;
    }

    public String getMessage()
    {
        return message;
    }

    public String getStartAddress()
    {
        return startAddress;
    }

    public long getStartAddressLong()
    {
        return startAddressLong;
    }

    public String getEndAddress()
    {
        return endAddress;
    }

    public long getEndAddressLong()
    {
        return endAddressLong;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }

    public static RangeBanEntry fromJson(JsonObject object)
    {
        JsonElement banner = object.get("banner");
        JsonElement message = object.get("message");
        JsonElement startAddress = object.get("start_address");
        JsonElement endAddress = object.get("end_address");
        JsonElement createdAt = object.get("created_at");

        if(message == null || startAddress == null || endAddress == null)
        {
            throw new IllegalArgumentException("Warning is not properly formatted:\n" +
                object.toString());
        }

        return new RangeBanEntry(
            banner.getAsString(),
            message.getAsString(),
            startAddress.getAsString(),
            endAddress.getAsString(),
            (createdAt == null) ? System.currentTimeMillis() : createdAt.getAsLong());
    }

    public JsonObject toJson()
    {
        JsonObject object = new JsonObject();
        object.addProperty("banner", banner);
        object.addProperty("message", message);
        object.addProperty("start_address", startAddress);
        object.addProperty("end_address", endAddress);
        object.addProperty("created_at", createdAt);
        return object;
    }
}
