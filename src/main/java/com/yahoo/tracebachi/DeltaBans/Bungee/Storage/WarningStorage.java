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

import com.google.common.base.Preconditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class WarningStorage
{
    private HashMap<String, List<WarningEntry>> warningsMap = new HashMap<>();

    public synchronized List<WarningEntry> getWarnings(String name)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        name = name.toLowerCase();

        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings == null)
        {
            return Collections.emptyList();
        }
        else if(warnings.size() == 0)
        {
            warningsMap.remove(name);
            return Collections.emptyList();
        }
        else
        {
            return Collections.unmodifiableList(warnings);
        }
    }

    public synchronized int add(String name, WarningEntry entry)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        Preconditions.checkNotNull(entry, "Message cannot be null.");
        name = name.toLowerCase();

        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings == null)
        {
            warnings = new ArrayList<>();
            warningsMap.put(name, warnings);
        }
        warnings.add(entry);

        return warnings.size();
    }

    public synchronized int remove(String name, int amount)
    {
        Preconditions.checkNotNull(name, "Name cannot be null.");
        name = name.toLowerCase();

        int count = 0;
        List<WarningEntry> warnings = warningsMap.get(name);

        if(warnings != null)
        {
            for(int i = amount; i > 0 && warnings.size() >= 0; i--)
            {
                if(warnings.size() <= 1)
                {
                    warnings.clear();
                    warningsMap.remove(name);
                    i = 0;
                }
                else
                {
                    warnings.remove(warnings.size() - 1);
                }
                count++;
            }
        }
        return count;
    }

    public void cleanupWarnings(long warningDuration)
    {
        Iterator<Map.Entry<String, List<WarningEntry>>> iterator = warningsMap.entrySet().iterator();
        long oldestTime = System.currentTimeMillis() - warningDuration;

        while(iterator.hasNext())
        {
            Map.Entry<String, List<WarningEntry>> entry = iterator.next();
            ListIterator<WarningEntry> listIterator = entry.getValue().listIterator();

            while(listIterator.hasNext())
            {
                WarningEntry warningEntry = listIterator.next();
                if(warningEntry.getCreatedAt() < oldestTime)
                {
                    listIterator.remove();
                }
            }

            if(entry.getValue().size() == 0)
            {
                iterator.remove();
            }
        }
    }

    public synchronized JsonArray toJson()
    {
        JsonArray array = new JsonArray();
        for(Map.Entry<String, List<WarningEntry>> entry : warningsMap.entrySet())
        {
            JsonObject object = new JsonObject();
            JsonArray warnings = new JsonArray();

            for(WarningEntry warningEntry : entry.getValue())
            {
                warnings.add(warningEntry.toJson());
            }

            object.addProperty("name", entry.getKey());
            object.add("warnings", warnings);
        }
        return array;
    }
}
