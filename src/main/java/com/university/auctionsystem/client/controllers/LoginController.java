package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.protocol.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private ClientNetworkHandler networkHandler;

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        errorLabel.setText("");
    }

    @FXML
    private void handleLoginButtonAction() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password cannot be empty.");
            return;
        }
        errorLabel.setText("Attempting login...");


        User loginCredentials = new User();
        loginCredentials.setUsername(username);
        loginCredentials.setPasswordHash(password);

        Request loginRequest = new Request(RequestType.LOGIN_USER, loginCredentials);


        networkHandler.sendRequestAsync(loginRequest).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    User loggedInUser = (User) response.getData();
                    ClientApp.getInstance().setCurrentUser(loggedInUser);
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Login Success", "Welcome, " + loggedInUser.getUsername() + "!");
                    ClientApp.getInstance().showMainDashboard();
                } else {
                    errorLabel.setText(response != null ? response.getMessage() : "Login failed. No response from server.");
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                errorLabel.setText("Login error: " + ex.getMessage());
                ex.printStackTrace();
            });
            return null;
        });
    }

    @FXML
    private void handleRegisterLinkAction() {
        ClientApp.getInstance().showRegisterView();
    }
}