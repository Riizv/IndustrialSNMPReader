package com.example.industrialsnmpreader;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import java.util.HashMap;
import java.util.Map;

public class ChartController {

    @FXML private LineChart<String, Number> cpuChart;
    @FXML private LineChart<String, Number> tempChart;

    private final Map<Device, XYChart.Series<String, Number>> cpuSeriesMap = new HashMap<>();
    private final Map<Device, XYChart.Series<String, Number>> tempSeriesMap = new HashMap<>();
    private final Map<Device, String> lastProcessedTimes = new HashMap<>();

    public void setDevices(ObservableList<Device> devices) {
        // Dodaj początkowe urządzenia
        devices.forEach(this::addDeviceSeries);

        // Nasłuchuj zmian w liście urządzeń (dodawanie/usuwanie)
        devices.addListener((ListChangeListener<Device>) c -> {
            while (c.next()) {
                if (c.wasAdded()) c.getAddedSubList().forEach(this::addDeviceSeries);
                if (c.wasRemoved()) c.getRemoved().forEach(this::removeDeviceSeries);
            }
        });
    }

    private void addDeviceSeries(Device device) {
        XYChart.Series<String, Number> cpuSeries = new XYChart.Series<>();
        cpuSeries.setName(device.getIpAddress() + " (" + device.getVendor() + ")");
        cpuSeriesMap.put(device, cpuSeries);
        Platform.runLater(() -> cpuChart.getData().add(cpuSeries));

        XYChart.Series<String, Number> tempSeries = new XYChart.Series<>();
        tempSeries.setName(device.getIpAddress() + " (" + device.getVendor() + ")");
        tempSeriesMap.put(device, tempSeries);
        Platform.runLater(() -> tempChart.getData().add(tempSeries));

        // Nasłuchuj zmian danych dla tego urządzenia
        device.lastUpdateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals("-") && !newVal.equals(lastProcessedTimes.get(device))) {
                updateDeviceData(device, newVal);
            }
        });

        // Jeśli są już dane, dodaj je od razu
        if (!device.lastUpdateProperty().get().equals("-")) {
            updateDeviceData(device, device.lastUpdateProperty().get());
        }
    }

    private void removeDeviceSeries(Device device) {
        XYChart.Series<String, Number> cpuSeries = cpuSeriesMap.remove(device);
        if (cpuSeries != null) Platform.runLater(() -> cpuChart.getData().remove(cpuSeries));

        XYChart.Series<String, Number> tempSeries = tempSeriesMap.remove(device);
        if (tempSeries != null) Platform.runLater(() -> tempChart.getData().remove(tempSeries));
        
        lastProcessedTimes.remove(device);
    }

    private void updateDeviceData(Device device, String time) {
        lastProcessedTimes.put(device, time);
        
        Platform.runLater(() -> {
            XYChart.Series<String, Number> cpuSeries = cpuSeriesMap.get(device);
            XYChart.Series<String, Number> tempSeries = tempSeriesMap.get(device);

            if (cpuSeries == null || tempSeries == null) return;

            double cpuVal = parseValue(device.getCpuUsage());
            double tempVal = parseValue(device.getTemperature());

            // Limitowanie do 30 punktów na urządzenie
            if (cpuSeries.getData().size() > 30) {
                cpuSeries.getData().remove(0);
                tempSeries.getData().remove(0);
            }

            // Dodanie nowych danych z Tooltipami
            XYChart.Data<String, Number> cpuData = new XYChart.Data<>(time, cpuVal);
            XYChart.Data<String, Number> tempData = new XYChart.Data<>(time, tempVal);

            cpuSeries.getData().add(cpuData);
            tempSeries.getData().add(tempData);

            // Dodanie Tooltipa po dodaniu punktu do wykresu (musi być w scenie)
            addTooltip(cpuData, cpuVal, "%");
            addTooltip(tempData, tempVal, "°C");
        });
    }

    private void addTooltip(XYChart.Data<String, Number> data, double value, String unit) {
        if (data.getNode() == null) {
            // Czekamy aż węzeł zostanie utworzony (JavaFX tworzy go asynchronicznie)
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    installTooltip(newNode, value, unit);
                }
            });
        } else {
            installTooltip(data.getNode(), value, unit);
        }
    }

    private void installTooltip(javafx.scene.Node node, double value, String unit) {
        Tooltip tooltip = new Tooltip(value + " " + unit);
        tooltip.getStyleClass().add("chart-tooltip");
        tooltip.setShowDelay(javafx.util.Duration.ZERO);
        Tooltip.install(node, tooltip);
    }

    private double parseValue(String val) {
        if (val == null || val.equals("-") || val.contains("Err") || val.isEmpty()) return 0;
        try {
            String clean = val.replaceAll("[^0-9.]", "");
            if (clean.isEmpty()) return 0;
            return Double.parseDouble(clean);
        } catch (Exception e) {
            return 0;
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) cpuChart.getScene().getWindow();
        stage.close();
    }
}
