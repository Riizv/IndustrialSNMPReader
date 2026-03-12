package com.example.industrialsnmpreader;

import javafx.beans.property.*;

public class User { //TODO: zmienić user na coś innego
    private final IntegerProperty id;
    private final StringProperty ip;
    private final StringProperty temp;

    public User(int id, String ip, String temp) {
        this.id = new SimpleIntegerProperty(id);
        this.ip = new SimpleStringProperty(ip);
        this.temp = new SimpleStringProperty(temp);
    }

    public int getId() { return id.get(); }
    public String getIP() { return ip.get(); }
    public String getTemp() { return temp.get(); }

    public void setName(String ip) { this.ip.set(ip); }
    public void setEmail(String temp) { this.temp.set(temp); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return ip; }
    public StringProperty emailProperty() { return temp; }
}