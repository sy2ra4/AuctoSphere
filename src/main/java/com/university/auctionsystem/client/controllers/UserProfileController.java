package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.client.utils.UIUtils;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class UserProfileController {
    @FXML private Label usernameLabel;
    @FXML private TextField emailField;
    @FXML private Label roleLabel;
    @FXML private Label memberSinceLabel;
    @FXML private Label statusLabel;

    private ClientNetworkHandler networkHandler;
    private User currentUserProfile;
    private final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("MMM dd, yyyy HH:mm");

    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        dateTimeFormatter.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
        statusLabel.setText("");
    }

    public void loadUserProfile() {
        User loggedInUser = ClientApp.getInstance().getCurrentUser();
        if (loggedInUser == null) {
            statusLabel.setText("Not logged in.");
            return;
        }

        Request request = new Request(RequestType.GET_USER_PROFILE, null);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess() && response.getData() instanceof User) {
                    currentUserProfile = (User) response.getData();
                    displayProfile(currentUserProfile);
                } else {
                    statusLabel.setText("Failed to load profile: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("Error loading profile: " + ex.getMessage()));
            return null;
        });
    }

    private void displayProfile(User user) {
        if (user == null) return;
        usernameLabel.setText(user.getUsername());
        emailField.setText(user.getEmail());
        roleLabel.setText(user.getRole().toString());
        if (user.getCreatedAt() != null) {
            memberSinceLabel.setText(dateTimeFormatter.format(user.getCreatedAt()));
        } else {
            memberSinceLabel.setText("N/A");
        }
    }

    @FXML
    private void handleUpdateEmailAction() {
        if (currentUserProfile == null) {
            statusLabel.setText("Profile not loaded.");
            return;
        }
        String newEmail = emailField.getText().trim();
        if (newEmail.isEmpty() || newEmail.equals(currentUserProfile.getEmail())) {
            statusLabel.setText("No changes to email or email is empty.");
            return;
        }
        if (!newEmail.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            statusLabel.setText("Invalid email format.");
            return;
        }

        statusLabel.setText("Updating email...");
        Request request = new Request(RequestType.UPDATE_USER_PROFILE, newEmail);
        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    statusLabel.setText("Email updated successfully!");
                    currentUserProfile.setEmail(newEmail);
                    ClientApp.getInstance().getCurrentUser().setEmail(newEmail);
                    UIUtils.showAlert(Alert.AlertType.INFORMATION, "Success", "Email updated.");
                } else {
                    statusLabel.setText("Failed to update email: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> statusLabel.setText("Error updating email: " + ex.getMessage()));
            return null;
        });
    }
}