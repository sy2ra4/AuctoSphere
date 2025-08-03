package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.Role;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.protocol.Response;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField emailField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private Label messageLabel;

    private ClientNetworkHandler networkHandler;

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        roleComboBox.setItems(FXCollections.observableArrayList(Role.BUYER, Role.SELLER));
        roleComboBox.setValue(Role.BUYER);
        messageLabel.setText("");
    }

    @FXML
    private void handleRegisterButtonAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String email = emailField.getText();
        Role role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || role == null) {
            messageLabel.setText("All fields are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            messageLabel.setText("Invalid email format.");
            return;
        }
        messageLabel.setText("Registering...");

        User newUser = new User(username, password, email, role);
        Request registerRequest = new Request(RequestType.REGISTER_USER, newUser);

        networkHandler.sendRequestAsync(registerRequest).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    UIUtils.showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Registration Success", "User registered successfully! Please login.");
                    ClientApp.getInstance().showLoginView();
                } else {
                    messageLabel.setText(response != null ? response.getMessage() : "Registration failed.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                messageLabel.setText("Registration error: " + ex.getMessage());
                ex.printStackTrace();
            });
            return null;
        });
    }

    @FXML
    private void handleBackButtonAction() {
        ClientApp.getInstance().showLoginView();
    }
}