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

import com.gmail.tracebachi.DeltaRedis.Shared.Shutdownable;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 8/27/16.
 */
public interface WhitelistStorage extends Loadable, Shutdownable
{
    int getWhitelistSize();

    boolean isOnNormalWhitelist(String name);

    boolean removeFromNormalWhitelist(String name);

    boolean addToNormalWhitelist(String name);

    boolean isOnRangeBanWhitelist(String name);

    boolean removeFromRangeBanWhitelist(String name);

    boolean addToRangeBanWhitelist(String name);
}
