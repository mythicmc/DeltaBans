package com.yahoo.tracebachi.DeltaBans.Spigot;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 1/13/16.
 */
public class PendingWarn
{
    private final String sender;
    private final String message;

    public PendingWarn(String sender, String message)
    {
        this.sender = sender;
        this.message = message;
    }

    public String getSender()
    {
        return sender;
    }

    public String getMessage()
    {
        return message;
    }
}
