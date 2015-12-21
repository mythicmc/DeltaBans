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
package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BanStorage
{
    private HashSet<BanEntry> banSet = new HashSet<>();
    private HashMap<String, BanEntry> nameMap = new HashMap<>();
    private HashMap<String, BanEntry> ipMap = new HashMap<>();

    public synchronized void add(BanEntry ban)
    {
        Preconditions.checkNotNull(ban, "Ban cannot be null.");

        banSet.add(ban);

        if(ban.hasName())
        {
            nameMap.put(ban.getName(), ban);
        }

        if(ban.hasIp())
        {
            ipMap.put(ban.getIp(), ban);
        }
    }

    public synchronized BanEntry removeUsingIp(String ip)
    {
        Preconditions.checkNotNull(ip, "IP cannot be null.");

        // Remove from the IP map
        BanEntry entryToRemove = ipMap.remove(ip);

        if(entryToRemove != null)
        {
            // Remove from the name map if it has a name
            if(entryToRemove.hasName())
            {
                nameMap.remove(entryToRemove.getName());
            }

            // Remove from the general ban set
            // Comparison is based on memory address, which is the desired behavior
            banSet.remove(entryToRemove);
        }

        return entryToRemove;
    }

    public synchronized BanEntry removeUsingName(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");

        // Remove from the name map
        BanEntry entryToRemove = nameMap.remove(name);

        if(entryToRemove != null)
        {
            // Remove from the name map if it has an IP
            if(entryToRemove.hasIp())
            {
                ipMap.remove(entryToRemove.getIp());
            }

            // Remove from the general ban set
            // Comparison is based on memory address, which is the desired behavior
            banSet.remove(entryToRemove);
        }

        return entryToRemove;
    }

    public synchronized boolean isIpBanned(String ip)
    {
        return ipMap.containsKey(ip);
    }

    public synchronized BanEntry getIpBanEntry(String ip)
    {
        return ipMap.get(ip);
    }

    public synchronized boolean isNameBanned(String name)
    {
        return nameMap.containsKey(name);
    }

    public synchronized BanEntry getNameBanEntry(String name)
    {
        return nameMap.get(name);
    }

    public synchronized JsonArray toJson()
    {
        JsonArray array = new JsonArray();
        for(BanEntry entry : banSet)
        {
            array.add(entry.toJson());
        }
        return array;
    }
}
