package com.university.auctionsystem.client.utils;

import javafx.scene.control.Alert;
import javafx.application.Platform;

public class UIUtils {

    public static void showAlert(Alert.AlertType alertType, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}