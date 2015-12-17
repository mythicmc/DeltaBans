package com.yahoo.tracebachi.DeltaBans.Bungee;

import com.google.gson.*;
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
            File file = com.yahoo.tracebachi.DeltaRedis.Bungee.ConfigUtil.loadResource(
                this, "bungee-config.yml", "config.yml");
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
