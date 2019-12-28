package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;

public interface RangeBanStorage extends LoadAndSaveable {
    RangeBanEntry getIpRangeBan(String var1);

    RangeBanEntry getIpRangeBan(long var1);

    void add(RangeBanEntry var1);

    int removeIpRangeBan(String var1);

    int removeIpRangeBan(long var1);
}
