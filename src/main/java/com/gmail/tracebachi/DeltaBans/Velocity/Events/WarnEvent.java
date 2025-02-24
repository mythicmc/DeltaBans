package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class WarnEvent {
    private final String staff;
    private final String player;
    private final String reason;
    private final boolean isSilent;

    public WarnEvent(String staff, String player, String reason, boolean isSilent) {
        this.staff = staff;
        this.player = player;
        this.reason = reason;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person warning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return name of the player getting warned
     */
    public String getPlayer() {
        return player;
    }

    /**
     * @return the ban reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * @return whether this is a silent announcement
     */
    public boolean isSilent() {
        return isSilent;
    }
}
