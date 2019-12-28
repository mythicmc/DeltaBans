package com.gmail.tracebachi.DeltaBans.Bungee.Entries;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.google.common.base.Preconditions;

public class RangeBanEntry {
    private final String banner;
    private final String message;
    private final String startAddress;
    private final long startAddressLong;
    private final String endAddress;
    private final long endAddressLong;
    private final long createdAt;

    public RangeBanEntry(String banner, String message, String startAddress, String endAddress) {
        this(banner, message, startAddress, endAddress, System.currentTimeMillis());
    }

    public RangeBanEntry(String banner, String message, String startAddress, String endAddress, long createdAt) {
        Preconditions.checkNotNull(banner, "banner");
        Preconditions.checkNotNull(message, "message");
        Preconditions.checkNotNull(startAddress, "startAddress");
        Preconditions.checkNotNull(endAddress, "endAddress");
        Preconditions.checkArgument(DeltaBansUtils.isIp(startAddress), "Non-IP startAddress");
        Preconditions.checkArgument(DeltaBansUtils.isIp(endAddress), "Non-IP endAddress");
        this.banner = banner;
        this.message = message;
        this.startAddress = startAddress;
        this.startAddressLong = DeltaBansUtils.convertIpToLong(startAddress);
        this.endAddress = endAddress;
        this.endAddressLong = DeltaBansUtils.convertIpToLong(endAddress);
        this.createdAt = createdAt;
    }

    public String getBanner() {
        return this.banner;
    }

    public String getMessage() {
        return this.message;
    }

    public String getStartAddress() {
        return this.startAddress;
    }

    public long getStartAddressLong() {
        return this.startAddressLong;
    }

    public String getEndAddress() {
        return this.endAddress;
    }

    public long getEndAddressLong() {
        return this.endAddressLong;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }
}
