package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.BanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.BanStorage.AddResult;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MySqlBanStorage implements BanStorage {
    private final DeltaBansPlugin plugin;
    private final Set<BanEntry> banSet = new HashSet();
    private final Map<String, List<BanEntry>> nameBanMap = new HashMap();
    private final Map<String, List<BanEntry>> ipBanMap = new HashMap();
    private final BasicLogger logger;

    public MySqlBanStorage(DeltaBansPlugin plugin, BasicLogger logger) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(logger, "logger");
        this.plugin = plugin;
        this.logger = logger;
    }

    public synchronized void load() throws SQLException {
        this.banSet.clear();
        this.nameBanMap.clear();
        this.ipBanMap.clear();
        Connection connection = this.plugin.getConnection();
        Throwable var2 = null;

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM deltabans_bans;");
            Throwable var4 = null;

            try {
                ResultSet resultSet = statement.executeQuery();
                Throwable var6 = null;

                try {
                    while(resultSet.next()) {
                        BanEntry banEntry = this.readBanEntry(resultSet);
                        if (banEntry != null) {
                            this.addToMemory(banEntry);
                        }
                    }
                } catch (Throwable var50) {
                    var6 = var50;
                    throw var50;
                } finally {
                    if (resultSet != null) {
                        if (var6 != null) {
                            try {
                                resultSet.close();
                            } catch (Throwable var49) {
                                var6.addSuppressed(var49);
                            }
                        } else {
                            resultSet.close();
                        }
                    }

                }
            } catch (Throwable var52) {
                var4 = var52;
                throw var52;
            } finally {
                if (statement != null) {
                    if (var4 != null) {
                        try {
                            statement.close();
                        } catch (Throwable var48) {
                            var4.addSuppressed(var48);
                        }
                    } else {
                        statement.close();
                    }
                }

            }
        } catch (Throwable var54) {
            var2 = var54;
            throw var54;
        } finally {
            if (connection != null) {
                if (var2 != null) {
                    try {
                        connection.close();
                    } catch (Throwable var47) {
                        var2.addSuppressed(var47);
                    }
                } else {
                    connection.close();
                }
            }

        }

        this.removeExpiredBans();
        this.removeDuplicateBans();
        this.logger.info("Loaded %s bans. %s bans have a name. %s bans have an IP.", new Object[]{this.banSet.size(), this.nameBanMap.size(), this.ipBanMap.size()});
    }

    public synchronized void save() {
    }

    public synchronized BanEntry getBanEntry(String name, String ip) {
        Preconditions.checkNotNull(name != null || ip != null, "name and ip both null");
        BanEntry foundEntry = null;
        List banEntryList;
        ArrayList entriesToRemove;
        Iterator var6;
        BanEntry b;
        if (name != null) {
            name = name.toLowerCase();
            banEntryList = (List)this.nameBanMap.get(name);
            if (banEntryList != null) {
                if (banEntryList.isEmpty()) {
                    this.nameBanMap.remove(name);
                } else {
                    entriesToRemove = new ArrayList(2);
                    var6 = banEntryList.iterator();

                    while(var6.hasNext()) {
                        b = (BanEntry)var6.next();
                        if (b.isDurationComplete()) {
                            entriesToRemove.add(b);
                        } else if (foundEntry == null) {
                            foundEntry = b;
                        }
                    }

                    var6 = entriesToRemove.iterator();

                    while(var6.hasNext()) {
                        b = (BanEntry)var6.next();
                        this.removeFromMemory(b);
                        this.removeFromDatabase(b);
                    }
                }
            }
        }

        if (foundEntry != null) {
            return foundEntry;
        } else {
            if (ip != null) {
                banEntryList = (List)this.ipBanMap.get(ip);
                if (banEntryList != null) {
                    if (banEntryList.isEmpty()) {
                        this.ipBanMap.remove(ip);
                    } else {
                        entriesToRemove = new ArrayList(2);
                        var6 = banEntryList.iterator();

                        while(var6.hasNext()) {
                            b = (BanEntry)var6.next();
                            if (b.isDurationComplete()) {
                                entriesToRemove.add(b);
                            } else if (foundEntry == null) {
                                foundEntry = b;
                            }
                        }

                        var6 = entriesToRemove.iterator();

                        while(var6.hasNext()) {
                            b = (BanEntry)var6.next();
                            this.removeFromMemory(b);
                            this.removeFromDatabase(b);
                        }
                    }
                }
            }

            return foundEntry;
        }
    }

    public synchronized AddResult addBanEntry(BanEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        boolean hasName = entry.hasName();
        boolean hasIp = entry.hasIp();
        String name = entry.getName();
        String ip = entry.getIp();
        List banEntryList;
        if (hasName && !hasIp) {
            banEntryList = (List)this.nameBanMap.get(name);
            if (banEntryList != null && !banEntryList.isEmpty()) {
                return AddResult.EXISTING_NAME_BAN;
            } else {
                this.addToMemory(entry);
                this.addToDatabase(entry);
                return AddResult.SUCCESS;
            }
        } else if (!hasName && hasIp) {
            banEntryList = (List)this.ipBanMap.get(ip);
            if (banEntryList != null && !banEntryList.isEmpty()) {
                return AddResult.EXISTING_IP_BAN;
            } else {
                this.addToMemory(entry);
                this.addToDatabase(entry);
                return AddResult.SUCCESS;
            }
        } else {
            banEntryList = (List)this.nameBanMap.get(name);
            Iterator var7;
            BanEntry b;
            if (banEntryList != null) {
                if (banEntryList.isEmpty()) {
                    this.nameBanMap.remove(name);
                } else {
                    var7 = banEntryList.iterator();

                    while(var7.hasNext()) {
                        b = (BanEntry)var7.next();
                        if (b.hasName() && b.hasIp() && b.getName().equalsIgnoreCase(name)) {
                            return AddResult.EXISTING_NAME_AND_IP_BAN;
                        }
                    }
                }
            }

            banEntryList = (List)this.ipBanMap.get(ip);
            if (banEntryList != null) {
                if (banEntryList.isEmpty()) {
                    this.ipBanMap.remove(ip);
                } else {
                    var7 = banEntryList.iterator();

                    while(var7.hasNext()) {
                        b = (BanEntry)var7.next();
                        if (b.hasName() && b.hasIp() && b.getIp().equals(ip)) {
                            return AddResult.EXISTING_NAME_AND_IP_BAN;
                        }
                    }
                }
            }

            this.addToMemory(entry);
            this.addToDatabase(entry);
            return AddResult.SUCCESS;
        }
    }

    public synchronized List<BanEntry> removeUsingIp(String ip) {
        Preconditions.checkNotNull(ip, "ip");
        List<BanEntry> banEntryList = (List)this.ipBanMap.remove(ip);
        if (banEntryList == null) {
            return Collections.emptyList();
        } else {
            Iterator var3 = banEntryList.iterator();

            while(var3.hasNext()) {
                BanEntry entry = (BanEntry)var3.next();
                this.removeFromMemory(entry);
                this.removeFromDatabase(entry);
                if (entry.hasName()) {
                    BanEntry newEntry = new BanEntry(entry.getName(), (String)null, entry.getBanner(), entry.getMessage(), entry.getDuration(), entry.getCreatedAt());
                    this.addToMemory(newEntry);
                    this.addToDatabase(newEntry);
                }
            }

            return banEntryList;
        }
    }

    public synchronized List<BanEntry> removeUsingName(String name) {
        name = ((String)Preconditions.checkNotNull(name, "name")).toLowerCase();
        List<BanEntry> banEntryList = (List)this.nameBanMap.remove(name);
        if (banEntryList == null) {
            return Collections.emptyList();
        } else {
            Iterator var3 = banEntryList.iterator();

            while(var3.hasNext()) {
                BanEntry entry = (BanEntry)var3.next();
                this.removeFromMemory(entry);
                this.removeFromDatabase(entry);
            }

            return banEntryList;
        }
    }

    private void addToMemory(BanEntry entry) {
        this.banSet.add(entry);
        String ip;
        if (entry.hasName()) {
            ip = entry.getName();
            ((List)this.nameBanMap.computeIfAbsent(ip, (k) -> {
                return new ArrayList();
            })).add(entry);
        }

        if (entry.hasIp()) {
            ip = entry.getIp();
            ((List)this.ipBanMap.computeIfAbsent(ip, (k) -> {
                return new ArrayList();
            })).add(entry);
        }

    }

    private void removeFromMemory(BanEntry entry) {
        this.banSet.remove(entry);
        String ip;
        List banEntryList;
        if (entry.hasName()) {
            ip = entry.getName();
            banEntryList = (List)this.nameBanMap.get(ip);
            if (banEntryList != null) {
                banEntryList.remove(entry);
                if (banEntryList.isEmpty()) {
                    this.nameBanMap.remove(ip);
                }
            }
        }

        if (entry.hasIp()) {
            ip = entry.getIp();
            banEntryList = (List)this.ipBanMap.get(ip);
            if (banEntryList != null) {
                banEntryList.remove(entry);
                if (banEntryList.isEmpty()) {
                    this.ipBanMap.remove(ip);
                }
            }
        }

    }

    private void addToDatabase(BanEntry entry) {
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" INSERT INTO deltabans_bans (name, ip, banner, message, duration, createdAt) VALUES (?,?,?,?,?,?);");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getIp());
                        statement.setString(3, entry.getBanner());
                        statement.setString(4, entry.getMessage());
                        if (entry.getDuration() == null) {
                            statement.setNull(5, -5);
                        } else {
                            statement.setLong(5, entry.getDuration());
                        }

                        statement.setTimestamp(6, new Timestamp(entry.getCreatedAt()));
                        statement.execute();
                    } catch (Throwable var30) {
                        var5 = var30;
                        throw var30;
                    } finally {
                        if (statement != null) {
                            if (var5 != null) {
                                try {
                                    statement.close();
                                } catch (Throwable var29) {
                                    var5.addSuppressed(var29);
                                }
                            } else {
                                statement.close();
                            }
                        }

                    }
                } catch (Throwable var32) {
                    var3 = var32;
                    throw var32;
                } finally {
                    if (connection != null) {
                        if (var3 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var28) {
                                var3.addSuppressed(var28);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var34) {
                var34.printStackTrace();
            }

        });
    }

    private void removeFromDatabase(BanEntry entry) {
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement;
                    Throwable var5;
                    if (entry.hasName() && entry.hasIp()) {
                        statement = connection.prepareStatement(" DELETE FROM deltabans_bans WHERE name = ? AND ip = ? AND banner = ? AND message = ? LIMIT 1;");
                        var5 = null;

                        try {
                            statement.setString(1, entry.getName());
                            statement.setString(2, entry.getIp());
                            statement.setString(3, entry.getBanner());
                            statement.setString(4, entry.getMessage());
                            statement.executeUpdate();
                        } catch (Throwable var83) {
                            var5 = var83;
                            throw var83;
                        } finally {
                            if (statement != null) {
                                if (var5 != null) {
                                    try {
                                        statement.close();
                                    } catch (Throwable var80) {
                                        var5.addSuppressed(var80);
                                    }
                                } else {
                                    statement.close();
                                }
                            }

                        }
                    } else if (entry.hasName() && !entry.hasIp()) {
                        statement = connection.prepareStatement(" DELETE FROM deltabans_bans WHERE name = ? AND ip is NULL AND banner = ? AND message = ? LIMIT 1;");
                        var5 = null;

                        try {
                            statement.setString(1, entry.getName());
                            statement.setString(2, entry.getBanner());
                            statement.setString(3, entry.getMessage());
                            statement.executeUpdate();
                        } catch (Throwable var82) {
                            var5 = var82;
                            throw var82;
                        } finally {
                            if (statement != null) {
                                if (var5 != null) {
                                    try {
                                        statement.close();
                                    } catch (Throwable var79) {
                                        var5.addSuppressed(var79);
                                    }
                                } else {
                                    statement.close();
                                }
                            }

                        }
                    } else if (!entry.hasName() && entry.hasIp()) {
                        statement = connection.prepareStatement(" DELETE FROM deltabans_bans WHERE name is NULL AND ip = ? AND banner = ? AND message = ? LIMIT 1;");
                        var5 = null;

                        try {
                            statement.setString(1, entry.getIp());
                            statement.setString(2, entry.getBanner());
                            statement.setString(3, entry.getMessage());
                            statement.executeUpdate();
                        } catch (Throwable var81) {
                            var5 = var81;
                            throw var81;
                        } finally {
                            if (statement != null) {
                                if (var5 != null) {
                                    try {
                                        statement.close();
                                    } catch (Throwable var78) {
                                        var5.addSuppressed(var78);
                                    }
                                } else {
                                    statement.close();
                                }
                            }

                        }
                    }
                } catch (Throwable var87) {
                    var3 = var87;
                    throw var87;
                } finally {
                    if (connection != null) {
                        if (var3 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var77) {
                                var3.addSuppressed(var77);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var89) {
                var89.printStackTrace();
            }

        });
    }

    private BanEntry readBanEntry(ResultSet resultSet) {
        try {
            String name = resultSet.getString("name");
            String ip = resultSet.getString("ip");
            String banner = resultSet.getString("banner");
            String message = resultSet.getString("message");
            Long duration = resultSet.getLong("duration");
            Long createdAt = resultSet.getTimestamp("createdAt").getTime();
            return new BanEntry(name, ip, banner, message, duration, createdAt);
        } catch (IllegalArgumentException | SQLException var8) {
            var8.printStackTrace();
            return null;
        }
    }

    private void removeExpiredBans() {
        List<BanEntry> entriesToRemove = new ArrayList(16);
        Iterator var2 = this.banSet.iterator();

        BanEntry b;
        while(var2.hasNext()) {
            b = (BanEntry)var2.next();
            if (b.isDurationComplete()) {
                entriesToRemove.add(b);
            }
        }

        var2 = entriesToRemove.iterator();

        while(var2.hasNext()) {
            b = (BanEntry)var2.next();
            this.removeFromMemory(b);
            this.removeFromDatabase(b);
        }

    }

    private void removeDuplicateBans() {
        List<BanEntry> entriesToRemove = new ArrayList(16);
        Iterator var2 = this.banSet.iterator();

        BanEntry ban;
        while(var2.hasNext()) {
            ban = (BanEntry)var2.next();
            String banName = ban.getName();
            String banIp = ban.getIp();
            List<BanEntry> list = (List)this.nameBanMap.get(banName);
            list = list == null ? Collections.emptyList() : list;
            Iterator var7 = list.iterator();

            BanEntry banToCheck;
            while(var7.hasNext()) {
                banToCheck = (BanEntry)var7.next();
                if (banToCheck != ban && Objects.equals(banToCheck.getName(), banName) && Objects.equals(banToCheck.getIp(), banIp)) {
                    entriesToRemove.add(banToCheck);
                }
            }

            list = (List)this.ipBanMap.get(banIp);
            list = list == null ? Collections.emptyList() : list;
            var7 = list.iterator();

            while(var7.hasNext()) {
                banToCheck = (BanEntry)var7.next();
                if (banToCheck != ban && Objects.equals(banToCheck.getName(), banName) && Objects.equals(banToCheck.getIp(), banIp)) {
                    entriesToRemove.add(banToCheck);
                }
            }
        }

        var2 = entriesToRemove.iterator();

        while(var2.hasNext()) {
            ban = (BanEntry)var2.next();
            this.removeFromMemory(ban);
            this.removeFromDatabase(ban);
        }

    }
}
