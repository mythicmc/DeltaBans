package com.gmail.tracebachi.DeltaBans.Bungee;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executor;

public interface DeltaBansPluginInterface {
    Executor getExecutor();
    Connection getConnection() throws SQLException;
}
