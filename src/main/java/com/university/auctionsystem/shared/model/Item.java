package com.university.auctionsystem.shared.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    private int itemId;
    private int sellerId;
    private String name;
    private String description;
    private String imagePath;
    private String category;
    private String tags;
    private Timestamp createdAt;

    public Item() {
    }

    public int getItemId() { return itemId; }
    public void setItemId(int itemId) { this.itemId = itemId; }
    public int getSellerId() { return sellerId; }
    public void setSellerId(int sellerId) { this.sellerId = sellerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return name + " (ID: " + itemId + ")";
    }
}