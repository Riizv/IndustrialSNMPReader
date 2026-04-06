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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

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



    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private final ObservableList<Device> deviceList = FXCollections.observableArrayList();
    private Timeline autoRefreshTimeline;
    private final ExecutorService pollExecutor = Executors.newFixedThreadPool(10, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

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

        deviceTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.setOnCloseRequest(e -> shutdown());
                    }
                });
            }
        });

        refreshAllDevices();
    }

    private void shutdown() {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        pollExecutor.shutdownNow();
    }

    private void loadDevicesFromDb() {
        deviceList.setAll(DatabaseManager.getAllDevices());
    }

    @FXML
    protected void onRefresh() {
        refreshAllDevices();
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
                    device.addHistoryPoint(
                        device.lastUpdateProperty().get(),
                        Device.parseMetricValue(device.getCpuUsage()),
                        Device.parseMetricValue(device.getTemperature())
                    );
                });
                return null;
            }
        };
        pollExecutor.submit(task);
    }

    @FXML
    protected void onEditSnmpSettings() {
        Device selected = deviceTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Wybierz urządzenie z tabeli.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("snmp-settings-view.fxml"));
            DialogPane dialogPane = new DialogPane();
            dialogPane.setContent(loader.load());
            dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialogPane.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            dialogPane.getStyleClass().add("root");

            SNMPSettingsController controller = loader.getController();
            controller.setDevice(selected);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Ustawienia SNMP - " + selected.getIpAddress());
            dialog.setDialogPane(dialogPane);

            dialogPane.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.COMMA && e.isShortcutDown()) {
                    dialog.close();
                }
            });

            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    controller.saveSettings();
                    DatabaseManager.updateDeviceSnmpSettings(selected);
                    updateSingleDevice(selected);
                    statusLabel.setText("Ustawienia SNMP zaktualizowane.");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Błąd ładowania okna ustawień.");
        }
    }

    @FXML
    protected void onAddDevice() {
        String ip = ipField.getText();
        String vendor = vendorChoiceBox.getValue();

        if (ip != null && !ip.isEmpty() && vendor != null) {
            if (!IP_PATTERN.matcher(ip).matches()) {
                statusLabel.setText("Nieprawidłowy adres IP.");
                return;
            }
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

    private Stage chartStage;

    @FXML
    protected void onShowChart() {
        if (chartStage != null && chartStage.isShowing()) {
            chartStage.close();
            chartStage = null;
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("chart-view.fxml"));
            chartStage = new Stage();
            chartStage.setTitle("Globalny Monitor Wydajności");
            chartStage.setScene(new Scene(loader.load()));

            ChartController controller = loader.getController();
            controller.setDevices(deviceList);

            chartStage.setOnCloseRequest(e -> chartStage = null);
            chartStage.getScene().setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.G && e.isShortcutDown()) {
                    chartStage.close();
                    chartStage = null;
                }
            });
            chartStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("Błąd otwierania wykresu.");
        }
    }

    @FXML
    protected void onLogoutClick() throws IOException {
        if (autoRefreshTimeline != null) autoRefreshTimeline.stop();
        pollExecutor.shutdownNow();
        Stage stage = (Stage) deviceTable.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(loader.load(), 600, 400));
    }
}