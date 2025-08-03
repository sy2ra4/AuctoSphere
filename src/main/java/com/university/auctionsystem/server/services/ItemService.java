package com.university.auctionsystem.server.services;

import com.university.auctionsystem.server.DatabaseManager;
import com.university.auctionsystem.shared.model.Item;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ItemService {
    private DatabaseManager dbManager;
    private AuctionService auctionService;

    public ItemService(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void setAuctionService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public Item listItem(Item item) {

        if (item.getSellerId() <= 0) {
            System.err.println("ItemService: Seller ID not set for item listing.");
            return null;
        }

        String sql = "INSERT INTO items (seller_id, name, description, image_path, category, tags) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, item.getSellerId());
            pstmt.setString(2, item.getName());
            pstmt.setString(3, item.getDescription());
            pstmt.setString(4, item.getImagePath());
            pstmt.setString(5, item.getCategory());
            pstmt.setString(6, item.getTags());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("ItemService: Creating item failed, no rows affected.");
                return null;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setItemId(generatedKeys.getInt(1));
                    return item;
                } else {
                    System.err.println("ItemService: Creating item failed, no ID obtained.");
                    return null;
                }
            }
        } catch (SQLException e) {
            System.err.println("ItemService SQL Error listing item: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<Item> getItemsBySellerId(int sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT item_id, seller_id, name, description, image_path, category, tags, created_at FROM items WHERE seller_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, sellerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            System.err.println("ItemService SQL Error getting items by seller ID: " + e.getMessage());
            e.printStackTrace();
        }
        return items;
    }

    public Item getItemById(int itemId) {
        String sql = "SELECT item_id, seller_id, name, description, image_path, category, tags, created_at FROM items WHERE item_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToItem(rs);
            }
        } catch (SQLException e) {
            System.err.println("ItemService SQL Error getting item by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        Item item = new Item();
        item.setItemId(rs.getInt("item_id"));
        item.setSellerId(rs.getInt("seller_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setImagePath(rs.getString("image_path"));
        item.setCategory(rs.getString("category"));
        item.setTags(rs.getString("tags"));
        item.setCreatedAt(rs.getTimestamp("created_at"));
        return item;
    }

    public boolean updateItem(Item itemToUpdate, int sellerId) {
        if (itemToUpdate == null || itemToUpdate.getItemId() <= 0) {
            System.err.println("ItemService: Invalid item or item ID for update.");
            return false;
        }
        Item currentItem = getItemById(itemToUpdate.getItemId());
        if (currentItem == null || currentItem.getSellerId() != sellerId) {
            System.err.println("ItemService: Item " + itemToUpdate.getItemId() + " not found or does not belong to seller " + sellerId);
            return false;
        }
        if (this.auctionService == null) {
            System.err.println("ItemService: AuctionService not set. Cannot check auction status for item update.");
            return false;
        }
        if (this.auctionService.isItemInActiveOrUpcomingAuction(itemToUpdate.getItemId())) {
            System.err.println("ItemService: Item " + itemToUpdate.getItemId() + " is in an active or upcoming auction and cannot be edited.");
            return false;
        }

        String sql = "UPDATE items SET name = ?, description = ?, image_path = ?, category = ?, tags = ? " +
                "WHERE item_id = ? AND seller_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setString(1, itemToUpdate.getName());
            pstmt.setString(2, itemToUpdate.getDescription());
            pstmt.setString(3, itemToUpdate.getImagePath());
            pstmt.setString(4, itemToUpdate.getCategory());
            pstmt.setString(5, itemToUpdate.getTags());
            pstmt.setInt(6, itemToUpdate.getItemId());
            pstmt.setInt(7, sellerId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("ItemService: Item " + itemToUpdate.getItemId() + " updated successfully.");
                return true;
            } else {
                System.err.println("ItemService: Item " + itemToUpdate.getItemId() + " update failed (no rows affected or ownership mismatch).");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("ItemService SQL Error updating item: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}