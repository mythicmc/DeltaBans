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

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaRedis.Shared.Interfaces.Shutdownable;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 8/27/16.
 */
public interface RangeBanStorage extends Loadable, Shutdownable
{
    void add(RangeBanEntry banEntry);

    RangeBanEntry getIpRangeBan(String ip);

    RangeBanEntry getIpRangeBan(long ipAsLong);

    int removeIpRangeBan(String ip);

    int removeIpRangeBan(long ipAsLong);

    int getTotalRangeBanCount();
}
