package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Entries.WarningEntry;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WarningStorage;
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
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

public class MySqlWarningStorage implements WarningStorage {
    private final DeltaBansPlugin plugin;
    private final BasicLogger logger;
    private final long warningDuration;
    private final HashMap<String, List<WarningEntry>> warningsMap = new HashMap();

    public MySqlWarningStorage(DeltaBansPlugin plugin, BasicLogger logger, long warningDuration) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(logger, "logger");
        this.plugin = plugin;
        this.logger = logger;
        this.warningDuration = warningDuration;
    }

    public synchronized void load() throws SQLException {
        List<WarningEntry> entriesToRemove = new ArrayList(16);
        Connection connection = this.plugin.getConnection();
        Throwable var3 = null;

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM deltabans_warnings;");
            Throwable var5 = null;

            try {
                ResultSet resultSet = statement.executeQuery();
                Throwable var7 = null;

                try {
                    while(resultSet.next()) {
                        WarningEntry warningEntry = this.readWarningEntry(resultSet);
                        if (warningEntry != null) {
                            if (System.currentTimeMillis() - warningEntry.getCreatedAt() >= this.warningDuration) {
                                entriesToRemove.add(warningEntry);
                            } else {
                                String name = warningEntry.getName();
                                List<WarningEntry> warningEntries = (List)this.warningsMap.computeIfAbsent(name, (k) -> {
                                    return new ArrayList();
                                });
                                warningEntries.add(warningEntry);
                            }
                        }
                    }
                } catch (Throwable var53) {
                    var7 = var53;
                    throw var53;
                } finally {
                    if (resultSet != null) {
                        if (var7 != null) {
                            try {
                                resultSet.close();
                            } catch (Throwable var52) {
                                var7.addSuppressed(var52);
                            }
                        } else {
                            resultSet.close();
                        }
                    }

                }
            } catch (Throwable var55) {
                var5 = var55;
                throw var55;
            } finally {
                if (statement != null) {
                    if (var5 != null) {
                        try {
                            statement.close();
                        } catch (Throwable var51) {
                            var5.addSuppressed(var51);
                        }
                    } else {
                        statement.close();
                    }
                }

            }
        } catch (Throwable var57) {
            var3 = var57;
            throw var57;
        } finally {
            if (connection != null) {
                if (var3 != null) {
                    try {
                        connection.close();
                    } catch (Throwable var50) {
                        var3.addSuppressed(var50);
                    }
                } else {
                    connection.close();
                }
            }

        }

        Iterator var59 = entriesToRemove.iterator();

        while(var59.hasNext()) {
            WarningEntry warningEntry = (WarningEntry)var59.next();
            this.removeFromDatabase(warningEntry);
        }

        this.logger.info("Loaded warnings for %s players", new Object[]{this.warningsMap.size()});
    }

    public synchronized void save() {
    }

    public synchronized int addWarning(WarningEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        String name = entry.getName();
        List<WarningEntry> warnings = (List)this.warningsMap.computeIfAbsent(name, (k) -> {
            return new ArrayList();
        });
        warnings.add(entry);
        this.addToDatabase(entry);
        return warnings.size();
    }

    public synchronized int removeWarnings(String name, int amount) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        int count = 0;
        List<WarningEntry> warnings = (List)this.warningsMap.get(name);
        if (warnings != null) {
            amount = Math.min(amount, warnings.size());

            for(int i = amount; i > 0; --i) {
                WarningEntry warningEntry = (WarningEntry)warnings.remove(i - 1);
                this.removeFromDatabase(warningEntry);
                ++count;
            }

            if (warnings.isEmpty()) {
                this.warningsMap.remove(name);
            }
        }

        return count;
    }

    public synchronized List<WarningEntry> getWarnings(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        List<WarningEntry> warnings = (List)this.warningsMap.get(name);
        if (warnings == null) {
            return Collections.emptyList();
        } else if (warnings.size() == 0) {
            this.warningsMap.remove(name);
            return Collections.emptyList();
        } else {
            return Collections.unmodifiableList(warnings);
        }
    }

    public synchronized void removeExpiredWarnings() {
        Iterator<Entry<String, List<WarningEntry>>> iterator = this.warningsMap.entrySet().iterator();
        long oldestTime = System.currentTimeMillis() - this.warningDuration;

        while(iterator.hasNext()) {
            Entry<String, List<WarningEntry>> entry = (Entry)iterator.next();
            ListIterator listIterator = ((List)entry.getValue()).listIterator();

            while(listIterator.hasNext()) {
                WarningEntry warningEntry = (WarningEntry)listIterator.next();
                if (warningEntry.getCreatedAt() < oldestTime) {
                    listIterator.remove();
                    this.removeFromDatabase(warningEntry);
                }
            }

            if (((List)entry.getValue()).size() == 0) {
                iterator.remove();
            }
        }

    }

    private void addToDatabase(WarningEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" INSERT INTO deltabans_warnings (name, warner, message, createdAt) VALUES (?,?,?,?);");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getWarner());
                        statement.setString(3, entry.getMessage());
                        statement.setTimestamp(4, new Timestamp(entry.getCreatedAt()));
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

    private void removeFromDatabase(WarningEntry entry) {
        Preconditions.checkNotNull(entry, "entry");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" DELETE FROM deltabans_warnings WHERE name = ? AND warner = ? AND message = ? LIMIT 1;");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, entry.getName());
                        statement.setString(2, entry.getWarner());
                        statement.setString(3, entry.getMessage());
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

    private WarningEntry readWarningEntry(ResultSet resultSet) {
        try {
            String name = resultSet.getString("name");
            String warner = resultSet.getString("warner");
            String message = resultSet.getString("message");
            long createdAt = resultSet.getTimestamp("createdAt").getTime();
            return new WarningEntry(name, warner, message, createdAt);
        } catch (NullPointerException | SQLException var7) {
            var7.printStackTrace();
            return null;
        }
    }
}
