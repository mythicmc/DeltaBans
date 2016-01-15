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
import com.yahoo.tracebachi.DeltaBans.Bungee.Storage.*;
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
    private BanListener banListener;
    private WarningStorage warningStorage;
    private WarningListener warningListener;
    private RangeBanStorage rangeBanStorage;
    private GeneralListener generalListener;
    private final Object saveLock = new Object();

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
        int minutesPerBanSave = config.getInt("MinutesPerBanSave");
        int minutesPerWarningCleanup = config.getInt("MinutesPerWarningCleanup");
        int warningDuration = config.getInt("WarningDuration");

        banStorage = new BanStorage();
        readBans();
        warningStorage = new WarningStorage();
        readWarnings();
        rangeBanStorage = new RangeBanStorage();
        readRangeBans();

        DeltaRedisPlugin deltaRedisPlugin = (DeltaRedisPlugin) getProxy()
            .getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        banListener = new BanListener(deltaRedisApi, this);
        getProxy().getPluginManager().registerListener(this, banListener);
        warningListener = new WarningListener(deltaRedisApi, warningStorage);
        getProxy().getPluginManager().registerListener(this, warningListener);
        generalListener = new GeneralListener(deltaRedisApi, this);
        getProxy().getPluginManager().registerListener(this, generalListener);

        getProxy().getScheduler().schedule(this, this::writeBansAndWarnings,
            minutesPerBanSave, minutesPerBanSave, TimeUnit.MINUTES);
        getProxy().getScheduler().schedule(this, () ->
            warningStorage.cleanupWarnings(warningDuration),
            minutesPerWarningCleanup, minutesPerWarningCleanup, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable()
    {
        getProxy().getScheduler().cancel(this);

        if(generalListener != null)
        {
            getProxy().getPluginManager().unregisterListener(generalListener);
            generalListener.shutdown();
            generalListener = null;
        }

        if(warningListener != null)
        {
            getProxy().getPluginManager().unregisterListener(warningListener);
            warningListener.shutdown();
            warningListener = null;
        }

        if(banListener != null)
        {
            getProxy().getPluginManager().unregisterListener(banListener);
            banListener.shutdown();
            banListener = null;
        }

        if(banStorage != null && warningStorage != null)
        {
            writeBansAndWarnings();
            banStorage = null;
        }
    }

    public String getPermanentBanFormat()
    {
        return (config != null) ? config.getString("PermanentBan") : "BAD_CONFIG";
    }

    public String getTemporaryBanFormat()
    {
        return (config != null) ? config.getString("TemporaryBan") : "BAD_CONFIG";
    }

    public String getRangeBanFormat()
    {
        return (config != null) ? config.getString("RangeBan") : "BAD_CONFIG";
    }

    public BanStorage getBanStorage()
    {
        return banStorage;
    }

    public RangeBanStorage getRangeBanStorage()
    {
        return rangeBanStorage;
    }

    public WarningStorage getWarningStorage()
    {
        return warningStorage;
    }

    public boolean writeBansAndWarnings()
    {
        getLogger().info("Saving bans and warnings ...");

        File banFile = new File(getDataFolder(), "bans.json");
        File rangeBanFile = new File(getDataFolder(), "rangebans.json");
        File warningFile = new File(getDataFolder(), "warnings.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray banArray = banStorage.toJson();
        JsonArray rangeBanArray = rangeBanStorage.toJson();
        JsonArray warningArray = warningStorage.toJson();

        synchronized(saveLock)
        {
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(banFile)))
            {
                gson.toJson(banArray, writer);
                getLogger().info("Done saving bans.");
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(rangeBanFile)))
            {
                gson.toJson(rangeBanArray, writer);
                getLogger().info("Done saving range bans.");
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(warningFile)))
            {
                gson.toJson(warningArray, writer);
                getLogger().info("Done saving warnings.");
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }

            return true;
        }
    }

    private void readBans()
    {
        File file = new File(getDataFolder(), "bans.json");

        if(!file.exists()) { return; }

        JsonParser parser = new JsonParser();

        try(BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            JsonArray array = parser.parse(reader).getAsJsonArray();

            for(JsonElement element : array)
            {
                try
                {
                    BanEntry entry = BanEntry.fromJson(element.getAsJsonObject());
                    banStorage.add(entry);
                }
                catch(NullPointerException | IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readWarnings()
    {
        File file = new File(getDataFolder(), "warnings.json");

        if(!file.exists()) { return; }

        JsonParser parser = new JsonParser();

        try(BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            JsonArray array = parser.parse(reader).getAsJsonArray();

            for(JsonElement element : array)
            {
                try
                {
                    JsonObject object = element.getAsJsonObject();
                    String name = object.get("name").getAsString();

                    for(JsonElement warning : object.get("warnings").getAsJsonArray())
                    {
                        JsonObject warningObject = warning.getAsJsonObject();
                        warningStorage.add(name, WarningEntry.fromJson(warningObject));
                    }
                }
                catch(NullPointerException | IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void readRangeBans()
    {
        File file = new File(getDataFolder(), "rangebans.json");

        if(!file.exists()) { return; }

        JsonParser parser = new JsonParser();

        try(BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            JsonArray array = parser.parse(reader).getAsJsonArray();

            for(JsonElement element : array)
            {
                try
                {
                    RangeBanEntry entry = RangeBanEntry.fromJson(element.getAsJsonObject());
                    rangeBanStorage.add(entry);
                }
                catch(NullPointerException | IllegalArgumentException ex)
                {
                    ex.printStackTrace();
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
