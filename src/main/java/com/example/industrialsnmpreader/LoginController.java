package com.example.industrialsnmpreader;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    @FXML
    protected void onLoginClick() throws IOException {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        // Prosta walidacja (w prawdziwej apce użyłbyś bazy danych)
        if ("admin".equals(user) && "admin123".equals(pass)) {
            switchToMainScene();
        } else {
            errorLabel.setText("Błędny użytkownik lub hasło!");
        }
    }

    private void switchToMainScene() throws IOException {
        // Pobieramy aktualne okno (Stage)
        Stage stage = (Stage) usernameField.getScene().getWindow();

        // Ładujemy Twój poprzedni widok CRUD
        FXMLLoader fxmlLoader = new FXMLLoader(IndustrialSNMPApplication.class.getResource("INS-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 600);

        stage.setScene(scene);
        stage.centerOnScreen();
    }
}