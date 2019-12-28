package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import java.util.List;

public interface BanStorage extends LoadAndSaveable {
    BanEntry getBanEntry(String var1, String var2);

    BanStorage.AddResult addBanEntry(BanEntry var1);

    List<BanEntry> removeUsingIp(String var1);

    List<BanEntry> removeUsingName(String var1);

    public static enum AddResult {
        SUCCESS,
        EXISTING_NAME_BAN,
        EXISTING_IP_BAN,
        EXISTING_NAME_AND_IP_BAN;

        private AddResult() {
        }
    }
}
