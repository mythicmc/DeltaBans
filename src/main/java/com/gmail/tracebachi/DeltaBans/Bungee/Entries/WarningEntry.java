package com.gmail.tracebachi.DeltaBans.Bungee.Entries;

import com.google.common.base.Preconditions;

public class WarningEntry {
    private final String name;
    private final String warner;
    private final String message;
    private final long createdAt;

    public WarningEntry(String name, String warner, String message) {
        this(name, warner, message, System.currentTimeMillis());
    }

    public WarningEntry(String name, String warner, String message, long createdAt) {
        this.name = ((String)Preconditions.checkNotNull(name, "name")).toLowerCase();
        this.warner = ((String)Preconditions.checkNotNull(warner, "warner")).toLowerCase();
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getName() {
        return this.name;
    }

    public String getWarner() {
        return this.warner;
    }

    public String getMessage() {
        return this.message;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }
}
