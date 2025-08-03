package com.university.auctionsystem.client;

import com.university.auctionsystem.client.controllers.BuyerDashboardController;
import com.university.auctionsystem.client.controllers.LoginController;
import com.university.auctionsystem.client.controllers.MainDashboardController;
import com.university.auctionsystem.client.controllers.SellerDashboardController;
import com.university.auctionsystem.shared.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ClientApp extends Application {

    private static ClientApp instance;
    private Stage primaryStage;
    private ClientNetworkHandler networkHandler;
    private User currentUser;

    private MainDashboardController mainDashboardController;
    private SellerDashboardController sellerDashboardController;

    public BuyerDashboardController getBuyerDashboardController() {
        return buyerDashboardController;
    }

    public void setBuyerDashboardController(BuyerDashboardController buyerDashboardController) {
        this.buyerDashboardController = buyerDashboardController;
    }

    public SellerDashboardController getSellerDashboardController() {
        return sellerDashboardController;
    }

    public void setSellerDashboardController(SellerDashboardController sellerDashboardController) {
        this.sellerDashboardController = sellerDashboardController;
    }

    public MainDashboardController getMainDashboardController() {
        return mainDashboardController;
    }

    public void setMainDashboardController(MainDashboardController mainDashboardController) {
        this.mainDashboardController = mainDashboardController;
    }

    private BuyerDashboardController buyerDashboardController;

    public ClientApp() {
        instance = this;
        networkHandler = new ClientNetworkHandler();
    }

    public static ClientApp getInstance() {
        return instance;
    }

    public ClientNetworkHandler getNetworkHandler() {
        return networkHandler;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }


    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("AuctoSphere");

        try {
            Image appIcon = new Image(getClass().getResourceAsStream("/images/logo.png"));
            primaryStage.getIcons().add(appIcon);
        } catch (Exception e) {
            System.err.println("Error loading application icon: /images/logo.png");
        }
        showLoginView();

        primaryStage.setOnCloseRequest(event -> {
            networkHandler.disconnect();
            System.out.println("Application closing.");
        });
    }

    public void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/LoginView.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, 400, 300));
            primaryStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/RegisterView.fxml"));
            Parent root = loader.load();
            primaryStage.setScene(new Scene(root, 450, 400));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMainDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("views/MainDashboardView.fxml"));
            Parent root = loader.load();
            this.mainDashboardController = loader.getController();
            primaryStage.setScene(new Scene(root, 800, 600));
            primaryStage.setTitle("AuctoSphere - " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}