package org.eucalyptus.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.eucalyptus.common.PacketSerializer;
import org.eucalyptus.model.DataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handler for processing responses from the server.
 */
public class ClientHandler extends SimpleChannelInboundHandler<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    // Map of request IDs to CompletableFutures that will be completed when a response is received
    private final Map<UUID, CompletableFuture<DataPacket>> responseMap = new ConcurrentHashMap<>();

    // Reference to the client for notifying about connection events
    private EucalyptusClient client;

    /**
     * Creates a new ClientHandler without a client reference.
     */
    public ClientHandler() {
        this(null);
    }

    /**
     * Creates a new ClientHandler with a client reference.
     * 
     * @param client The client to notify about connection events
     */
    public ClientHandler(EucalyptusClient client) {
        this.client = client;
    }

    /**
     * Sets the client reference.
     * 
     * @param client The client to notify about connection events
     */
    public void setClient(EucalyptusClient client) {
        this.client = client;
    }

    /**
     * Registers a CompletableFuture for a request ID.
     * The future will be completed when a response with the same ID is received.
     *
     * @param requestId The ID of the request
     * @param future The future to complete when a response is received
     */
    public void registerResponse(UUID requestId, CompletableFuture<DataPacket> future) {
        responseMap.put(requestId, future);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
        // Deserialize the response
        DataPacket response = PacketSerializer.deserialize(msg);
        logger.debug("Received response: {}", response);

        // Complete the future for this response
        CompletableFuture<DataPacket> future = responseMap.remove(response.getId());
        if (future != null) {
            future.complete(response);
        } else {
            logger.warn("Received response for unknown request ID: {}", response.getId());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught in ClientHandler", cause);

        // Complete all pending futures with an exception
        for (Map.Entry<UUID, CompletableFuture<DataPacket>> entry : responseMap.entrySet()) {
            entry.getValue().completeExceptionally(cause);
        }
        responseMap.clear();

        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Connection to server closed");

        // Complete all pending futures with an exception
        for (Map.Entry<UUID, CompletableFuture<DataPacket>> entry : responseMap.entrySet()) {
            entry.getValue().completeExceptionally(new RuntimeException("Connection closed"));
        }
        responseMap.clear();

        // Notify the client about the connection loss
        if (client != null) {
            client.handleConnectionLoss();
        }

        ctx.fireChannelInactive();
    }
}
