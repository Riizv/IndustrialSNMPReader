package com.example.industrialsnmpreader;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:devices.db";

    static {
        try (Connection conn = DriverManager.getConnection(URL)) {
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

    public static List<Device> getAllDevices() {
        List<Device> devices = new ArrayList<>();
        String sql = "SELECT id, ip, vendor FROM devices";
        try (Connection conn = DriverManager.getConnection(URL);
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
        try (Connection conn = DriverManager.getConnection(URL);
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
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}