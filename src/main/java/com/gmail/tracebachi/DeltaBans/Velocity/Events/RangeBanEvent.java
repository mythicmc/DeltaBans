package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class RangeBanEvent {
    private final String staff;
    private final String startIP;
    private final String endIP;
    private final String reason;
    private final boolean isSilent;

    public RangeBanEvent(String staff, String startIP, String endIP, String reason, boolean isSilent) {
        this.staff = staff;
        this.startIP = startIP;
        this.endIP = endIP;
        this.reason = reason;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person banning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return starting IP of this range
     */
    public String getStartIP() {
        return startIP;
    }

    /**
     * @return end IP of this range
     */
    public String getEndIP() {
        return endIP;
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
