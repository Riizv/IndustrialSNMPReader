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
        upgradeTableIfNeeded();
    }

    private static void initializeDatabasePath() {
        String appName = "IndustrialSNMPReader";
        String os = System.getProperty("os.name").toLowerCase();
        String path;

        if (os.contains("win")) {
            path = "C:\\Program Files\\" + appName + "\\";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            path = "/opt/" + appName + ".d/";
        } else {
            path = System.getProperty("user.home") + File.separator + appName + File.separator;
        }

        File directory = new File(path);
        
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
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
            path = System.getProperty("user.home") + File.separator + "." + appName + File.separator;
            new File(path).mkdirs();
        }

        String dbPath = path + "devices.db";
        dbUrl = "jdbc:sqlite:" + dbPath;
    }

    private static void createTableIfNotExist() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            if (conn != null) {
                String createTable = "CREATE TABLE IF NOT EXISTS devices (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "ip TEXT NOT NULL, " +
                        "vendor TEXT NOT NULL, " +
                        "snmp_version INTEGER DEFAULT 1, " +
                        "community TEXT DEFAULT 'public', " +
                        "security_name TEXT DEFAULT '', " +
                        "auth_protocol TEXT DEFAULT 'NONE', " +
                        "auth_passphrase TEXT DEFAULT '', " +
                        "priv_protocol TEXT DEFAULT 'NONE', " +
                        "priv_passphrase TEXT DEFAULT ''" +
                        ");";
                Statement stmt = conn.createStatement();
                stmt.execute(createTable);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void upgradeTableIfNeeded() {
        String[] columns = {
            "snmp_version INTEGER DEFAULT 1",
            "community TEXT DEFAULT 'public'",
            "security_name TEXT DEFAULT ''",
            "auth_protocol TEXT DEFAULT 'NONE'",
            "auth_passphrase TEXT DEFAULT ''",
            "priv_protocol TEXT DEFAULT 'NONE'",
            "priv_passphrase TEXT DEFAULT ''"
        };
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            for (String col : columns) {
                String colName = col.split(" ")[0];
                try {
                    Statement stmt = conn.createStatement();
                    stmt.execute("ALTER TABLE devices ADD COLUMN " + col);
                } catch (SQLException e) {
                    // Ignorujemy błąd jeśli kolumna już istnieje
                }
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
        String sql = "SELECT * FROM devices";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Device d = new Device(
                        rs.getInt("id"),
                        rs.getString("ip"),
                        rs.getString("vendor")
                );
                d.setSnmpSettings(
                    rs.getInt("snmp_version"),
                    rs.getString("community"),
                    rs.getString("security_name"),
                    rs.getString("auth_protocol"),
                    rs.getString("auth_passphrase"),
                    rs.getString("priv_protocol"),
                    rs.getString("priv_passphrase")
                );
                devices.add(d);
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

    public static void updateDeviceSnmpSettings(Device d) {
        String sql = "UPDATE devices SET snmp_version=?, community=?, security_name=?, auth_protocol=?, auth_passphrase=?, priv_protocol=?, priv_passphrase=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, d.getSnmpVersion());
            pstmt.setString(2, d.getCommunity());
            pstmt.setString(3, d.getSecurityName());
            pstmt.setString(4, d.getAuthProtocol());
            pstmt.setString(5, d.getAuthPassphrase());
            pstmt.setString(6, d.getPrivProtocol());
            pstmt.setString(7, d.getPrivPassphrase());
            pstmt.setInt(8, d.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
