package com.university.auctionsystem.server;

import com.university.auctionsystem.server.services.AuctionService;
import com.university.auctionsystem.server.services.ItemService;
import com.university.auctionsystem.server.services.UserService;
import com.university.auctionsystem.shared.model.*;
import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.protocol.Response;
import com.university.auctionsystem.server.services.MessageService;
import com.university.auctionsystem.shared.model.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private UserService userService;
    private ItemService itemService;
    private AuctionService auctionService;
    private User currentUser;

    private Set<ClientHandler> activeClientHandlers;
    private volatile boolean handlerRunning = true;
    private MessageService messageService;

    public ClientHandler(Socket socket, UserService userService, ItemService itemService, AuctionService auctionService, MessageService messageService, Set<ClientHandler> activeClientHandlers) {
        this.clientSocket = socket;
        this.userService = userService;
        this.itemService = itemService;
        this.auctionService = auctionService;
        this.messageService = messageService;
        this.activeClientHandlers = activeClientHandlers;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void sendAuctionUpdate(Auction auction) {
        if (out != null) {
            try {
                Response updateResponse = new Response(true, "Auction Updated", auction, RequestType.AUCTION_UPDATE);
                out.writeObject(updateResponse);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending auction update to client: " + e.getMessage());
            }
        }
    }


    @Override
    public void run() {
        activeClientHandlers.add(this);
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());

            Request request;
            while (handlerRunning && (request = (Request) in.readObject()) != null) {
                if (request.getType() == RequestType.DISCONNECT) {
                    handlerRunning = false;
                    break;
                }
                Response response = processRequest(request);
                if (response != null) {
                    out.writeObject(response);
                    out.flush();
                }
                if (!handlerRunning) break;
            }
        } catch (SocketException | EOFException e) {
            System.out.println("Client " + (currentUser != null ? currentUser.getUsername() : clientSocket.getInetAddress()) + " disconnected: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            if (handlerRunning && !clientSocket.isClosed()) {
                System.err.println("ClientHandler Error for " + (currentUser != null ? currentUser.getUsername() : clientSocket.getInetAddress()) + ": " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            closeConnection();
        }
    }

    private Response processRequest(Request request) {
        RequestType type = request.getType();
        Object payload = request.getPayload();
        String correlationId = request.getCorrelationId();
        System.out.println("Server received request: " + type + " with payload: " + (payload != null ? payload.toString().substring(0, Math.min(100, payload.toString().length())) : "null"));


        try {
            switch (type) {
                case REGISTER_USER:
                    User newUser = (User) payload;
                    User registeredUser = userService.registerUser(newUser);
                    if (registeredUser != null) {
                        return new Response(true, "Registration successful", registeredUser, type, correlationId);
                    } else {
                        return new Response(false, "Registration failed. Username or email might be taken.", null, type, correlationId);
                    }
                case LOGIN_USER:
                    User credentials = (User) payload;
                    User loggedInUser = userService.loginUser(credentials.getUsername(), credentials.getPasswordHash());
                    if (loggedInUser != null) {
                        this.currentUser = loggedInUser;
                        return new Response(true, "Login successful", loggedInUser, type, correlationId);
                    } else {
                        return new Response(false, "Login failed. Invalid credentials.", null, type, correlationId);
                    }
                case LIST_ITEM:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not logged in as Seller.", null, type, correlationId);
                    }
                    Item itemToSell = (Item) payload;
                    itemToSell.setSellerId(currentUser.getUserId());
                    Item listedItem = itemService.listItem(itemToSell);
                    if (listedItem != null) {
                        return new Response(true, "Item listed successfully.", listedItem, type, correlationId);
                    } else {
                        return new Response(false, "Failed to list item.", null, type, correlationId);
                    }
                case CREATE_AUCTION:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not logged in as Seller.", null, type, correlationId);
                    }
                    Auction auctionToCreate = (Auction) payload;
                    Item itemForAuction = itemService.getItemById(auctionToCreate.getItemId());
                    if (itemForAuction == null || itemForAuction.getSellerId() != currentUser.getUserId()) {
                        return new Response(false, "Item not found or does not belong to you.", null, type, correlationId);
                    }
                    Auction createdAuction = auctionService.createAuction(auctionToCreate, currentUser.getUserId());
                    if (createdAuction != null) {
                        return new Response(true, "Auction created successfully.", createdAuction, type, correlationId);
                    } else {
                        return new Response(false, "Failed to create auction.", null, type, correlationId);
                    }
                case GET_ACTIVE_AUCTIONS:
                    List<Auction> activeAuctions = auctionService.getActiveAuctions();
                    return new Response(true, "Fetched active auctions.", activeAuctions, type, correlationId);

                case GET_AUCTION_DETAILS:
                    Integer auctionId = (Integer) payload;
                    Auction auctionDetails = auctionService.getAuctionDetails(auctionId);
                    if (auctionDetails != null) {
                        auctionService.addSubscriber(auctionId, this);
                        return new Response(true, "Fetched auction details.", auctionDetails, type, correlationId);
                    } else {
                        return new Response(false, "Auction not found.", null, type, correlationId);
                    }
                case GET_SELLER_ITEMS:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not logged in as Seller.", null, type, correlationId);
                    }
                    List<Item> sellerItems = itemService.getItemsBySellerId(currentUser.getUserId());
                    return new Response(true, "Fetched seller items.", sellerItems, type, correlationId);

                case PLACE_BID:
                    if (currentUser == null || currentUser.getRole() != Role.BUYER) {
                        return new Response(false, "Unauthorized or not logged in as Buyer.", null, type, correlationId);
                    }
                    Bid bid = (Bid) payload;
                    bid.setBidderId(currentUser.getUserId());
                    Bid placedBid = auctionService.placeBid(bid);
                    if (placedBid != null) {
                        return new Response(true, "Bid placed successfully.", placedBid, type, correlationId);
                    } else {
                        return new Response(false, "Failed to place bid. It might be too low or auction ended.", null, type, correlationId);
                    }
                case GET_BIDS_FOR_AUCTION:
                    Integer auctionIdForBids = (Integer) payload;
                    List<Bid> bids = auctionService.getBidsForAuction(auctionIdForBids);
                    return new Response(true, "Fetched bids.", bids, type, correlationId);

                case GET_USER_PROFILE:
                    if (currentUser == null) {
                        return new Response(false, "Not logged in.", null, type, correlationId);
                    }
                    User profile = userService.getUserProfile(currentUser.getUserId());
                    if (profile != null) {
                        return new Response(true, "Profile fetched.", profile, type, correlationId);
                    } else {
                        return new Response(false, "Could not fetch profile.", null, type, correlationId);
                    }

                case UPDATE_USER_PROFILE:
                    if (currentUser == null) {
                        return new Response(false, "Not logged in.", null, type, correlationId);
                    }
                    if (payload instanceof String) {
                        String newEmail = (String) payload;
                        boolean success = userService.updateUserProfile(currentUser.getUserId(), newEmail);
                        if (success) {
                            currentUser.setEmail(newEmail);
                            return new Response(true, "Email updated successfully.", currentUser, type, correlationId);
                        } else {
                            return new Response(false, "Failed to update email. It might be taken or invalid.", null, type, correlationId);
                        }
                    } else {
                        return new Response(false, "Invalid payload for email update.", null, type, correlationId);
                    }

                case GET_MY_BIDS:
                    if (currentUser == null || currentUser.getRole() != Role.BUYER) {
                        return new Response(false, "Unauthorized or not a buyer.", null, type, correlationId);
                    }
                    List<Bid> myBids = auctionService.getBidsByUserId(currentUser.getUserId());
                    return new Response(true, "Fetched your bids.", myBids, type, correlationId);

                case GET_MY_WON_AUCTIONS:
                    if (currentUser == null || currentUser.getRole() != Role.BUYER) {
                        return new Response(false, "Unauthorized or not a buyer.", null, type, correlationId);
                    }
                    List<Auction> wonAuctions = auctionService.getWonAuctionsByUserId(currentUser.getUserId());
                    return new Response(true, "Fetched your won auctions.", wonAuctions, type, correlationId);

                case GET_MY_CREATED_AUCTIONS:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not a seller.", null, type, correlationId);
                    }
                    List<Auction> createdAuctions = auctionService.getAuctionsCreatedBySeller(currentUser.getUserId());
                    return new Response(true, "Fetched your created auctions.", createdAuctions, type, correlationId);

                case UPDATE_ITEM:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not a seller.", null, type, correlationId);
                    }
                    if (!(payload instanceof Item)) {
                        return new Response(false, "Invalid payload for update item.", null, type, correlationId);
                    }
                    Item itemToUpdate = (Item) payload;
                    boolean itemUpdated = itemService.updateItem(itemToUpdate, currentUser.getUserId());
                    return new Response(itemUpdated, itemUpdated ? "Item updated successfully." : "Failed to update item (e.g., in auction, or not owner).", itemUpdated ? itemToUpdate : null, type, correlationId);

                case CANCEL_UPCOMING_AUCTION:
                    if (currentUser == null || currentUser.getRole() != Role.SELLER) {
                        return new Response(false, "Unauthorized or not a seller.", null, type, correlationId);
                    }
                    if (!(payload instanceof Integer)) {
                        return new Response(false, "Invalid payload for cancel auction.", null, type, correlationId);
                    }
                    int auctionIdToCancel = (Integer) payload;
                    boolean auctionCancelled = auctionService.cancelUpcomingAuction(auctionIdToCancel, currentUser.getUserId());
                    return new Response(auctionCancelled, auctionCancelled ? "Auction cancelled successfully." : "Failed to cancel auction (not UPCOMING or not owner).", auctionIdToCancel, type, correlationId);
                case GET_ALL_USERS:
                    if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                        return new Response(false, "Unauthorized. Admin access required.", null, type, correlationId);
                    }
                    List<User> allUsers = userService.getAllUsers();
                    return new Response(true, "Fetched all users.", allUsers, type, correlationId);

                case GET_ALL_AUCTIONS:
                    if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                        return new Response(false, "Unauthorized. Admin access required.", null, type, correlationId);
                    }
                    List<Auction> allAuctions = auctionService.getAllAuctionsForAdmin();
                    return new Response(true, "Fetched all auctions.", allAuctions, type, correlationId);

                case SEND_MESSAGE:
                    if (currentUser == null) {
                        return new Response(false, "Not logged in.", null, type, correlationId);
                    }
                    if (!(payload instanceof Message)) {
                        return new Response(false, "Invalid payload for send message.", null, type, correlationId);
                    }
                    Message msgToSend = (Message) payload;
                    msgToSend.setSenderId(currentUser.getUserId());

                    if (msgToSend.getAuctionId() <= 0) {
                        return new Response(false, "Auction context (auctionId) is missing for the message.", null, type, correlationId);
                    }

                    if (msgToSend.getReceiverId() <= 0) {
                        Auction auctionContext = auctionService.getAuctionDetails(msgToSend.getAuctionId());
                        if (auctionContext == null || auctionContext.getItem() == null) {
                            System.err.println("ClientHandler: Auction context or item not found for initial message. AuctionID: " + msgToSend.getAuctionId());
                            return new Response(false, "Auction context not found for message.", null, type, correlationId);
                        }
                        int sellerOfItemId = auctionContext.getItem().getSellerId();
                        if (sellerOfItemId <= 0) {
                            System.err.println("ClientHandler: Invalid SellerID determined for initial message: " + sellerOfItemId);
                            return new Response(false, "Could not determine a valid seller for this item.", null, type, correlationId);
                        }
                        msgToSend.setReceiverId(sellerOfItemId);
                        System.out.println("ClientHandler SEND_MESSAGE (Initial): Determined ReceiverID (SellerOfItem)=" + sellerOfItemId);
                    } else {
                        System.out.println("ClientHandler SEND_MESSAGE (Reply): ReceiverID provided by client=" + msgToSend.getReceiverId());
                    }


                    System.out.println("ClientHandler SEND_MESSAGE: AuctionID=" + msgToSend.getAuctionId() +
                            ", SenderID=" + msgToSend.getSenderId() +
                            ", ReceiverID=" + msgToSend.getReceiverId() +
                            ", Text Preview=" + msgToSend.getMessageText().substring(0, Math.min(20, msgToSend.getMessageText().length())));

                    if (msgToSend.getSenderId() == msgToSend.getReceiverId()) {
                        System.err.println("ClientHandler: Sender is the same as receiver for message. Blocking.");
                        return new Response(false, "Cannot send message to yourself.", null, type, correlationId);
                    }

                    Message sentMessage = messageService.sendMessage(msgToSend);
                    if (sentMessage != null) {
                        notifyReceiverOfNewMessage(sentMessage);
                        return new Response(true, "Message sent.", sentMessage, type, correlationId);
                    } else {
                        return new Response(false, "Failed to send message (server error).", null, type, correlationId);
                    }

                case GET_MY_MESSAGES:
                    if (currentUser == null) {
                        return new Response(false, "Not logged in.", null, type, correlationId);
                    }
                    List<Message> myMessages = messageService.getMessagesForUser(currentUser.getUserId());
                    return new Response(true, "Fetched your messages.", myMessages, type, correlationId);

                case MARK_MESSAGE_AS_READ:
                    if (currentUser == null) {
                        return new Response(false, "Not logged in.", null, type, correlationId);
                    }
                    if (!(payload instanceof Integer)) {
                        return new Response(false, "Invalid payload for mark message read (expected messageId).", null, type, correlationId);
                    }
                    int messageIdToMark = (Integer) payload;
                    boolean marked = messageService.markMessageAsRead(messageIdToMark, currentUser.getUserId());
                    return new Response(marked, marked ? "Message marked as read." : "Failed to mark message as read (not receiver?).", null, type, correlationId);

                case PROCESS_PAYMENT:
                    if (currentUser == null || currentUser.getRole() != Role.BUYER) {
                        return new Response(false, "Only buyers can process payments.", null, type, correlationId);
                    }
                    if (!(payload instanceof Integer)) {
                        return new Response(false, "Invalid payload for payment processing.", null, type, correlationId);
                    }
                    int auctionIdToPay = (Integer) payload;
                    boolean paymentSuccess = auctionService.processPayment(auctionIdToPay, currentUser.getUserId());
                    if (paymentSuccess) {
                        return new Response(true, "Payment processed successfully.", auctionIdToPay, type, correlationId);
                    } else {
                        return new Response(false, "Payment processing failed. (Already paid, not winner, or server error).", auctionIdToPay, type, correlationId);
                    }

                case DELETE_USER:
                    if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                        return new Response(false, "Unauthorized. Admin access required.", null, type, correlationId);
                    }
                    if (!(payload instanceof Integer)) {
                        return new Response(false, "Invalid payload for delete user.", null, type, correlationId);
                    }
                    int userIdToDelete = (Integer) payload;
                    if (userIdToDelete == currentUser.getUserId()) {
                        return new Response(false, "Admin cannot delete themselves.", null, type, correlationId);
                    }
                    boolean userDeleted = userService.deleteUserAsAdmin(userIdToDelete);
                    return new Response(userDeleted, userDeleted ? "User deleted successfully." : "Failed to delete user.", userIdToDelete, type, correlationId);

                case DELETE_AUCTION:
                    if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
                        return new Response(false, "Unauthorized. Admin access required.", null, type, correlationId);
                    }
                    if (!(payload instanceof Integer)) {
                        return new Response(false, "Invalid payload for delete auction.", null, type, correlationId);
                    }
                    int auctionIdToDelete = (Integer) payload;
                    boolean auctionDeleted = auctionService.deleteAuctionAsAdmin(auctionIdToDelete);
                    return new Response(auctionDeleted, auctionDeleted ? "Auction deleted successfully." : "Failed to delete auction.", auctionIdToDelete, type, correlationId);

                default:
                    return new Response(false, "Unknown request type: " + type, null, type, correlationId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(false, "Server error processing request: " + e.getMessage(), null, type, correlationId);
        }
    }

    private void closeConnection() {
        handlerRunning = false;
        System.out.println("Closing connection for " + (currentUser != null ? currentUser.getUsername() : clientSocket.getInetAddress()));
        activeClientHandlers.remove(this);
        auctionService.unsubscribeClientFromAllAuctions(this);
        try {
            if (in != null) in.close();} catch (IOException e) {
            e.printStackTrace();
        }
        try { if (out != null) out.close();} catch (IOException e) {
            e.printStackTrace();
        }
        try { if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnectionGracefully() {
        handlerRunning = false;
        if (this.isAlive()) {
            this.interrupt();
        } else {
            closeConnection();
        }
    }

    public void sendNotification(Response notificationResponse) {
        if (out != null && handlerRunning && !clientSocket.isClosed()) {
            try {
                System.out.println("Server sending notification " + notificationResponse.getOriginalRequestType() + " to " + (currentUser != null ? currentUser.getUsername() : "client"));
                out.writeObject(notificationResponse);
                out.flush();
            } catch (IOException e) {
                System.err.println("Error sending notification to client " + (currentUser != null ? currentUser.getUsername() : "") + ": " + e.getMessage());
            }
        }
    }

    private void notifyReceiverOfNewMessage(Message message) {
        for (ClientHandler handler : activeClientHandlers) {
            if (handler.getCurrentUser() != null && handler.getCurrentUser().getUserId() == message.getReceiverId()) {
                Response msgNotification = new Response(true, "You have a new message from " + message.getSenderUsername(), message, RequestType.AUCTION_UPDATE);
                handler.sendNotification(msgNotification);
                System.out.println("Notified user " + message.getReceiverUsername() + " of new message.");
                break;
            }
        }
    }


}