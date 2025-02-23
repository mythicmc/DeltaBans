package com.gmail.tracebachi.DeltaBans.Velocity.Events;

import org.jetbrains.annotations.Nullable;

public class BanEvent {
    private final String staff;
    private final String player;
    private final String reason;
    private final String ip;
    private final boolean isSilent;
    private final Long duration;

    public BanEvent(String staff, String player, String reason, String ip, boolean isSilent, Long duration) {
        this.staff = staff;
        this.player = player;
        this.reason = reason;
        this.ip = ip;
        this.isSilent = isSilent;
        this.duration = duration != null && duration <= 0 ? null : duration;
    }

    /**
     * @return name of the person banning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return name of the player getting banned. May be null/empty string in case of an IP-only ban
     */
    public @Nullable String getPlayer() {
        return player;
    }

    /**
     * @return the ban reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return the IP associated with this ban, null/empty string in case of name-only ban
     */
    public @Nullable String getIp() {
        return ip;
    }

    /**
     * @return whether this is a silent announcement
     */
    public boolean isSilent() {
        return isSilent;
    }

    /**
     * @return the ban duration in milliseconds, null if permanent ban
     */
    public @Nullable Long getDuration() {
        return duration;
    }
}
