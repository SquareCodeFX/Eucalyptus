package org.eucalyptus.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eucalyptus.common.PacketSerializer;
import org.eucalyptus.model.DataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Handler for processing incoming messages from clients.
 * Uses a thread pool to process requests asynchronously.
 */
public class ServerHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(ServerHandler.class);
    private final ExecutorService executorService;

    public ServerHandler(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) {
        // Process the message in a separate thread from the thread pool
        CompletableFuture.runAsync(() -> {
            try {
                // Deserialize the incoming message
                DataPacket packet = PacketSerializer.deserialize(msg);
                logger.info("Received packet: {}", packet);

                // Process the packet based on the operation
                if (packet.isRequiresResponse()) {
                    DataPacket response = processPacket(packet);
                    // Send the response back to the client
                    byte[] responseBytes = PacketSerializer.serialize(response);
                    ctx.writeAndFlush(responseBytes);
                } else {
                    // Just process the packet without sending a response
                    processPacket(packet);
                }
            } catch (Exception e) {
                logger.error("Error processing message", e);
            }
        }, executorService);
    }

    /**
     * Process the incoming packet and generate a response if needed.
     *
     * @param packet The incoming packet to process
     * @return A response packet if the original packet requires a response, null otherwise
     */
    private DataPacket processPacket(DataPacket packet) {
        // This is a simple example implementation that can be extended for real use cases
        String operation = packet.getOperation();
        List<Object> data = packet.getData();
        
        // If no response is required, just log and return null
        if (!packet.isRequiresResponse()) {
            logger.info("Processing packet with operation: {} (no response required)", operation);
            return null;
        }
        
        // Process the data based on the operation
        List<Object> resultData = new ArrayList<>();
        
        switch (operation) {
            case "CALCULATE_SUM":
                // Example: Calculate sum of numbers
                double sum = data.stream()
                        .filter(obj -> obj instanceof Number)
                        .mapToDouble(obj -> ((Number) obj).doubleValue())
                        .sum();
                resultData.add(sum);
                break;
                
            case "CALCULATE_AVERAGE":
                // Example: Calculate average of numbers
                double avg = data.stream()
                        .filter(obj -> obj instanceof Number)
                        .mapToDouble(obj -> ((Number) obj).doubleValue())
                        .average()
                        .orElse(0.0);
                resultData.add(avg);
                break;
                
            case "UPPERCASE":
                // Example: Convert strings to uppercase
                List<String> upperCaseStrings = new ArrayList<>();
                data.stream()
                        .filter(obj -> obj instanceof String)
                        .forEach(obj -> upperCaseStrings.add(((String) obj).toUpperCase()));
                resultData.addAll(upperCaseStrings);
                break;
                
            default:
                logger.warn("Unknown operation: {}", operation);
                resultData.add("Unknown operation: " + operation);
        }
        
        // Create and return the response packet
        return new DataPacket("RESPONSE_" + operation, resultData, false);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught in ServerHandler", cause);
        ctx.close();
    }
}
