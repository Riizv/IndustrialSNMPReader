package com.example.industrialsnmpreader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
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
    @FXML private ComboBox<String> vendorChoiceBox;
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


        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(10), e -> refreshAllDevices()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        refreshAllDevices();
    }

    private void loadDevicesFromDb() {
        deviceList.setAll(DatabaseManager.getAllDevices());
    }

    private void refreshAllDevices() {
        deviceList.forEach(this::updateSingleDevice);
    }

    private void updateSingleDevice(Device device) {
        SNMPController.VendorOids oids = SNMPController.getOidsForVendor(device.getVendor());
        if (oids == null) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() {
                String hostname = SNMPController.getSnmpValue(device, oids.hostnameOid);
                String temp = SNMPController.getSnmpValue(device, oids.tempOid);
                String uptime = SNMPController.getSnmpValue(device, oids.uptimeOid);
                String cpu = SNMPController.getSnmpValue(device, oids.cpuOid);

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
                    // Option without checking errors - if (Integer.parseInt(cpu) >= 0)
                    if(cpu != null && !cpu.contains("Err") && !cpu.equals("Brak OID") && !cpu.equals("Błędny OID"))
                    {
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
    protected void onEditSnmpSettings() {
        Device selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Wybierz urządzenie z tabeli.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ustawienia SNMP - " + selected.getIpAddress());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        ComboBox<String> versionBox = new ComboBox<>(FXCollections.observableArrayList("v1", "v2c", "v3"));
        int currentVer = selected.getSnmpVersion();
        versionBox.getSelectionModel().select(currentVer == 0 ? 0 : (currentVer == 3 ? 2 : 1));

        TextField communityField = new TextField(selected.getCommunity());
        TextField securityNameField = new TextField(selected.getSecurityName());
        ComboBox<String> authProtBox = new ComboBox<>(FXCollections.observableArrayList("NONE", "MD5", "SHA"));
        authProtBox.getSelectionModel().select(selected.getAuthProtocol());
        PasswordField authPassField = new PasswordField();
        authPassField.setText(selected.getAuthPassphrase());
        ComboBox<String> privProtBox = new ComboBox<>(FXCollections.observableArrayList("NONE", "DES", "AES128", "AES192", "AES256"));
        privProtBox.getSelectionModel().select(selected.getPrivProtocol());
        PasswordField privPassField = new PasswordField();
        privPassField.setText(selected.getPrivPassphrase());

        grid.add(new Label("Wersja SNMP:"), 0, 0);
        grid.add(versionBox, 1, 0);
        grid.add(new Label("Community (v1/v2c):"), 0, 1);
        grid.add(communityField, 1, 1);
        grid.add(new Label("Security Name (v3):"), 0, 2);
        grid.add(securityNameField, 1, 2);
        grid.add(new Label("Auth Protocol (v3):"), 0, 3);
        grid.add(authProtBox, 1, 3);
        grid.add(new Label("Auth Passphrase (v3):"), 0, 4);
        grid.add(authPassField, 1, 4);
        grid.add(new Label("Priv Protocol (v3):"), 0, 5);
        grid.add(privProtBox, 1, 5);
        grid.add(new Label("Priv Passphrase (v3):"), 0, 6);
        grid.add(privPassField, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int version = versionBox.getSelectionModel().getSelectedIndex();
                if (version == 2) version = 3; // v3 index is 2 in box, but value is 3

                selected.setSnmpSettings(
                    version,
                    communityField.getText(),
                    securityNameField.getText(),
                    authProtBox.getValue(),
                    authPassField.getText(),
                    privProtBox.getValue(),
                    privPassField.getText()
                );
                DatabaseManager.updateDeviceSnmpSettings(selected);
                updateSingleDevice(selected);
                statusLabel.setText("Ustawienia SNMP zaktualizowane.");
            }
        });
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
    protected void onShowChart() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chart-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Globalny Monitor Wydajności");
            stage.setScene(new Scene(loader.load()));

            ChartController controller = loader.getController();
            // Przekazujemy całą listę urządzeń
            controller.setDevices(deviceList);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Błąd otwierania wykresu.");
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