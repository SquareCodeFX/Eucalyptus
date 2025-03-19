package org.eucalyptus.example;

import org.eucalyptus.client.EucalyptusClient;
import org.eucalyptus.model.DataPacket;
import org.eucalyptus.server.EucalyptusServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Example demonstrating the auto-reconnect functionality of the Eucalyptus client.
 */
public class AutoReconnectExample {
    private static final Logger logger = LoggerFactory.getLogger(AutoReconnectExample.class);
    private static final int SERVER_PORT = 8080;
    
    public static void main(String[] args) {
        EucalyptusServer server = null;
        
        // Create and connect the client
        EucalyptusClient client = new EucalyptusClient("localhost", SERVER_PORT);
        
        try {
            // Start the server
            server = startServer();
            
            // Wait for the server to start
            Thread.sleep(2000);
            
            // Connect to the server and enable auto-reconnect
            client.connect();
            client.setAutoReconnect(true);
            logger.info("Client connected to server with auto-reconnect enabled");
            
            // Send a request to the server
            sendRequest(client, "Initial connection");
            
            // Stop the server to simulate connection loss
            logger.info("Stopping server to simulate connection loss...");
            server.stop();
            Thread.sleep(1000);
            
            // Try to send a request - this should fail
            try {
                sendRequest(client, "After server stopped");
            } catch (Exception e) {
                logger.info("Expected error: {}", e.getMessage());
            }
            
            // Restart the server
            logger.info("Restarting server...");
            server = startServer();
            
            // Wait for auto-reconnect to happen (should take less than 5 seconds)
            logger.info("Waiting for client to auto-reconnect...");
            Thread.sleep(7000);
            
            // Send another request - this should succeed after reconnection
            sendRequest(client, "After auto-reconnect");
            
            // Wait a bit to ensure the message is processed
            Thread.sleep(1000);
            
        } catch (Exception e) {
            logger.error("Error in example", e);
        } finally {
            // Disconnect the client
            if (client.isConnected()) {
                client.disconnect();
            }
            
            // Stop the server
            if (server != null) {
                server.stop();
            }
        }
    }
    
    /**
     * Starts a server on the specified port.
     *
     * @return The started server
     * @throws Exception If an error occurs while starting the server
     */
    private static EucalyptusServer startServer() throws Exception {
        EucalyptusServer server = new EucalyptusServer(SERVER_PORT);
        server.start();
        logger.info("Server started on port {}", SERVER_PORT);
        return server;
    }
    
    /**
     * Sends a test request to the server and logs the response.
     *
     * @param client The client to use
     * @param context A context string to include in the log message
     * @throws Exception If an error occurs while sending the request
     */
    private static void sendRequest(EucalyptusClient client, String context) throws Exception {
        List<Object> strings = Arrays.asList("hello", "world", "eucalyptus");
        CompletableFuture<DataPacket> future = client.sendRequest("UPPERCASE", strings);
        
        try {
            DataPacket response = future.get();
            logger.info("[{}] Received response: {}", context, response.getData());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("[{}] Error getting response", context, e);
            throw new Exception("Failed to get response: " + e.getMessage());
        }
    }
}
