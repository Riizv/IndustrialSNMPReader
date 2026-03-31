package com.example.industrialsnmpreader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class IndustrialSNMPController {

    @FXML private TableView<Device> deviceTable;
    @FXML private TableColumn<Device, Integer> colId;
    @FXML private TableColumn<Device, String> colIp;
    @FXML private TableColumn<Device, String> colHostname;
    @FXML private TableColumn<Device, String> colVendor;
    @FXML private TableColumn<Device, String> colTemp;
    @FXML private TableColumn<Device, String> colUptime;
    @FXML private TableColumn<Device, String> colCpu;
    @FXML private TableColumn<Device, String> colUpdate;

    @FXML private TextField ipField;
    @FXML private ChoiceBox<String> vendorChoiceBox;
    @FXML private Label statusLabel;

    private final ObservableList<Device> deviceList = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> d.getValue().idProperty().asObject());
        colIp.setCellValueFactory(d -> d.getValue().ipAddressProperty());
        colHostname.setCellValueFactory(d -> d.getValue().hostnameProperty());
        colVendor.setCellValueFactory(d -> d.getValue().vendorProperty());
        colTemp.setCellValueFactory(d -> d.getValue().temperatureProperty());
        colUptime.setCellValueFactory(d -> d.getValue().uptimeProperty());
        colCpu.setCellValueFactory(d -> d.getValue().cpuUsageProperty());
        colUpdate.setCellValueFactory(d -> d.getValue().lastUpdateProperty());

        vendorChoiceBox.setItems(FXCollections.observableArrayList("Siemens", "Mikrotik"));
        vendorChoiceBox.getSelectionModel().selectFirst();

        loadDevicesFromDb();
        deviceTable.setItems(deviceList);

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshAllDevices()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
        
        refreshAllDevices();
    }

    private void loadDevicesFromDb() {
        deviceList.setAll(DatabaseManager.getAllDevices());
    }

    @FXML
    protected void onAddDevice() {
        String ip = ipField.getText();
        String vendor = vendorChoiceBox.getValue();
        
        if (ip != null && !ip.isEmpty() && vendor != null) {
            int id = DatabaseManager.addDevice(ip, vendor);
            if (id != -1) {
                Device newDev = new Device(id, ip, vendor);
                deviceList.add(newDev);
                updateSingleDevice(newDev);
                ipField.clear();
                statusLabel.setText("Dodano urządzenie (ID: " + id + ").");
            } else {
                statusLabel.setText("Błąd dodawania do bazy.");
            }
        }
    }

    private void refreshAllDevices() {
        deviceList.forEach(this::updateSingleDevice);
    }

    private void updateSingleDevice(Device device) {
        SNMPController.VendorOids oids = SNMPController.getOidsForVendor(device.getVendor());
        if (oids == null) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                String hostname = SNMPController.getSnmpValue(device.getIpAddress(), "public", oids.hostnameOid);
                String temp = SNMPController.getSnmpValue(device.getIpAddress(), "public", oids.tempOid);
                String uptime = SNMPController.getSnmpValue(device.getIpAddress(), "public", oids.uptimeOid);
                String cpu = SNMPController.getSnmpValue(device.getIpAddress(), "public", oids.cpuOid);

                javafx.application.Platform.runLater(() -> {
                    // Procesowanie Hostname
                    device.setHostname(hostname);

                    // Procesowanie temperatury - MikroTik (1.3.6.1.4.1.14988.1.1.3.10.0) wymaga / 10
                    if (temp != null && !temp.contains("Err") && !temp.equals("Brak OID") && !temp.equals("Błędny OID")) {
                        if (device.getVendor().equalsIgnoreCase("Mikrotik")) {
                            try {
                                double t = Double.parseDouble(temp) / 10.0;
                                device.setTemperature(t + " °C");
                            } catch (Exception ex) { device.setTemperature(temp); }
                        } else { device.setTemperature(temp + " °C"); }
                    } else { device.setTemperature(temp); }

                    // Procesowanie Uptime
                    device.setUptime(uptime);

                    // Procesowanie CPU - Wartość bezpośrednia w %
                    if (cpu != null && !cpu.contains("Err") && !cpu.equals("Brak OID") && !cpu.equals("Błędny OID")) {
                        device.setCpuUsage(cpu + " %");
                    } else { device.setCpuUsage(cpu); }

                    device.setLastUpdate(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    @FXML
    protected void onDeleteDevice() {
        Device selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            DatabaseManager.deleteDevice(selected.getId());
            deviceList.remove(selected);
            statusLabel.setText("Usunięto urządzenie.");
        }
    }

    @FXML
    protected void onLogoutClick() throws IOException {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        Stage stage = (Stage) deviceTable.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(loader.load(), 600, 400));
    }
}