package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.DeltaBansUtils;
import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.RangeBanEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.RangeBanStorage;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MySqlRangeBanStorage implements RangeBanStorage {
    private final DeltaBansPlugin plugin;
    private final BasicLogger logger;
    private final List<RangeBanEntry> rangeBanList = new ArrayList();

    public MySqlRangeBanStorage(DeltaBansPlugin plugin, BasicLogger logger) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(logger, "logger");
        this.plugin = plugin;
        this.logger = logger;
    }

    public synchronized void load() throws SQLException {
        Connection connection = this.plugin.getConnection();
        Throwable var2 = null;

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM deltabans_rangebans;");
            Throwable var4 = null;

            try {
                ResultSet resultSet = statement.executeQuery();
                Throwable var6 = null;

                try {
                    while(resultSet.next()) {
                        RangeBanEntry rangeBanEntry = this.readRangeBanEntry(resultSet);
                        if (rangeBanEntry != null) {
                            this.rangeBanList.add(rangeBanEntry);
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

        this.logger.info("Loaded %s range bans", new Object[]{this.rangeBanList.size()});
    }

    public synchronized void save() {
    }

    public synchronized RangeBanEntry getIpRangeBan(String ip) {
        return this.getIpRangeBan(DeltaBansUtils.convertIpToLong(ip));
    }

    public synchronized RangeBanEntry getIpRangeBan(long ipAsLong) {
        Iterator var3 = this.rangeBanList.iterator();

        RangeBanEntry entry;
        do {
            if (!var3.hasNext()) {
                return null;
            }

            entry = (RangeBanEntry)var3.next();
        } while(ipAsLong < entry.getStartAddressLong() || ipAsLong > entry.getEndAddressLong());

        return entry;
    }

    public synchronized void add(RangeBanEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        this.rangeBanList.add(entry);
        this.addToDatabase(entry);
    }

    public synchronized int removeIpRangeBan(String ip) {
        return this.removeIpRangeBan(DeltaBansUtils.convertIpToLong(ip));
    }

    public synchronized int removeIpRangeBan(long ipAsLong) {
        int count = 0;
        ListIterator iterator = this.rangeBanList.listIterator();

        while(iterator.hasNext()) {
            RangeBanEntry entry = (RangeBanEntry)iterator.next();
            if (ipAsLong >= entry.getStartAddressLong() && ipAsLong <= entry.getEndAddressLong()) {
                iterator.remove();
                this.removeFromDatabase(entry);
                ++count;
            }
        }

        return count;
    }

    private void addToDatabase(RangeBanEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" INSERT INTO deltabans_rangebans (ip_start, ip_end, banner, message, createdAt) VALUES (?,?,?,?,?);");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, entry.getStartAddress());
                        statement.setString(2, entry.getEndAddress());
                        statement.setString(3, entry.getBanner());
                        statement.setString(4, entry.getMessage());
                        statement.setTimestamp(5, new Timestamp(entry.getCreatedAt()));
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

    private void removeFromDatabase(RangeBanEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" DELETE FROM deltabans_rangebans WHERE ip_start = ? AND ip_end = ? AND banner = ? AND message = ? LIMIT 1;");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, entry.getStartAddress());
                        statement.setString(2, entry.getEndAddress());
                        statement.setString(3, entry.getBanner());
                        statement.setString(4, entry.getMessage());
                        statement.executeUpdate();
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

    private RangeBanEntry readRangeBanEntry(ResultSet resultSet) {
        try {
            String banner = resultSet.getString("banner");
            String message = resultSet.getString("message");
            String ipStart = resultSet.getString("ip_start");
            String ipEnd = resultSet.getString("ip_end");
            long createdAt = resultSet.getTimestamp("createdAt").getTime();
            return new RangeBanEntry(banner, message, ipStart, ipEnd, createdAt);
        } catch (NullPointerException | SQLException var8) {
            var8.printStackTrace();
            return null;
        }
    }
}
