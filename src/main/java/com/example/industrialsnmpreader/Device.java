package com.example.industrialsnmpreader;

import javafx.beans.property.*;

public class Device {
    private final IntegerProperty id;
    private final StringProperty ipAddress;
    private final StringProperty oid;
    private final StringProperty temperature;
    private final StringProperty lastUpdate;

    public Device(int id, String ipAddress, String oid) {
        this.id = new SimpleIntegerProperty(id);
        this.ipAddress = new SimpleStringProperty(ipAddress);
        this.oid = new SimpleStringProperty(oid);
        this.temperature = new SimpleStringProperty("Oczekiwanie...");
        this.lastUpdate = new SimpleStringProperty("-");
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty ipAddressProperty() { return ipAddress; }
    public StringProperty oidProperty() { return oid; }
    public StringProperty temperatureProperty() { return temperature; }
    public StringProperty lastUpdateProperty() { return lastUpdate; }

    public String getIpAddress() { return ipAddress.get(); }
    public String getOid() { return oid.get(); }

    public void setTemperature(String temp) { this.temperature.set(temp); }
    public void setLastUpdate(String time) { this.lastUpdate.set(time); }
}