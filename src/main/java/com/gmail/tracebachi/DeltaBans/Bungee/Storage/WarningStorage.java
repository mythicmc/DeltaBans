package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import java.util.List;

public interface WarningStorage extends LoadAndSaveable {
    List<WarningEntry> getWarnings(String var1);

    int addWarning(WarningEntry var1);

    int removeWarnings(String var1, int var2);

    void removeExpiredWarnings();
}
