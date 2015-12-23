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
package com.yahoo.tracebachi.DeltaBans.Spigot;

import com.yahoo.tracebachi.DeltaRedis.Spigot.DeltaRedisMessageEvent;
import com.yahoo.tracebachi.DeltaRedis.Spigot.Prefixes;
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
    private static final String ANNOUNCE = "DB-Announce";

    @EventHandler
    public void onRedisMessage(DeltaRedisMessageEvent event)
    {
        if(event.getChannel().equals(ANNOUNCE))
        {
            for(Player player : Bukkit.getOnlinePlayers())
            {
                player.sendMessage(Prefixes.INFO + event.getMessage());
            }
        }
    }
}
