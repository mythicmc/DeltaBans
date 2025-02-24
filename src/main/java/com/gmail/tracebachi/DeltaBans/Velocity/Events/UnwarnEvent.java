package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class UnwarnEvent {
    private final String staff;
    private final String player;
    private final int unwarnCount;
    private final boolean isSilent;

    public UnwarnEvent(String staff, String player, int unwarnCount, boolean isSilent) {
        this.staff = staff;
        this.player = player;
        this.unwarnCount = unwarnCount;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person un-warning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return name of the player getting un-warned
     */
    public String getPlayer() {
        return player;
    }

    /**
     * @return the number of warnings getting removed
     */
    public int getUnwarnCount() {
        return unwarnCount;
    }

    /**
     * @return whether this is a silent announcement
     */
    public boolean isSilent() {
        return isSilent;
    }
}
