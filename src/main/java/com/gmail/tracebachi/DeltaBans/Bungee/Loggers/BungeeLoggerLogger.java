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
package com.gmail.tracebachi.DeltaBans.Bungee.Loggers;

import io.github.kyzderp.bungeelogger.BungeeLog;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/28/16.
 */
public class BungeeLoggerLogger implements DeltaBansLogger
{
    private BungeeLog logger;

    public BungeeLoggerLogger(BungeeLog logger)
    {
        this.logger = logger;
    }

    @Override
    public void info(String message)
    {
        logger.info(message);
    }

    @Override
    public void severe(String message)
    {
        logger.error(message);
    }

    @Override
    public void debug(String message)
    {
        logger.debug(message);
    }
}
