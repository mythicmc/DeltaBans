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
package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.gson.*;
import com.yahoo.tracebachi.DeltaRedis.Bungee.ConfigUtil;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.yahoo.tracebachi.DeltaRedis.Bungee.DeltaRedisPlugin;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by Trace Bachi (tracebachi@yahoo.com, BigBossZee) on 12/16/15.
 */
public class DeltaBansPlugin extends Plugin
{
    private Configuration config;
    private BanStorage banStorage;
    private DeltaBanListener banListener;

    @Override
    public void onLoad()
    {
        try
        {
            File file = ConfigUtil.loadResource(this, "bungee-config.yml", "config.yml");
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
        }
        catch(IOException e)
        {
            getLogger().severe("Failed to load configuration file. " +
                "Report this error and the following stacktrace.");
            e.printStackTrace();
        }
    }

    @Override
    public void onEnable()
    {
        String permanentBanFormat = config.getString("PermanentBan");
        String temporaryBanFormat = config.getString("TemporaryBan");
        int minutesPerBanSave = config.getInt("MinutesPerBanSave");

        banStorage = new BanStorage();
        readBans();

        DeltaRedisPlugin deltaRedisPlugin = (DeltaRedisPlugin) getProxy().getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        banListener = new DeltaBanListener(permanentBanFormat, temporaryBanFormat,
            deltaRedisApi, banStorage);
        getProxy().getPluginManager().registerListener(this, banListener);

        getProxy().getScheduler().schedule(this, this::writeBans,
            minutesPerBanSave, minutesPerBanSave, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable()
    {
        getProxy().getScheduler().cancel(this);

        if(banListener != null)
        {
            getProxy().getPluginManager().unregisterListener(banListener);
            banListener.shutdown();
            banListener = null;
        }

        if(banStorage != null)
        {
            writeBans();
            banStorage = null;
        }
    }

    private void readBans()
    {
        File file = new File(getDataFolder(), "bans.json");

        if(file.exists())
        {
            JsonParser parser = new JsonParser();

            try(BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
                JsonArray array = parser.parse(reader).getAsJsonArray();

                for(JsonElement element : array)
                {
                    BanEntry entry = BanEntry.fromJson(element.getAsJsonObject());
                    banStorage.add(entry);
                }
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void writeBans()
    {
        getLogger().info("Saving bans ...");

        File file = new File(getDataFolder(), "bans.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray array = banStorage.toJson();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
        {
            gson.toJson(array, writer);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
