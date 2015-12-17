package com.yahoo.tracebachi.DeltaBans.Spigot;

import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.regex.Pattern;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/17/15.
 */
public class DeltaBanListener implements Listener
{
    private static final Pattern pattern = Pattern.compile("/\\\\");
    private static final String BAN_ANNOUNCE = "DB-Announce";

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(BAN_ANNOUNCE))
        {
            String[] splitMessage = pattern.split(event.getMessage(), 2);

            for(Player player : Bukkit.getOnlinePlayers())
            {
                player.sendMessage(Prefixes.INFO + splitMessage[1]);
            }
        }
    }
}
