package org.drpacket.allvsall;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
public class DatabaseManager {
    private final File databaseFile;

    public DatabaseManager(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    public void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS kits (
                    kit_name TEXT PRIMARY KEY,
                    layout TEXT NOT NULL
                )
                """);
        } catch (SQLException exception) {
            throw new RuntimeException("Failed to initialize database", exception);
        }
    }

    public void saveKit(String kitName, String layout) {
        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO kits(kit_name, layout) VALUES(?, ?) ON CONFLICT(kit_name) DO UPDATE SET layout = excluded.layout")) {
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
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        return DriverManager.getConnection(url);
    }
}
