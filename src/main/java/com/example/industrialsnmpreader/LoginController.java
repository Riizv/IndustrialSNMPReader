package com.example.industrialsnmpreader;

import javafx.application.Platform;
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
    @FXML private Button loginButton;

    @FXML
    private void initialize() {
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    protected void onLoginClick() {
        String user = usernameField.getText();
        String pass = passwordField.getText();

        loginButton.setDisable(true);
        errorLabel.setText("");

        Thread thread = new Thread(() -> {
            boolean ok = DatabaseManager.checkCredentials(user, pass);
            Platform.runLater(() -> {
                if (ok) {
                    try {
                        switchToMainScene();
                    } catch (IOException e) {
                        errorLabel.setText("Błąd ładowania widoku.");
                        loginButton.setDisable(false);
                    }
                } else {
                    errorLabel.setText("Błędny użytkownik lub hasło!");
                    loginButton.setDisable(false);
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void switchToMainScene() throws IOException {
        Stage stage = (Stage) usernameField.getScene().getWindow();
        FXMLLoader fxmlLoader = new FXMLLoader(IndustrialSNMPApplication.class.getResource("INS-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1024, 600);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}