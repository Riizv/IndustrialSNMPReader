package com.example.industrialsnmpreader;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChartController {

    @FXML private LineChart<String, Number> cpuChart;
    @FXML private LineChart<String, Number> tempChart;
    @FXML private ComboBox<Device> deviceFilter;

    private final Map<Device, XYChart.Series<String, Number>> cpuSeriesMap = new HashMap<>();
    private final Map<Device, XYChart.Series<String, Number>> tempSeriesMap = new HashMap<>();
    private final Map<Device, String> lastProcessedTimes = new HashMap<>();

    public void setDevices(ObservableList<Device> devices) {
        deviceFilter.setConverter(new StringConverter<>() {
            @Override public String toString(Device d) {
                if (d == null) return "Wszystkie urządzenia";
                String host = d.getHostname();
                return d.getIpAddress() + (host != null && !host.equals("-") ? " (" + host + ")" : "");
            }
            @Override public Device fromString(String s) { return null; }
        });
        deviceFilter.getItems().add(null);
        devices.forEach(d -> deviceFilter.getItems().add(d));
        deviceFilter.getSelectionModel().selectFirst();
        deviceFilter.setOnAction(e -> applyFilter());

        devices.forEach(this::addDeviceSeries);

        devices.addListener((ListChangeListener<Device>) c -> {
            while (c.next()) {
                if (c.wasAdded()) c.getAddedSubList().forEach(d -> {
                    deviceFilter.getItems().add(d);
                    addDeviceSeries(d);
                });
                if (c.wasRemoved()) c.getRemoved().forEach(d -> {
                    deviceFilter.getItems().remove(d);
                    removeDeviceSeries(d);
                });
            }
        });
    }

    private void applyFilter() {
        Device selected = deviceFilter.getValue();
        cpuSeriesMap.forEach((device, series) -> setSeriesVisible(series, selected == null || device == selected));
        tempSeriesMap.forEach((device, series) -> setSeriesVisible(series, selected == null || device == selected));
    }

    private void setSeriesVisible(XYChart.Series<String, Number> series, boolean visible) {
        if (series.getNode() != null) series.getNode().setVisible(visible);
        series.getData().forEach(d -> { if (d.getNode() != null) d.getNode().setVisible(visible); });
    }

    private void addDeviceSeries(Device device) {
        XYChart.Series<String, Number> cpuSeries = new XYChart.Series<>();
        cpuSeries.setName(device.getIpAddress() + " (" + device.getHostname() + ")");
        cpuSeriesMap.put(device, cpuSeries);

        XYChart.Series<String, Number> tempSeries = new XYChart.Series<>();
        tempSeries.setName(device.getIpAddress() + " (" + device.getHostname() + ")");
        tempSeriesMap.put(device, tempSeries);

        // Wypełnij serię istniejącą historią zebraną przed otwarciem wykresu
        List<Device.DataPoint> history = device.getHistory();
        List<XYChart.Data<String, Number>> cpuHistData = new ArrayList<>();
        List<XYChart.Data<String, Number>> tempHistData = new ArrayList<>();
        for (Device.DataPoint p : history) {
            XYChart.Data<String, Number> cpuD = new XYChart.Data<>(p.time(), p.cpu());
            XYChart.Data<String, Number> tempD = new XYChart.Data<>(p.time(), p.temperature());
            cpuSeries.getData().add(cpuD);
            tempSeries.getData().add(tempD);
            cpuHistData.add(cpuD);
            tempHistData.add(tempD);
        }
        if (!history.isEmpty()) {
            lastProcessedTimes.put(device, history.get(history.size() - 1).time());
        }

        Platform.runLater(() -> {
            cpuChart.getData().add(cpuSeries);
            tempChart.getData().add(tempSeries);
            // Tooltips dla historycznych punktów — dodajemy po dodaniu serii do wykresu
            cpuHistData.forEach(d -> addTooltip(d, d.getYValue().doubleValue(), "%"));
            tempHistData.forEach(d -> addTooltip(d, d.getYValue().doubleValue(), "°C"));
        });

        // Nasłuchuj nowych punktów
        device.lastUpdateProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals("-") && !newVal.equals(lastProcessedTimes.get(device))) {
                updateDeviceData(device, newVal);
            }
        });
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

            double cpuVal = Device.parseMetricValue(device.getCpuUsage());
            double tempVal = Device.parseMetricValue(device.getTemperature());

            if (cpuSeries.getData().size() >= 500) {
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

    @FXML
    private void onClose() {
        Stage stage = (Stage) cpuChart.getScene().getWindow();
        stage.close();
    }
}
