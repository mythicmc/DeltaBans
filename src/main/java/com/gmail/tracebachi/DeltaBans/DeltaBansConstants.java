package com.gmail.tracebachi.DeltaBans;

public class DeltaBansConstants {
    public DeltaBansConstants() {
    }

    public static class MySqlQueries {
        public static final String CREATE_BAN_TABLE = " CREATE TABLE IF NOT EXISTS deltabans_bans ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `name`      VARCHAR(32), `ip`        VARCHAR(40), `banner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `duration`  BIGINT, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`name`), INDEX (`ip`));";
        public static final String CREATE_WARNING_TABLE = " CREATE TABLE IF NOT EXISTS deltabans_warnings ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `name`      VARCHAR(32) NOT NULL, `warner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`name`));";
        public static final String CREATE_RANGE_BAN_TABLE = " CREATE TABLE IF NOT EXISTS deltabans_rangebans ( `id`        INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY, `ip_start`  VARCHAR(40) NOT NULL, `ip_end`    VARCHAR(40) NOT NULL, `banner`    VARCHAR(32) NOT NULL, `message`   VARCHAR(255) NOT NULL, `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX (`ip_start`, `ip_end`));";
        public static final String CREATE_WHITELIST_TABLE = " CREATE TABLE IF NOT EXISTS deltabans_whitelist ( `name` VARCHAR(32) NOT NULL PRIMARY KEY, `type` INT UNSIGNED NOT NULL DEFAULT 0);";
        public static final String SELECT_ALL_BANS_STATEMENT = "SELECT * FROM deltabans_bans;";
        public static final String ADD_BAN_ENTRY_STATEMENT = " INSERT INTO deltabans_bans (name, ip, banner, message, duration, createdAt) VALUES (?,?,?,?,?,?);";
        public static final String DELETE_BAN_ENTRY_NO_NULL_STATEMENT = " DELETE FROM deltabans_bans WHERE name = ? AND ip = ? AND banner = ? AND message = ? LIMIT 1;";
        public static final String DELETE_BAN_ENTRY_NAME_NULL_STATEMENT = " DELETE FROM deltabans_bans WHERE name is NULL AND ip = ? AND banner = ? AND message = ? LIMIT 1;";
        public static final String DELETE_BAN_ENTRY_IP_NULL_STATEMENT = " DELETE FROM deltabans_bans WHERE name = ? AND ip is NULL AND banner = ? AND message = ? LIMIT 1;";
        public static final String SELECT_ALL_RANGEBANS_STATEMENT = "SELECT * FROM deltabans_rangebans;";
        public static final String ADD_RANGEBAN_ENTRY_STATEMENT = " INSERT INTO deltabans_rangebans (ip_start, ip_end, banner, message, createdAt) VALUES (?,?,?,?,?);";
        public static final String DELETE_RANGEBAN_ENTRY_STATEMENT = " DELETE FROM deltabans_rangebans WHERE ip_start = ? AND ip_end = ? AND banner = ? AND message = ? LIMIT 1;";
        public static final String SELECT_ALL_WARNINGS_STATEMENT = "SELECT * FROM deltabans_warnings;";
        public static final String ADD_WARNING_STATEMENT = " INSERT INTO deltabans_warnings (name, warner, message, createdAt) VALUES (?,?,?,?);";
        public static final String DELETE_WARNING_STATEMENT = " DELETE FROM deltabans_warnings WHERE name = ? AND warner = ? AND message = ? LIMIT 1;";
        public static final String SELECT_ALL_WHITELIST_STATEMENT = "SELECT * FROM deltabans_whitelist;";
        public static final String ADD_TO_WHITELIST_STATEMENT = " INSERT INTO deltabans_whitelist (name, type) VALUES (?,?);";
        public static final String DELETE_FROM_WHITELIST_STATEMENT = " DELETE FROM deltabans_whitelist WHERE name = ?";
        public static final String UPDATE_IN_WHITELIST_STATEMENT = " UPDATE deltabans_whitelist SET type = ? WHERE name = ? LIMIT 1;";

        public MySqlQueries() {
        }
    }

    public static class Formats {
        public static final String DEFAULT_MESSAGE_BAN = "DefaultMessage/Ban";
        public static final String DEFAULT_MESSAGE_RANGEBAN = "DefaultMessage/RangeBan";
        public static final String DEFAULT_MESSAGE_KICK = "DefaultMessage/Kick";
        public static final String DEFAULT_MESSAGE_WARN = "DefaultMessage/Warn";
        public static final String SERVER_IN_WHITELIST_MODE = "ServerInWhitelistMode";
        public static final String BAN_NOT_FOUND = "BanNotFound";
        public static final String WARNINGS_NOT_FOUND = "WarningsNotFound";
        public static final String BAN_ALREADY_EXISTS = "BanAlreadyExists";
        public static final String ANNOUNCE_BAN = "Announce/Ban";
        public static final String ANNOUNCE_UNBAN = "Announce/Unban";
        public static final String ANNOUNCE_RANGEBAN = "Announce/RangeBan";
        public static final String ANNOUNCE_RANGEUNBAN = "Announce/RangeUnban";
        public static final String ANNOUNCE_KICK = "Announce/Kick";
        public static final String ANNOUNCE_WARN = "Announce/Warn";
        public static final String ANNOUNCE_UNWARN = "Announce/Unwarn";
        public static final String TEMP_BAN_MESSAGE = "TemporaryBanMessage";
        public static final String PERMANENT_BAN_MESSAGE = "PermanentBanMessage";
        public static final String RANGE_BAN_MESSAGE = "RangeBanMessage";
        public static final String WHITELIST_TOGGLED = "WhitelistToggled";
        public static final String ADDED_TO_WHITELIST = "AddedToWhitelist";
        public static final String REMOVED_FROM_WHITELIST = "RemovedFromWhitelist";
        public static final String PLAYER_OFFLINE = "PlayerOffline";
        public static final String KICK_MESSAGE = "KickMessage";
        public static final String BAN_INFO_LINE = "BanInfoLine";
        public static final String WARNING_INFO_LINE = "WarningInfoLine";
        public static final String NO_PERM = "NoPerm";
        public static final String USAGE = "Usage";
        public static final String NOT_ALLOWED_ON_SELF = "NotAllowedOnSelf";
        public static final String NO_IP_FOUND = "NoIpFound";
        public static final String INVALID_DURATION = "InvalidDuration";
        public static final String INVALID_IP = "InvalidIp";

        public Formats() {
        }
    }

    public static class Channels {
        public static final String ANNOUNCEMENT = "DeltaBansAnnouncements";
        public static final String BAN = "Ban";
        public static final String UNBAN = "Unban";
        public static final String RANGE_BAN = "RangeBan";
        public static final String RANGE_UNBAN = "RangeUnban";
        public static final String KICK = "Kick";
        public static final String WARN = "Warn";
        public static final String UNWARN = "Unwarn";
        public static final String WHITELIST_EDIT = "WhitelistEdit";
        public static final String WHITELIST_TOGGLE = "WhitelistToggle";
        public static final String CHECK_BAN = "CheckBan";
        public static final String CHECK_WARNINGS = "CheckWarnings";

        public Channels() {
        }
    }
}
