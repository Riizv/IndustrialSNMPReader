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
    @FXML private TableColumn<Device, String> colOid;
    @FXML private TableColumn<Device, String> colTemp;
    @FXML private TableColumn<Device, String> colUpdate;

    @FXML private TextField ipField;
    @FXML private TextField oidField;
    @FXML private Label statusLabel;

    private final ObservableList<Device> deviceList = FXCollections.observableArrayList();
    private int nextId = 1;
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(d -> d.getValue().idProperty().asObject());
        colIp.setCellValueFactory(d -> d.getValue().ipAddressProperty());
        colOid.setCellValueFactory(d -> d.getValue().oidProperty());
        colTemp.setCellValueFactory(d -> d.getValue().temperatureProperty());
        colUpdate.setCellValueFactory(d -> d.getValue().lastUpdateProperty());

        deviceTable.setItems(deviceList);

        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(60), e -> refreshAllDevices()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    @FXML
    protected void onAddDevice() {
        if (!ipField.getText().isEmpty() && !oidField.getText().isEmpty()) {
            Device newDev = new Device(nextId++, ipField.getText(), oidField.getText());
            deviceList.add(newDev);
            updateSingleDevice(newDev);
            ipField.clear();
            oidField.clear();
            statusLabel.setText("Dodano urządzenie.");
        }
    }

    private void refreshAllDevices() {
        deviceList.forEach(this::updateSingleDevice);
    }

    private void updateSingleDevice(Device device) {
        Task<String> task = new Task<>() {
            @Override protected String call() {
                return SNMPController.getSnmpValue(device.getIpAddress(), "public", device.getOid());
            }
        };

        task.setOnSucceeded(e -> {
            String res = task.getValue();
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            if (res != null && !res.contains("Err") && !res.equals("Błędny OID")) {
                try {
                    // MikroTik hAP ax3: wartość / 10
                    double t = Double.parseDouble(res) / 10.0;
                    device.setTemperature(t + " °C");
                } catch (Exception ex) {
                    device.setTemperature(res);
                }
            } else {
                device.setTemperature(res);
            }
            device.setLastUpdate(time);
        });
        new Thread(task).start();
    }

    @FXML
    protected void onDeleteDevice() {
        Device selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected != null) { deviceList.remove(selected); }
    }

    @FXML
    protected void onLogoutClick() throws IOException {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        Stage stage = (Stage) deviceTable.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(loader.load(), 600, 400));
    }
}