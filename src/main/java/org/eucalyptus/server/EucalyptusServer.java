package org.eucalyptus.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.eucalyptus.common.PacketDecoder;
import org.eucalyptus.common.PacketEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The main server class for the Eucalyptus project.
 * This server uses Netty for efficient, non-blocking I/O and
 * a separate thread pool for processing client requests.
 */
public class EucalyptusServer {
    private static final Logger logger = LoggerFactory.getLogger(EucalyptusServer.class);
    
    private final int port;
    private final int processingThreads;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ExecutorService processingExecutor;
    private ChannelFuture serverChannel;
    
    /**
     * Creates a new EucalyptusServer with the specified port and number of processing threads.
     *
     * @param port The port to listen on
     * @param processingThreads The number of threads to use for processing requests
     */
    public EucalyptusServer(int port, int processingThreads) {
        this.port = port;
        this.processingThreads = processingThreads;
    }
    
    /**
     * Creates a new EucalyptusServer with the specified port and default number of processing threads.
     * The default number of processing threads is the number of available processors.
     *
     * @param port The port to listen on
     */
    public EucalyptusServer(int port) {
        this(port, Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Starts the server.
     *
     * @throws Exception If an error occurs while starting the server
     */
    public void start() throws Exception {
        // Create the thread pools
        bossGroup = new NioEventLoopGroup(1); // Boss group with a single thread
        workerGroup = new NioEventLoopGroup(); // Worker group with default number of threads
        processingExecutor = Executors.newFixedThreadPool(processingThreads);
        
        try {
            // Create the server bootstrap
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(
                                    new PacketDecoder(),
                                    new PacketEncoder(),
                                    new ServerHandler(processingExecutor)
                            );
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            // Bind and start to accept incoming connections
            serverChannel = bootstrap.bind(port).sync();
            
            logger.info("EucalyptusServer started on port {} with {} processing threads", port, processingThreads);
            
            // Wait until the server socket is closed
            // This will block until the server is shut down
            // serverChannel.channel().closeFuture().sync();
        } catch (Exception e) {
            logger.error("Error starting EucalyptusServer", e);
            stop();
            throw e;
        }
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        logger.info("Stopping EucalyptusServer...");
        
        // Close the server channel
        if (serverChannel != null) {
            serverChannel.channel().close();
            serverChannel = null;
        }
        
        // Shut down the thread pools
        if (bossGroup != null) {
            bossGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
            bossGroup = null;
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
            workerGroup = null;
        }
        
        if (processingExecutor != null) {
            processingExecutor.shutdown();
            try {
                if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    processingExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                processingExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            processingExecutor = null;
        }
        
        logger.info("EucalyptusServer stopped");
    }
    
    /**
     * Returns whether the server is running.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.channel().isOpen();
    }
    
    /**
     * Main method for running the server as a standalone application.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        int port = 8080; // Default port
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number: {}", args[0]);
                System.exit(1);
            }
        }
        
        // Create and start the server
        EucalyptusServer server = new EucalyptusServer(port);
        try {
            server.start();
            
            // Add shutdown hook to stop the server when the JVM is shutting down
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
            
            // Wait for the server to be stopped
            while (server.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error running EucalyptusServer", e);
            System.exit(1);
        }
    }
}
