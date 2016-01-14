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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class BanStorage
{
    private HashSet<BanEntry> banSet = new HashSet<>();
    private HashMap<String, BanEntry> nameMap = new HashMap<>();
    private HashMap<String, Set<BanEntry>> ipMap = new HashMap<>();

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
            Set<BanEntry> bansOnIp = ipMap.get(ban.getIp());

            if(bansOnIp == null)
            {
                bansOnIp = new HashSet<>();
                ipMap.put(ban.getIp(), bansOnIp);
            }

            bansOnIp.add(ban);
        }
    }

    public synchronized Set<BanEntry> removeUsingIp(String ip)
    {
        Preconditions.checkNotNull(ip, "IP cannot be null.");

        // Remove from the IP map
        Set<BanEntry> entriesToRemove = ipMap.remove(ip);

        if(entriesToRemove != null)
        {
            // Loop through every ban associated with the IP
            for(BanEntry entry : entriesToRemove)
            {
                // Remove from the name map if it has a name
                if(entry.hasName())
                {
                    nameMap.remove(entry.getName());
                }

                // Remove from the general ban set
                // Comparison is based on memory address, which is the desired behavior
                banSet.remove(entry);
            }
        }

        return entriesToRemove;
    }

    public synchronized BanEntry removeUsingName(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");

        // Remove from the name map
        name = name.toLowerCase();
        BanEntry entryToRemove = nameMap.remove(name);

        if(entryToRemove != null)
        {
            // Remove from the ip map if it has an IP
            if(entryToRemove.hasIp())
            {
                Set<BanEntry> bansOnIp = ipMap.getOrDefault(
                    entryToRemove.getIp(), Collections.emptySet());

                // Remove from the ip ban set
                // Comparison is based on memory address, which is the desired behavior
                bansOnIp.remove(entryToRemove);
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

    public synchronized Set<BanEntry> getIpBanEntries(String ip)
    {
        return ipMap.getOrDefault(ip, Collections.emptySet());
    }

    public synchronized boolean isNameBanned(String name)
    {
        return nameMap.containsKey(name.toLowerCase());
    }

    public synchronized BanEntry getNameBanEntry(String name)
    {
        return nameMap.get(name.toLowerCase());
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
