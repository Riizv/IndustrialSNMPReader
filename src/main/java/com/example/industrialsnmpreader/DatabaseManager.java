package com.example.industrialsnmpreader;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static String dbUrl;

    static {
        initializeDatabasePath();
        createTableIfNotExist();
    }

    private static void initializeDatabasePath() {
        String appName = "IndustrialSNMPReader";
        String os = System.getProperty("os.name").toLowerCase();
        String path;

        if (os.contains("win")) {
            // Próba użycia Program Files (wymaga uprawnień administratora przy tworzeniu folderu)
            path = "C:\\Program Files\\" + appName + "\\";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            // Próba użycia /opt/
            path = "/opt/" + appName + ".d/";
        } else {
            path = System.getProperty("user.home") + File.separator + appName + File.separator;
        }

        File directory = new File(path);
        
        // Sprawdzenie uprawnień do zapisu. Jeśli nie mamy dostępu do /opt/ lub Program Files, 
        // spadamy bezpiecznie do katalogu domowego użytkownika (AppData lub ~/.config).
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                // Brak uprawnień do systemowych ścieżek - używamy ścieżki użytkownika
                if (os.contains("win")) {
                    path = System.getenv("APPDATA") + File.separator + appName + File.separator;
                } else if (os.contains("mac")) {
                    path = System.getProperty("user.home") + "/Library/Application Support/" + appName + "/";
                } else {
                    path = System.getProperty("user.home") + "/.config/" + appName + "/";
                }
                new File(path).mkdirs();
            }
        } else if (!directory.canWrite()) {
            // Jeśli folder istnieje, ale nie możemy w nim pisać (np. zainstalowany przez instalator, ale apka działa bez sudo)
            path = System.getProperty("user.home") + File.separator + "." + appName + File.separator;
            new File(path).mkdirs();
        }

        String dbPath = path + "devices.db";
        dbUrl = "jdbc:sqlite:" + dbPath;
        System.out.println("Baza danych znajduje się pod adresem: " + dbPath);
    }

    private static void createTableIfNotExist() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                String createTable = "CREATE TABLE IF NOT EXISTS devices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ip TEXT NOT NULL, " +
                        "vendor TEXT NOT NULL" +
                        ");";
                Statement stmt = conn.createStatement();
                stmt.execute(createTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public static List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT id, ip, vendor FROM devices";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                devices.add(new Device(
                        rs.getInt("id"),
                        rs.getString("ip"),
                        rs.getString("vendor")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return devices;
    }

    public static int addDevice(String ip, String vendor) {
        String sql = "INSERT INTO devices(ip, vendor) VALUES(?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, ip);
            pstmt.setString(2, vendor);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static void deleteDevice(int id) {
        String sql = "DELETE FROM devices WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
