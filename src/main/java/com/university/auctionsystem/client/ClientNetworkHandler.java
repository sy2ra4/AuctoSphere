package com.university.auctionsystem.client;

import com.university.auctionsystem.shared.protocol.Request;
import com.university.auctionsystem.shared.protocol.Response;
import com.university.auctionsystem.shared.protocol.RequestType;
import com.university.auctionsystem.shared.model.Auction;
import javafx.application.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.university.auctionsystem.client.utils.UIUtils;
import javafx.scene.control.Alert;

public class ClientNetworkHandler {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean connected = false;
    private Thread listenerThread;

    private Consumer<Auction> auctionUpdateConsumer;
    private final Map<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    public ClientNetworkHandler() {}

    public boolean connect() {
        if (connected) return true;
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            startServerListener();
            System.out.println("Connected to server.");
            return true;
        } catch (IOException e) {
            System.err.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
            connected = false;
            return false;
        }
    }

    public void setAuctionUpdateConsumer(Consumer<Auction> consumer) {
        this.auctionUpdateConsumer = consumer;
    }

    private void startServerListener() {
        listenerThread = new Thread(() -> {
            try {
                while (connected && socket != null && !socket.isClosed() && socket.isConnected()) {
                    Object receivedObject = in.readObject();
                    if (receivedObject instanceof Response) {
                        Response response = (Response) receivedObject;
                        String correlationId = response.getCorrelationId();

                        if (correlationId != null && pendingRequests.containsKey(correlationId)) {
                            CompletableFuture<Response> future = pendingRequests.remove(correlationId);
                            if (future != null) {
                                future.complete(response);
                            }
                        } else {
                            Platform.runLater(() -> handleServerPush(response));
                        }
                    } else {
                        System.err.println("Listener received unknown object type: " + receivedObject.getClass().getName());
                    }
                }
            } catch (SocketException | EOFException e) {
                System.out.println("Socket closed or EOF, listener stopping: " + e.getMessage());
                cleanupPendingRequests(new IOException("Connection to server lost: " + e.getMessage(), e));
                disconnect();
            }
            catch (IOException | ClassNotFoundException e) {
                if (connected) {
                    System.err.println("Error in server listener: " + e.getMessage());
                    e.printStackTrace();
                    cleanupPendingRequests(e);
                    disconnect();
                }
            } finally {
                System.out.println("Server listener thread finished.");
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleServerPush(Response response) {
        Auction auctionData = null;
        if (response.getData() instanceof Auction) {
            auctionData = (Auction) response.getData();
        }

        switch (response.getOriginalRequestType()) {
            case AUCTION_UPDATE:
                if (auctionData != null) {
                    ClientStateManager.getInstance().handleAuctionUpdate(auctionData);
                }
                break;
            case OUTBID_NOTIFICATION:
                String itemNameOutbid = (auctionData != null && auctionData.getItem() != null) ? auctionData.getItem().getName() : "an item";
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Outbid!",
                        "You have been outbid on " + itemNameOutbid + ".\nCurrent bid: " + (auctionData != null ? auctionData.getCurrentHighestBid() : "N/A"));
                break;
            case WINNER_NOTIFICATION:
                String itemNameWon = (auctionData != null && auctionData.getItem() != null) ? auctionData.getItem().getName() : "an item";
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Congratulations!",
                        "You won the auction for " + itemNameWon + "!\nFinal Price: " + (auctionData != null ? auctionData.getCurrentHighestBid() : "N/A"));
                if (ClientApp.getInstance().getMainDashboardController() != null && ClientApp.getInstance().getBuyerDashboardController() != null) {
                    ClientApp.getInstance().getBuyerDashboardController().loadBuyerData();
                }
                break;
            case AUCTION_ENDED_SELLER_NOTIFICATION:
                UIUtils.showAlert(Alert.AlertType.INFORMATION, "Auction Ended", response.getMessage());
                if (ClientApp.getInstance().getMainDashboardController() != null && ClientApp.getInstance().getSellerDashboardController() != null) {
                    ClientApp.getInstance().getSellerDashboardController().loadSellerData();
                }
                break;
            default:
                System.out.println("Client listener received unhandled server push type: " + response.getOriginalRequestType());
                break;
        }
    }

    private void cleanupPendingRequests(Exception e) {
        for (Map.Entry<String, CompletableFuture<Response>> entry : new ConcurrentHashMap<>(pendingRequests).entrySet()) {
            entry.getValue().completeExceptionally(e);
            pendingRequests.remove(entry.getKey());
        }
    }


    public CompletableFuture<Response> sendRequestAsync(Request request) {
        if (!connected) {
            if (!connect()) {
                return CompletableFuture.failedFuture(new IOException("Failed to connect to server."));
            }
        }
        if (out == null) {
            return CompletableFuture.failedFuture(new IOException("Output stream not initialized. Cannot send request."));
        }


        CompletableFuture<Response> future = new CompletableFuture<>();
        String correlationId = UUID.randomUUID().toString();
        request.setCorrelationId(correlationId);
        pendingRequests.put(correlationId, future);

        System.out.println("Client sending request: " + request.getType() + " (CorrID: " + correlationId + ")");
        try {
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            System.err.println("Error sending request " + request.getType() + ": " + e.getMessage());
            e.printStackTrace();
            pendingRequests.remove(correlationId);
            future.completeExceptionally(e);
            disconnect();
        }
        return future;
    }

    public void disconnect() {
        if (connected) {
            System.out.println("Disconnecting from server...");
            connected = false;
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (out != null) {
                try {
                    Request disconnectRequest = new Request(RequestType.DISCONNECT, null);
                    disconnectRequest.setCorrelationId(UUID.randomUUID().toString());
                    out.writeObject(disconnectRequest);
                    out.flush();
                    System.out.println("Sent DISCONNECT request.");
                } catch (IOException e) {
                    System.err.println("Error sending disconnect message (socket might already be closed): " + e.getMessage());
                }
            }
            try {
                if (in != null) in.close();
            } catch (IOException e) {
                System.err.println("Error closing input stream: " + e.getMessage());
            }
            try {
                if (out != null) out.close();
            } catch (IOException e) {
                System.err.println("Error closing output stream: " + e.getMessage());
            }
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
            cleanupPendingRequests(new IOException("Client disconnected."));

            socket = null;
            in = null;
            out = null;
            System.out.println("Disconnected.");
        }
    }

    public boolean isConnected() {
        return connected;
    }
}