package org.eucalyptus.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.eucalyptus.common.PacketDecoder;
import org.eucalyptus.common.PacketEncoder;
import org.eucalyptus.common.PacketSerializer;
import org.eucalyptus.model.DataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The main client class for the Eucalyptus project.
 * This client uses Netty for efficient, non-blocking I/O and
 * provides methods for sending requests to the server and receiving responses asynchronously.
 */
public class EucalyptusClient {
    private static final Logger logger = LoggerFactory.getLogger(EucalyptusClient.class);
    private static final int RECONNECT_INTERVAL_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 12; // 1 minute (12 * 5 seconds)

    private final String host;
    private final int port;
    private EventLoopGroup group;
    private Channel channel;
    private ClientHandler clientHandler;
    private Bootstrap bootstrap;

    // Reconnection related fields
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private ScheduledExecutorService reconnectExecutor;
    private boolean autoReconnect = true;

    /**
     * Creates a new EucalyptusClient with the specified host and port.
     *
     * @param host The host to connect to
     * @param port The port to connect to
     */
    public EucalyptusClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to the server.
     *
     * @throws Exception If an error occurs while connecting
     */
    public void connect() throws Exception {
        // Reset reconnection state
        reconnectAttempts.set(0);
        reconnecting.set(false);

        // Initialize reconnect executor if needed
        if (reconnectExecutor == null || reconnectExecutor.isShutdown()) {
            reconnectExecutor = new ScheduledThreadPoolExecutor(1);
        }

        // Create the event loop group if needed
        if (group == null) {
            group = new NioEventLoopGroup();
        }

        // Create the client handler if needed
        if (clientHandler == null) {
            clientHandler = new ClientHandler();
            clientHandler.setClient(this);
        }

        try {
            // Create the client bootstrap
            bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new PacketDecoder(),
                                    new PacketEncoder(),
                                    clientHandler
                            );
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            // Connect to the server
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();

            logger.info("Connected to server at {}:{}", host, port);
        } catch (Exception e) {
            logger.error("Error connecting to server at {}:{}", host, port, e);

            // Don't disconnect if we're reconnecting, as it will shut down resources
            if (!reconnecting.get()) {
                disconnect();
                throw e;
            }
        }
    }

    /**
     * Disconnects from the server.
     */
    public void disconnect() {
        logger.info("Disconnecting from server...");

        // Stop any reconnection attempts
        reconnecting.set(false);
        reconnectAttempts.set(0);

        if (reconnectExecutor != null && !reconnectExecutor.isShutdown()) {
            reconnectExecutor.shutdownNow();
            reconnectExecutor = null;
        }

        // Close the channel
        if (channel != null) {
            channel.close();
            channel = null;
        }

        // Shut down the event loop group
        if (group != null) {
            group.shutdownGracefully(0, 5, TimeUnit.SECONDS);
            group = null;
        }

        logger.info("Disconnected from server");
    }

    /**
     * Handles connection loss by attempting to reconnect.
     * Will try to reconnect every 5 seconds for up to 1 minute.
     */
    public void handleConnectionLoss() {
        // If auto-reconnect is disabled or we're already reconnecting, do nothing
        if (!autoReconnect || reconnecting.getAndSet(true)) {
            return;
        }

        logger.info("Connection lost. Attempting to reconnect...");

        // Reset the channel
        channel = null;

        // Schedule reconnection attempts
        reconnectExecutor.scheduleAtFixedRate(() -> {
            try {
                // If we've reached the maximum number of attempts, stop reconnecting
                if (reconnectAttempts.incrementAndGet() > MAX_RECONNECT_ATTEMPTS) {
                    logger.warn("Failed to reconnect after {} attempts. Giving up.", MAX_RECONNECT_ATTEMPTS);
                    reconnecting.set(false);
                    reconnectExecutor.shutdown();
                    return;
                }

                logger.info("Reconnection attempt {}/{}", reconnectAttempts.get(), MAX_RECONNECT_ATTEMPTS);

                // Attempt to connect
                if (!isConnected()) {
                    try {
                        // Connect to the server
                        ChannelFuture future = bootstrap.connect(host, port).sync();
                        channel = future.channel();

                        logger.info("Successfully reconnected to server at {}:{}", host, port);

                        // Reset reconnection state
                        reconnecting.set(false);
                        reconnectAttempts.set(0);
                        reconnectExecutor.shutdown();
                    } catch (Exception e) {
                        logger.error("Failed to reconnect to server at {}:{}", host, port, e);
                    }
                }
            } catch (Exception e) {
                logger.error("Error during reconnection attempt", e);
            }
        }, 0, RECONNECT_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Returns whether the client is connected to the server.
     *
     * @return true if the client is connected, false otherwise
     */
    public boolean isConnected() {
        return channel != null && channel.isOpen();
    }

    /**
     * Enables or disables auto-reconnect functionality.
     * When enabled, the client will attempt to reconnect to the server if the connection is lost.
     * 
     * @param enable true to enable auto-reconnect, false to disable
     */
    public void setAutoReconnect(boolean enable) {
        this.autoReconnect = enable;
        logger.info("Auto-reconnect {}", enable ? "enabled" : "disabled");
    }

    /**
     * Returns whether auto-reconnect is enabled.
     * 
     * @return true if auto-reconnect is enabled, false otherwise
     */
    public boolean isAutoReconnectEnabled() {
        return autoReconnect;
    }

    /**
     * Sends a request to the server and returns a CompletableFuture that will be completed
     * when a response is received.
     *
     * @param operation The operation to perform
     * @param data The data to send
     * @return A CompletableFuture that will be completed with the response
     * @throws Exception If an error occurs while sending the request
     */
    public CompletableFuture<DataPacket> sendRequest(String operation, List<Object> data) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }

        // Create the request packet
        DataPacket request = new DataPacket(operation, data, true);

        // Create a future for the response
        CompletableFuture<DataPacket> responseFuture = new CompletableFuture<>();

        // Register the future with the client handler
        clientHandler.registerResponse(request.getId(), responseFuture);

        // Serialize and send the request
        byte[] requestBytes = PacketSerializer.serialize(request);
        channel.writeAndFlush(requestBytes);

        logger.debug("Sent request: {}", request);

        return responseFuture;
    }

    /**
     * Sends a one-way message to the server (no response expected).
     *
     * @param operation The operation to perform
     * @param data The data to send
     * @throws Exception If an error occurs while sending the message
     */
    public void sendOneWayMessage(String operation, List<Object> data) throws Exception {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }

        // Create the message packet
        DataPacket message = new DataPacket(operation, data, false);

        // Serialize and send the message
        byte[] messageBytes = PacketSerializer.serialize(message);
        channel.writeAndFlush(messageBytes);

        logger.debug("Sent one-way message: {}", message);
    }
}
