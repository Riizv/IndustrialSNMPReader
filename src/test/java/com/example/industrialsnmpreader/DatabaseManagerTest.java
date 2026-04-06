package com.example.industrialsnmpreader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {

    private File tempDb;

    @BeforeEach
    void setUp() throws IOException {
        tempDb = Files.createTempFile("testdb", ".db").toFile();
        DatabaseManager.setDbUrlForTesting("jdbc:sqlite:" + tempDb.getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        if (tempDb != null) tempDb.delete();
    }

    @Test
    void addDevice_returnsPositiveId() {
        int id = DatabaseManager.addDevice("192.168.1.1", "Siemens");
        assertTrue(id > 0);
    }

    @Test
    void getAllDevices_returnsInsertedDevice() {
        DatabaseManager.addDevice("10.0.0.1", "Mikrotik");
        List<Device> devices = DatabaseManager.getAllDevices();
        assertEquals(1, devices.size());
        assertEquals("10.0.0.1", devices.get(0).getIpAddress());
        assertEquals("Mikrotik", devices.get(0).getVendor());
    }

    @Test
    void updateDeviceSnmpSettings_persistsSettings() {
        int id = DatabaseManager.addDevice("192.168.0.1", "Siemens");
        Device d = DatabaseManager.getAllDevices().stream()
                .filter(dev -> dev.getId() == id)
                .findFirst()
                .orElseThrow();

        d.setSnmpSettings(3, "public", "adminUser", "SHA", "authPass", "AES", "privPass");
        DatabaseManager.updateDeviceSnmpSettings(d);

        Device updated = DatabaseManager.getAllDevices().stream()
                .filter(dev -> dev.getId() == id)
                .findFirst()
                .orElseThrow();

        assertEquals(3, updated.getSnmpVersion());
        assertEquals("adminUser", updated.getSecurityName());
        assertEquals("SHA", updated.getAuthProtocol());
        assertEquals("authPass", updated.getAuthPassphrase());
        assertEquals("AES", updated.getPrivProtocol());
        assertEquals("privPass", updated.getPrivPassphrase());
    }

    @Test
    void deleteDevice_removesRow() {
        int id = DatabaseManager.addDevice("172.16.0.1", "Siemens");
        DatabaseManager.deleteDevice(id);
        List<Device> devices = DatabaseManager.getAllDevices();
        assertTrue(devices.stream().noneMatch(d -> d.getId() == id));
    }

    @Test
    void checkCredentials_adminDefaultPassword_returnsTrue() {
        assertTrue(DatabaseManager.checkCredentials("admin", "admin123"));
    }

    @Test
    void checkCredentials_wrongPassword_returnsFalse() {
        assertFalse(DatabaseManager.checkCredentials("admin", "wrongpass"));
    }

    @Test
    void checkCredentials_unknownUser_returnsFalse() {
        assertFalse(DatabaseManager.checkCredentials("nobody", "admin123"));
    }

    @Test
    void checkCredentials_nullInputs_returnsFalse() {
        assertFalse(DatabaseManager.checkCredentials(null, "admin123"));
        assertFalse(DatabaseManager.checkCredentials("admin", null));
    }
}
