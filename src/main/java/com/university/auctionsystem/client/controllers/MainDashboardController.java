package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.shared.model.Role;
import com.university.auctionsystem.shared.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;


public class MainDashboardController {

    @FXML private TabPane mainTabPane;
    @FXML private Tab sellerTab;
    @FXML private Tab buyerTab;
    @FXML private Tab adminTab;
    @FXML private Label welcomeLabel;
    @FXML private Tab profileTab;
    @FXML private Tab messagesTab;


    @FXML private AuctionViewController auctionViewTabController;
    @FXML private SellerDashboardController sellerDashboardTabController;
    @FXML private BuyerDashboardController buyerDashboardTabController;
    @FXML private AdminDashboardController adminDashboardTabController;
    @FXML private UserProfileController userProfileTabController;
    @FXML private MessagesViewController messagesViewTabController;


    private User currentUser;

    @FXML
    public void initialize() {
        ClientApp.getInstance().setMainDashboardController(this);
        currentUser = ClientApp.getInstance().getCurrentUser();
        if (currentUser != null) {
            welcomeLabel.setText("Welcome, " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            configureTabsForRole(currentUser.getRole());
            if(auctionViewTabController != null) {
                auctionViewTabController.loadActiveAuctions();
            }
            if (messagesViewTabController != null) {
                messagesViewTabController.loadUserMessages();
            }
            if(userProfileTabController != null) {
                userProfileTabController.loadUserProfile();
            }
        } else {

            welcomeLabel.setText("Welcome, Guest!");
            ClientApp.getInstance().showLoginView();
        }
    }

    private void configureTabsForRole(Role role) {
        mainTabPane.getTabs().remove(sellerTab);
        mainTabPane.getTabs().remove(buyerTab);
        mainTabPane.getTabs().remove(adminTab);

        if (role == Role.SELLER) {
            mainTabPane.getTabs().add(sellerTab);
            if(sellerDashboardTabController != null) sellerDashboardTabController.loadSellerData();
        }
        if (role == Role.BUYER) {
            mainTabPane.getTabs().add(buyerTab);
            if(buyerDashboardTabController != null) buyerDashboardTabController.loadBuyerData();
        }
        if (role == Role.ADMIN) {
            mainTabPane.getTabs().add(adminTab);

            if (!mainTabPane.getTabs().contains(adminTab)) mainTabPane.getTabs().add(adminTab);

            if(adminDashboardTabController != null) adminDashboardTabController.loadAdminData();
        }

    }

    @FXML
    private void handleLogout() {
        ClientApp.getInstance().setCurrentUser(null);
        if (ClientApp.getInstance().getNetworkHandler() != null) {
            ClientApp.getInstance().getNetworkHandler().disconnect();
        }
        ClientApp.getInstance().showLoginView();
    }
}