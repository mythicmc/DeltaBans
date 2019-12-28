package com.gmail.tracebachi.DeltaBans.Bungee.Storage;

public interface WhitelistStorage extends LoadAndSaveable {
    boolean isOnNormalWhitelist(String var1);

    boolean isOnRangeBanWhitelist(String var1);

    boolean addToNormalWhitelist(String var1);

    boolean addToRangeBanWhitelist(String var1);

    boolean removeFromNormalWhitelist(String var1);

    boolean removeFromRangeBanWhitelist(String var1);
}
