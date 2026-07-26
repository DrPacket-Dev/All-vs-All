package org.drpacket.allvsall;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DatabaseManager {
    public enum DatabaseType {
        SQLITE,
        MYSQL,
        MARIADB
    }

    private final DatabaseType databaseType;
    private final String databasePath;
    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;

    public DatabaseManager(DatabaseType databaseType, String databasePath, String host, int port, String databaseName, String username, String password) {
        this.databaseType = databaseType;
        this.databasePath = databasePath;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }

    public DatabaseManager(File databaseFile) {
        this(DatabaseType.SQLITE, databaseFile.getAbsolutePath(), null, 0, null, null, null);
    }

    public void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kits (
                    kit_name VARCHAR(255) PRIMARY KEY,
                    layout TEXT NOT NULL
                )
                """);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize database", exception);
        }
    }

    public void saveKit(String kitName, String layout) {
        String sql = switch (databaseType) {
            case SQLITE -> "INSERT INTO kits(kit_name, layout) VALUES(?, ?) ON CONFLICT(kit_name) DO UPDATE SET layout = excluded.layout";
            default -> "INSERT INTO kits(kit_name, layout) VALUES(?, ?) ON DUPLICATE KEY UPDATE layout = VALUES(layout)";
        };

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, kitName);
            statement.setString(2, layout);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to save kit", exception);
        }
    }

    public String loadKit(String kitName) {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT layout FROM kits WHERE kit_name = ?")) {
            statement.setString(1, kitName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("layout");
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to load kit", exception);
        }
        return null;
    }

    private Connection getConnection() throws SQLException {
        return switch (databaseType) {
            case SQLITE -> DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            case MARIADB -> DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + databaseName + "?useSSL=false", username, password);
            case MYSQL -> DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + databaseName + "?useSSL=false&serverTimezone=UTC", username, password);
        };
    }
}
