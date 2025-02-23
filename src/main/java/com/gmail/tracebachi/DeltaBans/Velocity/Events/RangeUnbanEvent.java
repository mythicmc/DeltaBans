package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class RangeUnbanEvent {
    private final String staff;
    private final String ip;
    private final boolean isSilent;

    public RangeUnbanEvent(String staff, String ip, boolean isSilent) {
        this.staff = staff;
        this.ip = ip;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person un-banning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return the IP getting unbanned
     */
    public String getIP() {
        return ip;
    }

    /**
     * @return whether this is a silent announcement
     */
    public boolean isSilent() {
        return isSilent;
    }
}
