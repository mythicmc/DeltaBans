package com.gmail.tracebachi.DeltaBans.Bungee.Storage.MySQL;

import com.gmail.tracebachi.DeltaBans.Bungee.DeltaBansPlugin;
import com.gmail.tracebachi.DeltaBans.Bungee.Storage.WhitelistStorage;
import com.gmail.tracebachi.SockExchange.Utilities.BasicLogger;
import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MySqlWhitelistStorage implements WhitelistStorage {
    private final int NORMAL_WHITELIST = 1;
    private final int RANGEBAN_WHITELIST = 2;
    private final DeltaBansPlugin plugin;
    private final BasicLogger logger;
    private final Map<String, Integer> whitelistMap = new HashMap();

    public MySqlWhitelistStorage(DeltaBansPlugin plugin, BasicLogger logger) {
        Preconditions.checkNotNull(plugin, "plugin");
        Preconditions.checkNotNull(logger, "logger");
        this.plugin = plugin;
        this.logger = logger;
    }

    public synchronized void load() throws SQLException {
        Connection connection = this.plugin.getConnection();
        Throwable var2 = null;

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM deltabans_whitelist;");
            Throwable var4 = null;

            try {
                ResultSet resultSet = statement.executeQuery();
                Throwable var6 = null;

                try {
                    while(resultSet.next()) {
                        try {
                            String name = resultSet.getString("name").toLowerCase();
                            int type = resultSet.getInt("type");
                            this.whitelistMap.put(name, type);
                        } catch (SQLException var54) {
                            var54.printStackTrace();
                        }
                    }
                } catch (Throwable var55) {
                    var6 = var55;
                    throw var55;
                } finally {
                    if (resultSet != null) {
                        if (var6 != null) {
                            try {
                                resultSet.close();
                            } catch (Throwable var53) {
                                var6.addSuppressed(var53);
                            }
                        } else {
                            resultSet.close();
                        }
                    }

                }
            } catch (Throwable var57) {
                var4 = var57;
                throw var57;
            } finally {
                if (statement != null) {
                    if (var4 != null) {
                        try {
                            statement.close();
                        } catch (Throwable var52) {
                            var4.addSuppressed(var52);
                        }
                    } else {
                        statement.close();
                    }
                }

            }
        } catch (Throwable var59) {
            var2 = var59;
            throw var59;
        } finally {
            if (connection != null) {
                if (var2 != null) {
                    try {
                        connection.close();
                    } catch (Throwable var51) {
                        var2.addSuppressed(var51);
                    }
                } else {
                    connection.close();
                }
            }

        }

        this.logger.info("Loaded normal and rangeban whitelists for %s players", new Object[]{this.whitelistMap.size()});
    }

    public synchronized void save() {
    }

    public synchronized boolean isOnNormalWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        return ((Integer)this.whitelistMap.getOrDefault(name, 0) & 1) != 0;
    }

    public synchronized boolean isOnRangeBanWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        return ((Integer)this.whitelistMap.getOrDefault(name, 0) & 2) != 0;
    }

    public synchronized boolean addToNormalWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        Integer stored = (Integer)this.whitelistMap.get(name);
        if (stored != null) {
            if ((stored & 1) != 0) {
                return false;
            } else {
                this.whitelistMap.put(name, stored | 1);
                this.updateInDatabase(name, stored | 1);
                return true;
            }
        } else {
            this.whitelistMap.put(name, 1);
            this.addToDatabase(name, 1);
            return true;
        }
    }

    public boolean addToRangeBanWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        Integer stored = (Integer)this.whitelistMap.get(name);
        if (stored != null) {
            if ((stored & 2) != 0) {
                return false;
            } else {
                this.whitelistMap.put(name, stored | 2);
                this.updateInDatabase(name, stored | 2);
                return true;
            }
        } else {
            this.whitelistMap.put(name, 2);
            this.addToDatabase(name, 2);
            return true;
        }
    }

    public synchronized boolean removeFromNormalWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        Integer stored = (Integer)this.whitelistMap.get(name);
        if (stored == null) {
            return false;
        } else {
            Integer newFlags = stored & -2;
            if (stored.equals(newFlags)) {
                return false;
            } else if (newFlags == 0) {
                this.whitelistMap.remove(name);
                this.removeFromDatabase(name);
                return true;
            } else {
                this.whitelistMap.put(name, newFlags);
                this.updateInDatabase(name, newFlags);
                return true;
            }
        }
    }

    public synchronized boolean removeFromRangeBanWhitelist(String name) {
        Preconditions.checkNotNull(name, "name");
        name = name.toLowerCase();
        Integer stored = (Integer)this.whitelistMap.get(name);
        if (stored == null) {
            return false;
        } else {
            Integer newFlags = stored & -3;
            if (stored.equals(newFlags)) {
                return false;
            } else if (newFlags == 0) {
                this.whitelistMap.remove(name);
                this.removeFromDatabase(name);
                return true;
            } else {
                this.whitelistMap.put(name, newFlags);
                this.updateInDatabase(name, newFlags);
                return true;
            }
        }
    }

    private void addToDatabase(String name, Integer type) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(type, "type");
        String finalName = name.toLowerCase();
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var4 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" INSERT INTO deltabans_whitelist (name, type) VALUES (?,?);");
                    Throwable var6 = null;

                    try {
                        statement.setString(1, finalName);
                        statement.setInt(2, type);
                        statement.execute();
                    } catch (Throwable var31) {
                        var6 = var31;
                        throw var31;
                    } finally {
                        if (statement != null) {
                            if (var6 != null) {
                                try {
                                    statement.close();
                                } catch (Throwable var30) {
                                    var6.addSuppressed(var30);
                                }
                            } else {
                                statement.close();
                            }
                        }

                    }
                } catch (Throwable var33) {
                    var4 = var33;
                    throw var33;
                } finally {
                    if (connection != null) {
                        if (var4 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var29) {
                                var4.addSuppressed(var29);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var35) {
                var35.printStackTrace();
            }

        });
    }

    private void removeFromDatabase(String name) {
        Preconditions.checkNotNull(name, "name");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var3 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" DELETE FROM deltabans_whitelist WHERE name = ?");
                    Throwable var5 = null;

                    try {
                        statement.setString(1, name);
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

    private void updateInDatabase(String name, Integer type) {
        Preconditions.checkNotNull(name, "name");
        Preconditions.checkNotNull(type, "type");
        this.plugin.getExecutor().execute(() -> {
            try {
                Connection connection = this.plugin.getConnection();
                Throwable var4 = null;

                try {
                    PreparedStatement statement = connection.prepareStatement(" UPDATE deltabans_whitelist SET type = ? WHERE name = ? LIMIT 1;");
                    Throwable var6 = null;

                    try {
                        statement.setInt(1, type);
                        statement.setString(2, name);
                        statement.executeUpdate();
                    } catch (Throwable var31) {
                        var6 = var31;
                        throw var31;
                    } finally {
                        if (statement != null) {
                            if (var6 != null) {
                                try {
                                    statement.close();
                                } catch (Throwable var30) {
                                    var6.addSuppressed(var30);
                                }
                            } else {
                                statement.close();
                            }
                        }

                    }
                } catch (Throwable var33) {
                    var4 = var33;
                    throw var33;
                } finally {
                    if (connection != null) {
                        if (var4 != null) {
                            try {
                                connection.close();
                            } catch (Throwable var29) {
                                var4.addSuppressed(var29);
                            }
                        } else {
                            connection.close();
                        }
                    }

                }
            } catch (SQLException var35) {
                var35.printStackTrace();
            }

        });
    }
}
