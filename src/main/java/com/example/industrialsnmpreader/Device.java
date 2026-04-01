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

    public Device(int id, String ipAddress, String vendor) {
        this.id = new SimpleIntegerProperty(id);
        this.ipAddress = new SimpleStringProperty(ipAddress);
        this.hostname = new SimpleStringProperty("-");
        this.vendor = new SimpleStringProperty(vendor);
        this.temperature = new SimpleStringProperty("-");
        this.uptime = new SimpleStringProperty("-");
        this.cpuUsage = new SimpleStringProperty("-");
        this.lastUpdate = new SimpleStringProperty("-");
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty ipAddressProperty() { return ipAddress; }
    public StringProperty hostnameProperty() { return hostname; }
    public StringProperty vendorProperty() { return vendor; }
    public StringProperty temperatureProperty() { return temperature; }
    public StringProperty uptimeProperty() { return uptime; }
    public StringProperty cpuUsageProperty() { return cpuUsage; }
    public StringProperty lastUpdateProperty() { return lastUpdate; }

    public int getId() { return id.get(); }
    public String getIpAddress() { return ipAddress.get(); }
    public String getVendor() { return vendor.get(); }
    public String getCpuUsage() { return cpuUsage.get(); }
    public String getTemperature() { return temperature.get(); }

    public void setHostname(String host) { this.hostname.set(host); }
    public void setTemperature(String temp) { this.temperature.set(temp); }
    public void setUptime(String up) { this.uptime.set(up); }
    public void setCpuUsage(String cpu) { this.cpuUsage.set(cpu); }
    public void setLastUpdate(String time) { this.lastUpdate.set(time); }
}