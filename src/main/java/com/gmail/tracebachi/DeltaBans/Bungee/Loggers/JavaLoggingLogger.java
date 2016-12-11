package com.gmail.tracebachi.DeltaBans.Bungee.Loggers;

import java.util.logging.Logger;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 7/28/16.
 */
public class JavaLoggingLogger implements DeltaBansLogger
{
    private Logger logger;

    public JavaLoggingLogger(Logger logger)
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
        logger.severe(message);
    }

    @Override
    public void debug(String message)
    {
        logger.info("[Debug] " + message);
    }
}
