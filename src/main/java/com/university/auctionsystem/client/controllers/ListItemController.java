package com.university.auctionsystem.client.controllers;

import com.university.auctionsystem.client.ClientApp;
import com.university.auctionsystem.client.ClientNetworkHandler;
import com.university.auctionsystem.shared.model.Item;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class ListItemController {

    @FXML private TextField itemNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField categoryField;
    @FXML private TextField tagsField;
    @FXML private TextField imagePathField;
    @FXML private Label statusLabel;

    private ClientNetworkHandler networkHandler;
    private Item itemToEdit=null;


    @FXML
    public void initialize() {
        networkHandler = ClientApp.getInstance().getNetworkHandler();
        statusLabel.setText("");
    }


    public void setItemToEdit(Item item) {
        this.itemToEdit = item;
        if (item != null) {
            itemNameField.setText(item.getName());
            descriptionArea.setText(item.getDescription());
            categoryField.setText(item.getCategory());
            tagsField.setText(item.getTags());
            imagePathField.setText(item.getImagePath());
        }
    }

    @FXML
    private void handleListItemButtonAction() {
        String name = itemNameField.getText();
        String description = descriptionArea.getText();
        String category = categoryField.getText();
        String tags = tagsField.getText();
        String imagePath = imagePathField.getText();

        if (name.isEmpty() || description.isEmpty()) {
            statusLabel.setText("Item name and description are required.");
            return;
        }

        Item newItem = new Item();
        Item itemData = new Item();
        itemData.setName(name);
        itemData.setDescription(description);
        itemData.setCategory(categoryField.getText().trim());
        itemData.setTags(tagsField.getText().trim());
        itemData.setImagePath(imagePath);

        Request request;
        if (itemToEdit != null) {
            itemData.setItemId(itemToEdit.getItemId());

            request = new Request(RequestType.UPDATE_ITEM, itemData);
            statusLabel.setText("Updating item...");
        } else {
            request = new Request(RequestType.LIST_ITEM, itemData);
            statusLabel.setText("Listing item...");
        }

        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    statusLabel.setText(itemToEdit != null ? "Item updated successfully!" : "Item listed successfully!");
                    ((Stage) statusLabel.getScene().getWindow()).close();
                } else {
                    statusLabel.setText("Operation failed: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusLabel.setText("Error: " + ex.getMessage());
                ex.printStackTrace();
            });
            return null;
        });


        request = new Request(RequestType.LIST_ITEM, newItem);
        statusLabel.setText("Listing item...");

        networkHandler.sendRequestAsync(request).thenAccept(response -> {
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    statusLabel.setText("Item listed successfully!");

                    ((Stage) itemNameField.getScene().getWindow()).close();
                } else {
                    statusLabel.setText("Failed to list item: " + (response != null ? response.getMessage() : "No response"));
                }
            });
        }).exceptionally(ex -> {
            Platform.runLater(() -> {
                statusLabel.setText("Error listing item: " + ex.getMessage());
                ex.printStackTrace();
            });
            return null;
        });
    }

}