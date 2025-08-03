package com.university.auctionsystem.server.services;

import com.university.auctionsystem.server.ClientHandler;
import com.university.auctionsystem.server.DatabaseManager;
import com.university.auctionsystem.shared.model.Auction;
import com.university.auctionsystem.shared.model.Bid;
import com.university.auctionsystem.shared.model.Item;
import com.university.auctionsystem.shared.model.User;
import com.university.auctionsystem.shared.protocol.Response;
import com.university.auctionsystem.shared.protocol.RequestType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class AuctionService {
    private DatabaseManager dbManager;
    private ItemService itemService;
    private UserService userService;
    private ConcurrentHashMap<Integer, List<ClientHandler>> auctionSubscribers = new ConcurrentHashMap<>();
    private Timer auctionTimer;
    private final Set<ClientHandler> activeClientHandlers;


    public AuctionService(DatabaseManager dbManager, ItemService itemService,Set<ClientHandler> activeClientHandlers) {
        this.dbManager = dbManager;
        this.itemService = itemService;
        this.activeClientHandlers = activeClientHandlers;
        this.auctionTimer = new Timer("AuctionScheduler", true);
        this.userService = new UserService(dbManager);
        scheduleAuctionStatusChecks();
    }

    public void addSubscriber(int auctionId, ClientHandler handler) {
        auctionSubscribers.computeIfAbsent(auctionId, k -> new ArrayList<>()).add(handler);
    }

    public void removeSubscriber(int auctionId, ClientHandler handler) {
        List<ClientHandler> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null) {
            subscribers.remove(handler);
            if (subscribers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        }
    }

    public void unsubscribeClientFromAllAuctions(ClientHandler handlerToRemove) {
        auctionSubscribers.forEach((auctionId, handlers) -> {
            Iterator<ClientHandler> iterator = handlers.iterator();
            while (iterator.hasNext()) {
                ClientHandler handler = iterator.next();
                if (handler.equals(handlerToRemove)) {
                    iterator.remove();
                }
            }
            if (handlers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        });
        System.out.println("Client unsubscribed from all auction updates.");
    }



    private void broadcastAuctionUpdate(Auction auction) {
        System.out.println("Broadcasting update for auction ID: " + auction.getAuctionId());
        List<ClientHandler> subscribers = auctionSubscribers.get(auction.getAuctionId());
        if (subscribers != null) {
            for (ClientHandler handler : subscribers) {
                handler.sendAuctionUpdate(auction);
            }
        }
    }


    public Auction createAuction(Auction auction, int sellerId) {
        Item itemForAuction = itemService.getItemById(auction.getItemId());
        if (itemForAuction == null) {
            System.err.println("AuctionService: Item ID " + auction.getItemId() + " not found for auction creation.");
            return null;
        }
        if (itemForAuction.getSellerId() != sellerId) {
            System.err.println("AuctionService: Item ID " + auction.getItemId() + " does not belong to seller ID " + sellerId);
            return null;
        }


        if (isItemInActiveOrUpcomingAuction(auction.getItemId())) {
            System.err.println("AuctionService: Item ID " + auction.getItemId() + " is already in an active or upcoming auction.");
            return null;
        }


        String sql = "INSERT INTO auctions (item_id, start_time, end_time, start_price, reserve_price, current_highest_bid, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, auction.getItemId());
            pstmt.setTimestamp(2, auction.getStartTime());
            pstmt.setTimestamp(3, auction.getEndTime());
            pstmt.setBigDecimal(4, auction.getStartPrice());
            pstmt.setBigDecimal(5, auction.getReservePrice());
            pstmt.setBigDecimal(6, auction.getStartPrice());
            pstmt.setString(7, Auction.AuctionStatus.UPCOMING.name());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                System.err.println("AuctionService: Creating auction failed, no rows affected.");
                return null;
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auction.setAuctionId(generatedKeys.getInt(1));
                    auction.setStatus(Auction.AuctionStatus.UPCOMING);
                    auction.setCurrentHighestBid(auction.getStartPrice());
                    auction.setItem(itemForAuction);
                    System.out.println("AuctionService: Auction created successfully ID " + auction.getAuctionId() + " for item ID " + auction.getItemId());
                    return auction;
                } else {
                    System.err.println("AuctionService: Creating auction failed, no ID obtained.");
                    return null;
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || e.getMessage().toLowerCase().contains("duplicate entry")) {
                System.err.println("AuctionService: Item ID " + auction.getItemId() + " is likely already in an auction (unique constraint). " + e.getMessage());
            } else {
                System.err.println("AuctionService SQL Error creating auction: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    public boolean isItemInActiveOrUpcomingAuction(int itemId) {
        String sql = "SELECT COUNT(*) FROM auctions WHERE item_id = ? AND (status = 'ACTIVE' OR status = 'UPCOMING')";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("AuctionService SQL Error checking if item is in auction: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean cancelUpcomingAuction(int auctionId, int sellerId) {
        Auction auction = getAuctionDetails(auctionId);

        if (auction == null) {
            System.err.println("AuctionService: Auction " + auctionId + " not found for cancellation.");
            return false;
        }
        if (auction.getItem() == null || auction.getItem().getSellerId() != sellerId) {
            System.err.println("AuctionService: Auction " + auctionId + " does not belong to seller " + sellerId);
            return false;
        }
        if (auction.getStatus() != Auction.AuctionStatus.UPCOMING) {
            System.err.println("AuctionService: Auction " + auctionId + " is not UPCOMING. Current status: " + auction.getStatus());
            return false;
        }

        String sql = "UPDATE auctions SET status = ? WHERE auction_id = ? AND status = 'UPCOMING'";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setString(1, Auction.AuctionStatus.CANCELLED.name());
            pstmt.setInt(2, auctionId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("AuctionService: Auction " + auctionId + " cancelled successfully by seller " + sellerId);
                auction.setStatus(Auction.AuctionStatus.CANCELLED);
                broadcastAuctionUpdate(auction);
                auctionSubscribers.remove(auctionId);
                return true;
            } else {
                System.err.println("AuctionService: Auction " + auctionId + " cancellation failed (no rows affected or status changed concurrently).");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("AuctionService SQL Error cancelling auction: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Auction> getActiveAuctions() {
        List<Auction> auctions = new ArrayList<>();

        String sql = "SELECT a.*, i.name as item_name, i.description as item_desc, i.image_path as item_image " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "WHERE a.status = 'ACTIVE' ORDER BY a.end_time ASC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                auctions.add(mapResultSetToAuctionWithItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }



    public Auction getAuctionDetails(int auctionId) {
        String sql = "SELECT a.*, i.name as item_name, i.description as item_desc, i.image_path as item_image, i.seller_id as item_seller_id " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "WHERE a.auction_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToAuctionWithItem(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Auction mapResultSetToAuctionWithItem(ResultSet rs) throws SQLException {
        Auction auction = new Auction();
        auction.setAuctionId(rs.getInt("auction_id"));
        auction.setItemId(rs.getInt("item_id"));
        auction.setStartTime(rs.getTimestamp("start_time"));
        auction.setEndTime(rs.getTimestamp("end_time"));
        auction.setStartPrice(rs.getBigDecimal("start_price"));
        auction.setReservePrice(rs.getBigDecimal("reserve_price"));
        auction.setCurrentHighestBid(rs.getBigDecimal("current_highest_bid"));
        auction.setWinningBidderId(rs.getInt("winning_bidder_id"));
        auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("status")));
        if (hasColumn(rs, "payment_status")) {
            String paymentStatusStr = rs.getString("payment_status");
            if (paymentStatusStr != null) {
                auction.setPaymentStatus(Auction.PaymentStatus.valueOf(paymentStatusStr));
            } else {
                auction.setPaymentStatus(Auction.PaymentStatus.PENDING);
            }
        } else {
            auction.setPaymentStatus(Auction.PaymentStatus.PENDING);
        }
        auction.setCreatedAt(rs.getTimestamp("created_at"));
        auction.setStatus(Auction.AuctionStatus.valueOf(rs.getString("status")));


        Item item = new Item();
        item.setItemId(rs.getInt("item_id"));
        item.setName(rs.getString("item_name"));
        item.setDescription(rs.getString("item_desc"));
        item.setImagePath(rs.getString("item_image"));

        if (hasColumn(rs, "item_seller_id")) {
            item.setSellerId(rs.getInt("item_seller_id"));
        }
        if (hasColumn(rs, "item_category")) {
            item.setCategory(rs.getString("item_category"));
        }
        auction.setItem(item);
        return auction;
    }

    public synchronized boolean processPayment(int auctionId, int buyerId) {
        Auction auction = getAuctionDetails(auctionId);
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.ENDED ||
                auction.getWinningBidderId() != buyerId ||
                auction.getPaymentStatus() == Auction.PaymentStatus.PAID) {
            System.err.println("AuctionService: Payment processing failed. Auction ID: " + auctionId +
                    ", Buyer ID: " + buyerId + ", Auction Status: " + (auction != null ? auction.getStatus() : "N/A") +
                    ", Winner: " + (auction != null ? auction.getWinningBidderId() : "N/A") +
                    ", Payment Status: " + (auction != null ? auction.getPaymentStatus() : "N/A"));
            return false;
        }
        String sql = "UPDATE auctions SET payment_status = ? WHERE auction_id = ? AND winning_bidder_id = ?";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setString(1, Auction.PaymentStatus.PAID.name());
            pstmt.setInt(2, auctionId);
            pstmt.setInt(3, buyerId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("AuctionService: Payment successfully processed for auction ID: " + auctionId);
                auction.setPaymentStatus(Auction.PaymentStatus.PAID);
                broadcastAuctionUpdate(auction);
                return true;
            } else {
                System.err.println("AuctionService: Payment update failed for auction ID: " + auctionId + ", no rows affected or conditions not met.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteAuctionAsAdmin(int auctionId) {
        Auction auction = getAuctionDetails(auctionId);
        if (auction == null) {
            System.err.println("AuctionService: Cannot delete. Auction ID " + auctionId + " not found.");
            return false;
        }
        String deleteBidsSql = "DELETE FROM bids WHERE auction_id = ?";
        try (PreparedStatement pstmtBids = dbManager.getPreparedStatement(deleteBidsSql)) {
            pstmtBids.setInt(1, auctionId);
            int bidsDeleted = pstmtBids.executeUpdate();
            System.out.println("AuctionService: Deleted " + bidsDeleted + " bids for auction ID " + auctionId);
        } catch (SQLException e) {
            System.err.println("AuctionService: Error deleting bids for auction " + auctionId + ": " + e.getMessage());
        }
        String deleteMessagesSql = "DELETE FROM messages WHERE auction_id = ?";
        try (PreparedStatement pstmtMessages = dbManager.getPreparedStatement(deleteMessagesSql)) {
            pstmtMessages.setInt(1, auctionId);
            int messagesDeleted = pstmtMessages.executeUpdate();
            System.out.println("AuctionService: Deleted " + messagesDeleted + " messages for auction ID " + auctionId);
        } catch (SQLException e) {
            System.err.println("AuctionService: Error deleting messages for auction " + auctionId + ": " + e.getMessage());
        }
        String deleteAuctionSql = "DELETE FROM auctions WHERE auction_id = ?";
        try (PreparedStatement pstmtAuction = dbManager.getPreparedStatement(deleteAuctionSql)) {
            pstmtAuction.setInt(1, auctionId);
            int affectedRows = pstmtAuction.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("AuctionService: Auction ID " + auctionId + " deleted successfully by admin.");
                List<ClientHandler> subscribers = auctionSubscribers.remove(auctionId);
                if (subscribers != null) {
                    System.out.println("AuctionService: Removed " + subscribers.size() + " subscribers for deleted auction " + auctionId);
                }
                return true;
            } else {
                System.err.println("AuctionService: Deleting auction ID " + auctionId + " failed, no rows affected.");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        java.sql.ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnName(x)) || columnName.equalsIgnoreCase(rsmd.getColumnLabel(x))) {
                return true;
            }
        }
        return false;
    }

    public synchronized Bid placeBid(Bid bid) {
        Auction auction = getAuctionDetails(bid.getAuctionId());
        if (auction == null || auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
            System.out.println("Bid rejected: Auction " + bid.getAuctionId() + " not active or not found.");
            return null;
        }
        if (auction.getItem() == null) {
            System.out.println("Bid rejected: Auction " + bid.getAuctionId() + " item details missing.");
            return null;
        }
        if (bid.getBidderId() == auction.getItem().getSellerId()) {
            System.out.println("Bid rejected: Seller cannot bid on their own item. Auction: " + bid.getAuctionId());
            return null;
        }
        BigDecimal currentHighest = auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : auction.getStartPrice();
        if (bid.getBidAmount().compareTo(currentHighest) <= 0) {
            System.out.println("Bid rejected: Bid amount " + bid.getBidAmount() + " not higher than current " + currentHighest + " for auction " + bid.getAuctionId());
            return null;
        }

        int previousHighestBidderId = auction.getWinningBidderId();


        String sqlInsertBid = "INSERT INTO bids (auction_id, bidder_id, bid_amount) VALUES (?, ?, ?)";
        String sqlUpdateAuction = "UPDATE auctions SET current_highest_bid = ?, winning_bidder_id = ? WHERE auction_id = ?";

        try (PreparedStatement pstmtInsert = dbManager.getPreparedStatement(sqlInsertBid, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmtUpdate = dbManager.getPreparedStatement(sqlUpdateAuction)) {

            dbManager.getConnection().setAutoCommit(false);

            pstmtInsert.setInt(1, bid.getAuctionId());
            pstmtInsert.setInt(2, bid.getBidderId());
            pstmtInsert.setBigDecimal(3, bid.getBidAmount());
            int affectedRows = pstmtInsert.executeUpdate();
            if (affectedRows == 0) {
                dbManager.getConnection().rollback();
                return null;
            }
            try (ResultSet generatedKeys = pstmtInsert.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    bid.setBidId(generatedKeys.getInt(1));
                    User bidder = userService.getUserById(bid.getBidderId());
                    if (bidder != null) bid.setBidderUsername(bidder.getUsername());
                } else {
                    dbManager.getConnection().rollback();
                    return null;
                }
            }

            pstmtUpdate.setBigDecimal(1, bid.getBidAmount());
            pstmtUpdate.setInt(2, bid.getBidderId());
            pstmtUpdate.setInt(3, bid.getAuctionId());
            pstmtUpdate.executeUpdate();

            dbManager.getConnection().commit();
            auction.setCurrentHighestBid(bid.getBidAmount());
            auction.setWinningBidderId(bid.getBidderId());

            broadcastAuctionUpdate(auction);
            if (previousHighestBidderId > 0 && previousHighestBidderId != bid.getBidderId()) {
                sendOutbidNotification(previousHighestBidderId, auction);
            }

            return bid;

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                dbManager.getConnection().rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return null;
        } finally {
            try {
                dbManager.getConnection().setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendOutbidNotification(int outbidUserId, Auction auction) {
        for (ClientHandler handler : activeClientHandlers) {
            if (handler.getCurrentUser() != null && handler.getCurrentUser().getUserId() == outbidUserId) {
                Response outbidResponse = new Response(true, "You have been outbid on " + auction.getItem().getName(), auction, RequestType.OUTBID_NOTIFICATION);
                handler.sendNotification(outbidResponse);
                break;
            }
        }
    }

    public List<Bid> getBidsForAuction(int auctionId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_username " +
                "FROM bids b JOIN users u ON b.bidder_id = u.user_id " +
                "WHERE b.auction_id = ? ORDER BY b.bid_time DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Bid bid = new Bid();
                bid.setBidId(rs.getInt("bid_id"));
                bid.setAuctionId(rs.getInt("auction_id"));
                bid.setBidderId(rs.getInt("bidder_id"));
                bid.setBidAmount(rs.getBigDecimal("bid_amount"));
                bid.setBidTime(rs.getTimestamp("bid_time"));
                bid.setBidderUsername(rs.getString("bidder_username"));
                bids.add(bid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    private void scheduleAuctionStatusChecks() {
        auctionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateUpcomingAuctions();
                updateActiveAuctionsToEnd();
            }
        }, 0, 10_000);
    }

    private void updateUpcomingAuctions() {
        String sql = "UPDATE auctions SET status = 'ACTIVE' WHERE status = 'UPCOMING' AND start_time <= CURRENT_TIMESTAMP";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            int updatedCount = pstmt.executeUpdate();
            if (updatedCount > 0) {
                System.out.println(updatedCount + " auctions moved from UPCOMING to ACTIVE.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateActiveAuctionsToEnd() {
        String sqlSelect = "SELECT auction_id, item_id, winning_bidder_id, reserve_price, current_highest_bid FROM auctions WHERE status = 'ACTIVE' AND end_time <= CURRENT_TIMESTAMP";

        List<Integer> auctionsToEnd = new ArrayList<>();
        try (PreparedStatement pstmtSelect = dbManager.getPreparedStatement(sqlSelect)) {
            ResultSet rs = pstmtSelect.executeQuery();
            while (rs.next()) {
                auctionsToEnd.add(rs.getInt("auction_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        for (int auctionId : auctionsToEnd) {
            Auction auction = getAuctionDetails(auctionId);
            if (auction == null || auction.getStatus() != Auction.AuctionStatus.ACTIVE)
                continue;

            boolean reserveMet = true;
            boolean hasWinner = auction.getWinningBidderId() > 0;

            if (auction.getReservePrice() != null && auction.getReservePrice().compareTo(BigDecimal.ZERO) > 0) {
                if (!hasWinner || auction.getCurrentHighestBid().compareTo(auction.getReservePrice()) < 0) {
                    reserveMet = false;
                }
            }

            Auction.AuctionStatus finalStatus;
            int finalWinnerId = 0;

            if (hasWinner && reserveMet) {
                finalStatus = Auction.AuctionStatus.ENDED;
                finalWinnerId = auction.getWinningBidderId();
            } else if (hasWinner && !reserveMet) {
                finalStatus = Auction.AuctionStatus.ENDED;
                System.out.println("Auction " + auctionId + " ended, reserve not met.");
            } else {
                finalStatus = Auction.AuctionStatus.ENDED;
                System.out.println("Auction " + auctionId + " ended, no bids.");
            }

            String sqlUpdateStatus = "UPDATE auctions SET status = ?, winning_bidder_id = ? WHERE auction_id = ?";
            try (PreparedStatement pstmtUpdate = dbManager.getPreparedStatement(sqlUpdateStatus)) {
                pstmtUpdate.setString(1, finalStatus.name());
                if (finalWinnerId > 0) {
                    pstmtUpdate.setInt(2, finalWinnerId);
                } else {
                    pstmtUpdate.setNull(2, Types.INTEGER);
                }
                pstmtUpdate.setInt(3, auctionId);
                int updated = pstmtUpdate.executeUpdate();

                if (updated > 0) {
                    System.out.println("Auction ID " + auctionId + " status updated to " + finalStatus);
                    auction.setStatus(finalStatus);
                    auction.setWinningBidderId(finalWinnerId);

                    broadcastAuctionUpdate(auction);
                    if (finalStatus == Auction.AuctionStatus.ENDED && finalWinnerId > 0) {
                        sendWinnerNotification(finalWinnerId, auction);
                    }
                    sendSellerAuctionEndedNotification(auction);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendWinnerNotification(int winnerId, Auction auction) {
        for (ClientHandler handler : activeClientHandlers) {
            if (handler.getCurrentUser() != null && handler.getCurrentUser().getUserId() == winnerId) {
                Response winnerResponse = new Response(true, "Congratulations! You won the auction for " + auction.getItem().getName(), auction, RequestType.WINNER_NOTIFICATION);
                handler.sendNotification(winnerResponse);
                break;
            }
        }
    }

    private void sendSellerAuctionEndedNotification(Auction auction) {
        if (auction.getItem() == null || auction.getItem().getSellerId() <= 0) return;
        int sellerId = auction.getItem().getSellerId();

        for (ClientHandler handler : activeClientHandlers) {
            if (handler.getCurrentUser() != null && handler.getCurrentUser().getUserId() == sellerId) {
                String message;
                if (auction.getWinningBidderId() > 0) {
                    User winner = userService.getUserById(auction.getWinningBidderId());
                    String winnerName = winner != null ? winner.getUsername() : "Unknown";
                    message = String.format("Your auction for '%s' has ended. Sold to %s for %s.",
                            auction.getItem().getName(), winnerName, auction.getCurrentHighestBid());
                } else {
                    message = String.format("Your auction for '%s' has ended. Item was not sold (no qualifying bids or reserve not met).",
                            auction.getItem().getName());
                }
                Response sellerResponse = new Response(true, message, auction, RequestType.AUCTION_ENDED_SELLER_NOTIFICATION);
                handler.sendNotification(sellerResponse);
                break;
            }
        }

    }

    public void shutdownScheduler() {
        if (this.auctionTimer != null) {
            this.auctionTimer.cancel();
            this.auctionTimer.purge();
            System.out.println("Auction scheduler has been shut down.");
        }
    }

    public List<Bid> getBidsByUserId(int userId) {
        List<Bid> bids = new ArrayList<>();
        String sql = "SELECT b.*, u.username as bidder_username, a.item_id, i.name as item_name, a.status as auction_status " +
                "FROM bids b " +
                "JOIN users u ON b.bidder_id = u.user_id " +
                "JOIN auctions a ON b.auction_id = a.auction_id " +
                "JOIN items i ON a.item_id = i.item_id " +
                "WHERE b.bidder_id = ? ORDER BY b.bid_time DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Bid bid = new Bid();
                bid.setBidId(rs.getInt("bid_id"));
                bid.setAuctionId(rs.getInt("auction_id"));
                bid.setBidderId(rs.getInt("bidder_id"));
                bid.setBidAmount(rs.getBigDecimal("bid_amount"));
                bid.setBidTime(rs.getTimestamp("bid_time"));
                bid.setBidderUsername(rs.getString("bidder_username"));

                bid.setAuctionItemName(rs.getString("item_name"));
                bid.setAuctionStatus(rs.getString("auction_status"));
                bids.add(bid);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return bids;
    }

    public List<Auction> getWonAuctionsByUserId(int userId) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.*, i.name as item_name, i.description as item_desc, i.image_path as item_image " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "WHERE a.winning_bidder_id = ? AND a.status = 'ENDED' " +
                "ORDER BY a.end_time DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                auctions.add(mapResultSetToAuctionWithItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    public List<Auction> getAllAuctionsForAdmin() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.*, i.name as item_name, i.description as item_desc, i.image_path as item_image " +
                "FROM auctions a JOIN items i ON a.item_id = i.item_id " +
                "ORDER BY a.created_at DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                auctions.add(mapResultSetToAuctionWithItem(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    public List<Auction> getAuctionsCreatedBySeller(int sellerId) {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT a.*, i.name as item_name, i.description as item_desc, i.image_path as item_image, i.seller_id as item_seller_id, " +
                "u_winner.username as winner_username " +
                "FROM auctions a " +
                "JOIN items i ON a.item_id = i.item_id " +
                "LEFT JOIN users u_winner ON a.winning_bidder_id = u_winner.user_id " +
                "WHERE i.seller_id = ? " +
                "ORDER BY a.created_at DESC";
        try (PreparedStatement pstmt = dbManager.getPreparedStatement(sql)) {
            pstmt.setInt(1, sellerId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Auction auction = mapResultSetToAuctionWithItem(rs);
                if (auction.getWinningBidderId() > 0 && hasColumn(rs, "winner_username")) {
                }
                auctions.add(auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }


}