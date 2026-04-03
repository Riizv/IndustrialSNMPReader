package com.example.industrialsnmpreader;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SNMPSettingsController {

    @FXML private ComboBox<String> versionBox;
    @FXML private TextField communityField;
    @FXML private TextField securityNameField;
    @FXML private ComboBox<String> authProtBox;
    @FXML private PasswordField authPassField;
    @FXML private ComboBox<String> privProtBox;
    @FXML private PasswordField privPassField;

    private Device device;

    @FXML
    public void initialize() {
        versionBox.setItems(FXCollections.observableArrayList("v1", "v2c", "v3"));
        authProtBox.setItems(FXCollections.observableArrayList("NONE", "MD5", "SHA"));
        privProtBox.setItems(FXCollections.observableArrayList("NONE", "DES", "AES128", "AES192", "AES256"));
    }

    public void setDevice(Device device) {
        this.device = device;
        
        int currentVer = device.getSnmpVersion();
        versionBox.getSelectionModel().select(currentVer == 0 ? 0 : (currentVer == 3 ? 2 : 1));
        
        communityField.setText(device.getCommunity());
        securityNameField.setText(device.getSecurityName());
        authProtBox.getSelectionModel().select(device.getAuthProtocol());
        authPassField.setText(device.getAuthPassphrase());
        privProtBox.getSelectionModel().select(device.getPrivProtocol());
        privPassField.setText(device.getPrivPassphrase());
    }

    public void saveSettings() {
        int version = versionBox.getSelectionModel().getSelectedIndex();
        if (version == 2) version = 3; // v3 index is 2 in box, but value is 3

        device.setSnmpSettings(
            version,
            communityField.getText(),
            securityNameField.getText(),
            authProtBox.getValue(),
            authPassField.getText(),
            privProtBox.getValue(),
            privPassField.getText()
        );
    }
}
