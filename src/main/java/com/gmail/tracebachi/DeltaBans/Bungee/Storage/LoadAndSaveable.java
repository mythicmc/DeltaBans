package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

public interface LoadAndSaveable {
    void load() throws Exception;

    void save() throws Exception;
}
