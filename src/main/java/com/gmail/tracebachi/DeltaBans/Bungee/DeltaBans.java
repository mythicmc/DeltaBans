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
package com.gmail.tracebachi.DeltaBans.Bungee;

import com.gmail.tracebachi.DeltaBans.Bungee.Storage.*;
import com.gmail.tracebachi.DeltaRedis.Bungee.ConfigUtil;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedis;
import com.gmail.tracebachi.DeltaRedis.Bungee.DeltaRedisApi;
import com.google.gson.*;
import io.netty.util.internal.ConcurrentSet;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.*;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by Trace Bachi (tracebachi@gmail.com, BigBossZee) on 12/16/15.
 */
public class DeltaBans extends Plugin
{
    private Configuration config;
    private BanStorage banStorage;
    private BanListener banListener;
    private WarningStorage warningStorage;
    private WarningListener warningListener;
    private RangeBanStorage rangeBanStorage;
    private final Set<String> rangeBanWhitelist = new ConcurrentSet<>();
    private GeneralListener generalListener;
    private final Object saveLock = new Object();

    @Override
    public void onEnable()
    {
        reloadConfig();
        if(config == null) { return; }

        int minutesPerBanSave = config.getInt("MinutesPerBanSave");
        int minutesPerWarningCleanup = config.getInt("MinutesPerWarningCleanup");
        int warningDuration = config.getInt("WarningDuration");

        banStorage = new BanStorage();
        readBans();
        warningStorage = new WarningStorage();
        readWarnings();
        rangeBanStorage = new RangeBanStorage();
        readRangeBans();
        readRangeBanWhitelist();

        DeltaRedis deltaRedisPlugin = (DeltaRedis) getProxy()
            .getPluginManager().getPlugin("DeltaRedis");
        DeltaRedisApi deltaRedisApi = deltaRedisPlugin.getDeltaRedisApi();

        banListener = new BanListener(deltaRedisApi, this);
        banListener.register();

        warningListener = new WarningListener(deltaRedisApi, warningStorage, this);
        warningListener.register();

        generalListener = new GeneralListener(deltaRedisApi, this);
        generalListener.register();

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

        generalListener.shutdown();
        generalListener = null;

        warningListener.shutdown();
        warningListener = null;

        banListener.shutdown();
        banListener = null;

        if(banStorage != null && warningStorage != null && rangeBanStorage != null)
        {
            writeBansAndWarnings();
            banStorage = null;
            warningStorage = null;
            rangeBanStorage = null;
            rangeBanWhitelist.clear();
        }
        else
        {
            getLogger().severe("Failed to save ban files on shutdown! " +
                "One of the storages was not initialized correctly.");
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

    public Set<String> getRangeBanWhitelist()
    {
        return rangeBanWhitelist;
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
        File rangeBanWhitelistFile = new File(getDataFolder(), "rangeban-whitelist.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray banArray = banStorage.toJson();
        JsonArray rangeBanArray = rangeBanStorage.toJson();
        JsonArray warningArray = warningStorage.toJson();
        JsonArray rangeBanWhitelistArray = new JsonArray();

        synchronized(rangeBanWhitelist)
        {
            for(String name : rangeBanWhitelist)
            {
                rangeBanWhitelistArray.add(new JsonPrimitive(name));
            }
        }

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

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(rangeBanWhitelistFile)))
            {
                gson.toJson(rangeBanWhitelistArray, writer);
                getLogger().info("Done saving rangeban whitelist.");
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

    private void readRangeBanWhitelist()
    {
        File file = new File(getDataFolder(), "rangeban-whitelist.json");

        if(!file.exists()) { return; }

        JsonParser parser = new JsonParser();

        try(BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            JsonArray array = parser.parse(reader).getAsJsonArray();

            for(JsonElement element : array)
            {
                String name = element.getAsString();
                rangeBanWhitelist.add(name.toLowerCase());
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    private void reloadConfig()
    {
        try
        {
            File file = ConfigUtil.saveResource(this, "bungee-config.yml", "config.yml");
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);

            if(config == null)
            {
                ConfigUtil.saveResource(this, "bungee-config.yml", "config-example.yml", true);
                getLogger().severe("Invalid configuration file! An example configuration has been saved to the DeltaBans folder.");
            }
        }
        catch(IOException e)
        {
            getLogger().severe("Failed to load configuration file.");
            e.printStackTrace();
        }

    }
}
