package com.gmail.tracebachi.DeltaBans.Bungee.Loggers;

import io.github.kyzderp.bungeelogger.BungeeLog;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/28/16.
 */
public class BungeeLogger implements DeltaBansLogger
{
    private BungeeLog logger;

    public BungeeLogger(BungeeLog logger)
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
