package com.gmail.tracebachi.DeltaBans.Bungee.Entries;

import com.google.common.base.Preconditions;

public class BanEntry {
    private final String name;
    private final String ip;
    private final String banner;
    private final String message;
    private final Long duration;
    private final long createdAt;

    public BanEntry(String name, String ip, String banner, String message, Long duration) {
        this(name, ip, banner, message, duration, System.currentTimeMillis());
    }

    public BanEntry(String name, String ip, String banner, String message, Long duration, Long createdAt) {
        Preconditions.checkNotNull(banner, "banner");
        Preconditions.checkArgument(name != null || ip != null, "name and ip are null");
        if (duration != null && duration <= 0L) {
            duration = null;
        }

        this.name = name == null ? null : name.toLowerCase();
        this.ip = ip;
        this.banner = banner;
        this.message = message;
        this.duration = duration;
        this.createdAt = createdAt == null ? System.currentTimeMillis() : createdAt;
    }

    public String getName() {
        return this.name;
    }

    public boolean hasName() {
        return this.name != null;
    }

    public String getIp() {
        return this.ip;
    }

    public boolean hasIp() {
        return this.ip != null;
    }

    public String getBanner() {
        return this.banner;
    }

    public String getMessage() {
        return this.message;
    }

    public Long getDuration() {
        return this.duration;
    }

    public boolean hasDuration() {
        return this.duration != null;
    }

    public boolean isDurationComplete() {
        if (this.duration != null) {
            return this.duration < System.currentTimeMillis() - this.createdAt;
        } else {
            return false;
        }
    }

    public long getCreatedAt() {
        return this.createdAt;
    }
}
