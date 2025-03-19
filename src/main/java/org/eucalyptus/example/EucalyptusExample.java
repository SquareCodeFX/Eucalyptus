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
 * Example usage of the Eucalyptus client and server.
 */
public class EucalyptusExample {
    private static final Logger logger = LoggerFactory.getLogger(EucalyptusExample.class);
    
    public static void main(String[] args) {
        // Start the server in a separate thread
        Thread serverThread = new Thread(() -> {
            try {
                EucalyptusServer server = new EucalyptusServer(8080);
                server.start();
                
                // Keep the server running until the JVM shuts down
                Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
                
                while (server.isRunning()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("Error running server", e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        
        // Wait for the server to start
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        // Create and connect the client
        EucalyptusClient client = new EucalyptusClient("localhost", 8080);
        try {
            // Connect to the server
            client.connect();
            
            // Example 1: Calculate sum of numbers
            List<Object> numbers = Arrays.asList(1, 2, 3, 4, 5);
            CompletableFuture<DataPacket> sumFuture = client.sendRequest("CALCULATE_SUM", numbers);
            
            // Example 2: Calculate average of numbers
            CompletableFuture<DataPacket> avgFuture = client.sendRequest("CALCULATE_AVERAGE", numbers);
            
            // Example 3: Convert strings to uppercase
            List<Object> strings = Arrays.asList("hello", "world", "eucalyptus");
            CompletableFuture<DataPacket> upperFuture = client.sendRequest("UPPERCASE", strings);
            
            // Wait for all responses and print the results
            try {
                DataPacket sumResponse = sumFuture.get();
                logger.info("Sum result: {}", sumResponse.getData().get(0));
                
                DataPacket avgResponse = avgFuture.get();
                logger.info("Average result: {}", avgResponse.getData().get(0));
                
                DataPacket upperResponse = upperFuture.get();
                logger.info("Uppercase result: {}", upperResponse.getData());
                
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error getting response", e);
            }
            
            // Example 4: Send a one-way message (no response expected)
            client.sendOneWayMessage("LOG", Arrays.asList("This is a log message"));
            
            // Wait a bit to ensure the message is processed
            Thread.sleep(1000);
            
        } catch (Exception e) {
            logger.error("Error in client", e);
        } finally {
            // Disconnect the client
            if (client.isConnected()) {
                client.disconnect();
            }
        }
    }
}
