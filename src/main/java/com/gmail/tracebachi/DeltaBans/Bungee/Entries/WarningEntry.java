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

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public final class WarningEntry
{
    private final String name;
    private final String warner;
    private final String message;
    private final long createdAt;

    public WarningEntry(String name, String warner, String message)
    {
        this(name, warner, message, System.currentTimeMillis());
    }

    public WarningEntry(String name, String warner, String message, long createdAt)
    {
        this.name = Preconditions.checkNotNull(name, "name").toLowerCase();
        this.warner = Preconditions.checkNotNull(warner, "warner").toLowerCase();
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getName()
    {
        return name;
    }

    public String getWarner()
    {
        return warner;
    }

    public String getMessage()
    {
        return message;
    }

    public long getCreatedAt()
    {
        return createdAt;
    }
}
