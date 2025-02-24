package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class KickEvent {
    private final String staff;
    private final String player;
    private final String reason;
    private final boolean isSilent;

    public KickEvent(String staff, String player, String reason, boolean isSilent) {
        this.staff = staff;
        this.player = player;
        this.reason = reason;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person kicking
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return name of the player getting kicked
     */
    public String getPlayer() {
        return player;
    }

    /**
     * @return the kick reason
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
