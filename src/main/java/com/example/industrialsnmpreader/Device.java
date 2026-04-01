package com.example.industrialsnmpreader;

import javafx.beans.property.*;

public class Device {
    private final IntegerProperty id;
    private final StringProperty ipAddress;
    private final StringProperty hostname;
    private final StringProperty vendor;
    private final StringProperty temperature;
    private final StringProperty uptime;
    private final StringProperty cpuUsage;
    private final StringProperty lastUpdate;

    // SNMP Settings
    private final IntegerProperty snmpVersion; // 0=v1, 1=v2c, 3=v3
    private final StringProperty community;    // dla v1/v2c
    private final StringProperty securityName; // dla v3
    private final StringProperty authProtocol; // MD5, SHA, NONE
    private final StringProperty authPassphrase;
    private final StringProperty privProtocol; // DES, AES, NONE
    private final StringProperty privPassphrase;

    public Device(int id, String ipAddress, String vendor) {
        this.id = new SimpleIntegerProperty(id);
        this.ipAddress = new SimpleStringProperty(ipAddress);
        this.hostname = new SimpleStringProperty("-");
        this.vendor = new SimpleStringProperty(vendor);
        this.temperature = new SimpleStringProperty("-");
        this.uptime = new SimpleStringProperty("-");
        this.cpuUsage = new SimpleStringProperty("-");
        this.lastUpdate = new SimpleStringProperty("-");

        // Domyślne wartości SNMP v2c
        this.snmpVersion = new SimpleIntegerProperty(1); // v2c
        this.community = new SimpleStringProperty("public");
        this.securityName = new SimpleStringProperty("");
        this.authProtocol = new SimpleStringProperty("NONE");
        this.authPassphrase = new SimpleStringProperty("");
        this.privProtocol = new SimpleStringProperty("NONE");
        this.privPassphrase = new SimpleStringProperty("");
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty ipAddressProperty() { return ipAddress; }
    public StringProperty hostnameProperty() { return hostname; }
    public StringProperty vendorProperty() { return vendor; }
    public StringProperty temperatureProperty() { return temperature; }
    public StringProperty uptimeProperty() { return uptime; }
    public StringProperty cpuUsageProperty() { return cpuUsage; }
    public StringProperty lastUpdateProperty() { return lastUpdate; }

    public IntegerProperty snmpVersionProperty() { return snmpVersion; }
    public StringProperty communityProperty() { return community; }
    public StringProperty securityNameProperty() { return securityName; }
    public StringProperty authProtocolProperty() { return authProtocol; }
    public StringProperty authPassphraseProperty() { return authPassphrase; }
    public StringProperty privProtocolProperty() { return privProtocol; }
    public StringProperty privPassphraseProperty() { return privPassphrase; }

    public int getId() { return id.get(); }
    public String getIpAddress() { return ipAddress.get(); }
    public String getVendor() { return vendor.get(); }
    public String getCpuUsage() { return cpuUsage.get(); }
    public String getTemperature() { return temperature.get(); }

    public int getSnmpVersion() { return snmpVersion.get(); }
    public String getCommunity() { return community.get(); }
    public String getSecurityName() { return securityName.get(); }
    public String getAuthProtocol() { return authProtocol.get(); }
    public String getAuthPassphrase() { return authPassphrase.get(); }
    public String getPrivProtocol() { return privProtocol.get(); }
    public String getPrivPassphrase() { return privPassphrase.get(); }

    public void setHostname(String host) { this.hostname.set(host); }
    public void setTemperature(String temp) { this.temperature.set(temp); }
    public void setUptime(String up) { this.uptime.set(up); }
    public void setCpuUsage(String cpu) { this.cpuUsage.set(cpu); }
    public void setLastUpdate(String time) { this.lastUpdate.set(time); }

    public void setSnmpSettings(int version, String comm, String user, String aProt, String aPass, String pProt, String pPass) {
        this.snmpVersion.set(version);
        this.community.set(comm);
        this.securityName.set(user);
        this.authProtocol.set(aProt);
        this.authPassphrase.set(aPass);
        this.privProtocol.set(pProt);
        this.privPassphrase.set(pPass);
    }
}
