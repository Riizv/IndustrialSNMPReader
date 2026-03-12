package com.example.industrialsnmpreader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

import javafx.concurrent.Task;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class IndustrialSNMPController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colId;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private Label statusLabel;

    private final ObservableList<User> userList = FXCollections.observableArrayList();
    private int nextId = 1;
    private Timeline autoRefreshTimeline;

    @FXML
    public void initialize() {
        // Powiązanie kolumn z modelem
        colId.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colEmail.setCellValueFactory(cellData -> cellData.getValue().emailProperty());

        userTable.setItems(userList);

        // Słuchacz wyboru w tabeli
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                nameField.setText(newVal.getIP());
                emailField.setText(newVal.getTemp());
            }
        });


        // Konfiguracja auto-odświeżania co 60 sekund
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(60), event -> {
            System.out.println("Auto-odświeżanie SNMP...");
            refreshTemperature(); // Wywołuje Twoją istniejącą metodę z Taskiem
        }));

        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE); // Powtarzaj w nieskończoność
        autoRefreshTimeline.play(); // Start
    }

    @FXML
    protected void onAddUser() {
        if (!nameField.getText().isEmpty()) {
            userList.add(new User(nextId++, nameField.getText(), emailField.getText()));
            clearFields();
            statusLabel.setText("Dodano użytkownika.");
        }
    }

    @FXML
    protected void onUpdateUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selected.setName(nameField.getText());
            selected.setEmail(emailField.getText());
            userTable.refresh();
            statusLabel.setText("Zaktualizowano dane.");
        }
    }

    @FXML
    protected void onDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Mały bonus: proste potwierdzenie przed usunięciem
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Czy na pewno usunąć " + selected.getIP() + "?", ButtonType.YES, ButtonType.NO);
            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    userList.remove(selected);
                    clearFields();
                    statusLabel.setText("Usunięto użytkownika.");
                }
            });
        }
    }

    @FXML
    protected void onLogoutClick() throws IOException {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
        Stage stage = (Stage) userTable.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
        stage.setScene(new Scene(loader.load(), 600, 400));
        stage.setTitle("Logowanie");
    }

    private void clearFields() {
        nameField.clear();
        emailField.clear();
    }

    @FXML
    protected void refreshTemperature() {
        Task<String> tempTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // Pobieramy dane (przykład dla MikroTika)
                return SNMPController.getSnmpValue("192.168.88.1", "public", "1.3.6.1.4.1.14988.1.1.3.11.0");
            }
        };

        tempTask.setOnSucceeded(e -> {
            String result = tempTask.getValue();
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            try {
                // MikroTik: wynik/10, Hirschmann: wynik
                double temp = Double.parseDouble(result) / 10.0;
                statusLabel.setText("Temp: " + temp + "°C (Updated on: " + time + ")");

                // Logika kolorów
                if (temp > 50) statusLabel.setStyle("-fx-text-fill: red;");
                else statusLabel.setStyle("-fx-text-fill: green;");

            } catch (Exception ex) {
                statusLabel.setText("Data error: " + result + " (" + time + ")");
            }
        });

        new Thread(tempTask).start();
    }

}