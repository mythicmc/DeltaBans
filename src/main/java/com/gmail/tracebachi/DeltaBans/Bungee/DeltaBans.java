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

import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.BanListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.GeneralListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Listeners.WarningListener;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.BungeeLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.DefaultLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Loggers.DeltaBansLogger;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.*;
import com.gmail.tracebachi.DeltaRedis.Bungee.ConfigUtil;
import com.google.gson.*;
import io.github.kyzderp.bungeelogger.BungeeLoggerPlugin;
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
    private GeneralListener generalListener;
    private BanStorage banStorage;
    private BanListener banListener;
    private WarningStorage warningStorage;
    private WarningListener warningListener;
    private RangeBanStorage rangeBanStorage;
    private DeltaBansLogger logger;
    private final Set<String> rangeBanWhitelist = new ConcurrentSet<>();
    private final Set<String> whitelist = new ConcurrentSet<>();
    private final Object saveLock = new Object();

    @Override
    public void onEnable()
    {
        reloadConfig();
        if(config == null) return;
        Settings.read(config);

        banStorage = new BanStorage();
        readBans();
        warningStorage = new WarningStorage();
        readWarnings();
        rangeBanStorage = new RangeBanStorage();
        readRangeBans();
        readRangeBanWhitelist();
        readWhitelist();

        banListener = new BanListener(this);
        banListener.register();

        warningListener = new WarningListener(warningStorage, this);
        warningListener.register();

        generalListener = new GeneralListener(this);
        generalListener.register();

        getProxy().getScheduler().schedule(this,
            this::saveAll,
            Settings.getMinutesPerBanSave(),
            Settings.getMinutesPerBanSave(),
            TimeUnit.MINUTES);

        getProxy().getScheduler().schedule(this,
            () -> warningStorage.cleanupWarnings(Settings.getWarningDuration()),
            Settings.getMinutesPerWarningCleanup(),
            Settings.getMinutesPerWarningCleanup(),
            TimeUnit.MINUTES);

        Plugin foundPlugin = getProxy().getPluginManager().getPlugin("BungeeLogger");

        if(foundPlugin != null)
        {
            BungeeLoggerPlugin bungeeLoggerPlugin = (BungeeLoggerPlugin) foundPlugin;

            logger = new BungeeLogger(
                bungeeLoggerPlugin.createLogger(this));
        }
        else
        {
            logger = new DefaultLogger(getLogger());
        }
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
            saveAll();
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

    public Set<String> getWhitelist()
    {
        return whitelist;
    }

    public WarningStorage getWarningStorage()
    {
        return warningStorage;
    }

    public Configuration getConfig()
    {
        return config;
    }

    public void reloadConfig()
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

    public boolean saveAll()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        boolean result;

        getLogger().info("Saving bans ...");
        result = saveBans(gson);

        getLogger().info("Saving warnings ...");
        result &= saveWarnings(gson);

        getLogger().info("Saving whitelists ...");
        result &= saveWhitelists(gson);

        return result;
    }

    public void info(String message)
    {
        logger.info(message);
    }

    private boolean saveBans(Gson gson)
    {
        JsonArray banArray = banStorage.toJson();
        JsonArray rangeBanArray = rangeBanStorage.toJson();
        File banFile = new File(getDataFolder(), "bans.json");
        File rangeBanFile = new File(getDataFolder(), "rangebans.json");

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

            return true;
        }
    }

    private boolean saveWarnings(Gson gson)
    {
        JsonArray warningArray = warningStorage.toJson();
        File warningFile = new File(getDataFolder(), "warnings.json");

        synchronized(saveLock)
        {
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

    private boolean saveWhitelists(Gson gson)
    {
        JsonArray whitelistArray = new JsonArray();
        JsonArray rangeBanWhitelistArray = new JsonArray();
        File whitelistFile = new File(getDataFolder(), "whitelist.json");
        File rangeBanWhitelistFile = new File(getDataFolder(), "rangeban-whitelist.json");

        synchronized(whitelist)
        {
            for(String name : whitelist)
            {
                whitelistArray.add(new JsonPrimitive(name));
            }
        }

        synchronized(rangeBanWhitelist)
        {
            for(String name : rangeBanWhitelist)
            {
                rangeBanWhitelistArray.add(new JsonPrimitive(name));
            }
        }

        synchronized(saveLock)
        {
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(whitelistFile)))
            {
                gson.toJson(whitelistArray, writer);
                getLogger().info("Done saving whitelist.");
            }
            catch(IOException e)
            {
                e.printStackTrace();
                return false;
            }

            try(BufferedWriter writer = new BufferedWriter(new FileWriter(rangeBanWhitelistFile)))
            {
                gson.toJson(rangeBanWhitelistArray, writer);
                getLogger().info("Done saving range ban whitelist.");
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

            rangeBanWhitelist.clear();

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

    private void readWhitelist()
    {
        File file = new File(getDataFolder(), "whitelist.json");

        if(!file.exists()) { return; }

        JsonParser parser = new JsonParser();

        try(BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            JsonArray array = parser.parse(reader).getAsJsonArray();

            whitelist.clear();

            for(JsonElement element : array)
            {
                String name = element.getAsString();
                whitelist.add(name);
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
