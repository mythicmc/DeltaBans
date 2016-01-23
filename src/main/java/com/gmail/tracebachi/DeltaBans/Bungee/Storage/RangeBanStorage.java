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
import com.google.gson.JsonArray;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class RangeBanStorage
{
    private List<RangeBanEntry> rangeBanList = new ArrayList<>();

    public synchronized void add(RangeBanEntry ban)
    {
        Preconditions.checkNotNull(ban, "Ban cannot be null.");
        rangeBanList.add(ban);
    }

    public synchronized RangeBanEntry getIpRangeBan(String ip)
    {
        long ipAsLong = DeltaBansUtils.convertIpToLong(ip);

        for(RangeBanEntry entry : rangeBanList)
        {
            if(ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
            {
                return entry;
            }
        }
        return null;
    }

    public synchronized int removeIpRangeBan(String ip)
    {
        int count = 0;
        long ipAsLong = DeltaBansUtils.convertIpToLong(ip);

        ListIterator<RangeBanEntry> iterator = rangeBanList.listIterator();
        while(iterator.hasNext())
        {
            RangeBanEntry entry = iterator.next();
            if(ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong())
            {
                iterator.remove();
                count++;
            }
        }
        return count;
    }

    public synchronized JsonArray toJson()
    {
        JsonArray array = new JsonArray();
        for(RangeBanEntry entry : rangeBanList)
        {
            array.add(entry.toJson());
        }
        return array;
    }
}
