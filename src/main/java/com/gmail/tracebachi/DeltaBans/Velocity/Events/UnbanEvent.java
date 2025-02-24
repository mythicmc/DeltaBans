package com.gmail.tracebachi.DeltaBans.Velocity.Events;

public class UnbanEvent {
    private final String staff;
    private final String nameOrIP;
    private final boolean isIP;
    private final boolean isSilent;

    public UnbanEvent(String staff, String nameOrIP, boolean isIP, boolean isSilent) {
        this.staff = staff;
        this.nameOrIP = nameOrIP;
        this.isIP = isIP;
        this.isSilent = isSilent;
    }

    /**
     * @return name of the person un-banning
     */
    public String getStaff() {
        return staff;
    }

    /**
     * @return name or Ip of the player getting un-banned. Check {@link UnbanEvent#isIP()}
     */
    public String getNameOrIP() {
        return nameOrIP;
    }

    /**
     * @return whether this is an IP-only unban
     */
    public boolean isIP() {
        return isIP;
    }

    /**
     * @return whether this is a silent announcement
     */
    public boolean isSilent() {
        return isSilent;
    }
}
