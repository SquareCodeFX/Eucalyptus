# Eucalyptus

Eucalyptus is a high-performance, asynchronous client-server framework built on Netty for efficient data processing and communication.

## Features

- **Asynchronous Communication**: Send requests and receive responses asynchronously
- **Multithreaded Processing**: Efficient multithreaded processing of client requests
- **Flexible Data Model**: Send and receive any Java objects (serialized as JSON)
- **Robust Error Handling**: Comprehensive error handling and logging
- **Simple API**: Easy-to-use client and server APIs
- **Auto-Reconnect**: Automatic reconnection to the server if the connection is lost
- **Scalable Architecture**: Designed to handle many concurrent connections

## Architecture

Eucalyptus consists of two main components:

1. **EucalyptusServer**: A multithreaded Netty-based server that can process client requests efficiently
2. **EucalyptusClient**: A non-blocking client that can send requests to the server and receive responses asynchronously

## Installation

### Gradle

Add the following to your `build.gradle` file:

```gradle
dependencies {
    implementation 'org.example:eucalyptus:1.0-SNAPSHOT'
}
```

### Maven

Add the following to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>eucalyptus</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Manual Installation

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/eucalyptus.git
   ```

2. Build the project:
   ```
   cd eucalyptus
   ./gradlew build
   ```

3. Add the generated JAR file to your project's classpath.

## Getting Started

### Server Setup

```java
// Create and start the server on port 8080
EucalyptusServer server = new EucalyptusServer(8080);
server.start();

// Add shutdown hook to stop the server when the JVM shuts down
Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
```

### Client Usage

```java
// Create and connect the client
EucalyptusClient client = new EucalyptusClient("localhost", 8080);
client.connect();

try {
    // Send a request and get a CompletableFuture for the response
    List<Object> numbers = Arrays.asList(1, 2, 3, 4, 5);
    CompletableFuture<DataPacket> future = client.sendRequest("CALCULATE_SUM", numbers);

    // Process the response when it arrives
    future.thenAccept(response -> {
        System.out.println("Sum: " + response.getData().get(0));
    });

    // Or wait for the response synchronously
    DataPacket response = future.get();
    System.out.println("Sum: " + response.getData().get(0));

    // Send a one-way message (no response expected)
    client.sendOneWayMessage("LOG", Arrays.asList("This is a log message"));
} finally {
    // Disconnect the client when done
    client.disconnect();
}
```

## Supported Operations

The server currently supports the following operations:

- **CALCULATE_SUM**: Calculate the sum of a list of numbers
- **CALCULATE_AVERAGE**: Calculate the average of a list of numbers
- **UPPERCASE**: Convert a list of strings to uppercase

You can easily extend the server to support additional operations by modifying the `processPacket` method in the `ServerHandler` class.

## Custom Data Processing

To implement custom data processing on the server:

1. Modify the `processPacket` method in `ServerHandler.java`
2. Add your custom operation to the switch statement
3. Process the data as needed
4. Return a response packet with the results

Example:

```java
case "MY_CUSTOM_OPERATION":
    // Process the data
    List<Object> resultData = new ArrayList<>();
    // ... custom processing logic ...
    resultData.add(result);
    break;
```

## Auto-Reconnect Feature

Eucalyptus client provides an auto-reconnect feature that automatically attempts to reconnect to the server if the connection is lost.

```java
// Create and connect the client
EucalyptusClient client = new EucalyptusClient("localhost", 8080);
client.connect();

// Enable auto-reconnect (enabled by default)
client.setAutoReconnect(true);

// Check if auto-reconnect is enabled
boolean isAutoReconnectEnabled = client.isAutoReconnectEnabled();
```

When auto-reconnect is enabled, the client will:
- Detect connection loss automatically
- Attempt to reconnect every 5 seconds
- Continue reconnection attempts for up to 1 minute (configurable)
- Automatically restore the connection when the server becomes available again

## Examples

The project includes several example applications that demonstrate how to use Eucalyptus:

### Basic Example

`EucalyptusExample.java` demonstrates basic client-server communication, including:
- Starting a server
- Connecting a client
- Sending requests and receiving responses
- Sending one-way messages

### Auto-Reconnect Example

`AutoReconnectExample.java` demonstrates the auto-reconnect feature:
- Connecting to a server with auto-reconnect enabled
- Handling server disconnection and reconnection
- Continuing operation after reconnection

## API Documentation

### EucalyptusClient

The main client class for communicating with the server.

#### Constructor

```java
// Create a client for the specified host and port
EucalyptusClient(String host, int port)
```

#### Methods

```java
// Connect to the server
void connect() throws Exception

// Disconnect from the server
void disconnect()

// Check if connected to the server
boolean isConnected()

// Enable or disable auto-reconnect
void setAutoReconnect(boolean enable)

// Check if auto-reconnect is enabled
boolean isAutoReconnectEnabled()

// Send a request and get a future for the response
CompletableFuture<DataPacket> sendRequest(String operation, List<Object> data) throws Exception

// Send a one-way message (no response expected)
void sendOneWayMessage(String operation, List<Object> data) throws Exception
```

### EucalyptusServer

The main server class for processing client requests.

#### Constructors

```java
// Create a server with the specified port and default thread count
EucalyptusServer(int port)

// Create a server with the specified port and thread count
EucalyptusServer(int port, int processingThreads)
```

#### Methods

```java
// Start the server
void start() throws Exception

// Stop the server
void stop()

// Check if the server is running
boolean isRunning()
```

### DataPacket

The data model class for communication between client and server.

```java
// Create a new data packet
DataPacket(String operation, List<Object> data, boolean requiresResponse)

// Get/set the packet ID
UUID getId()
void setId(UUID id)

// Get/set the operation
String getOperation()
void setOperation(String operation)

// Get/set the data
List<Object> getData()
void setData(List<Object> data)

// Get/set whether the packet requires a response
boolean isRequiresResponse()
void setRequiresResponse(boolean requiresResponse)
```

## Configuration Options

### Client Configuration

- **Host and Port**: Specify the server host and port when creating the client
- **Auto-Reconnect**: Enable or disable automatic reconnection to the server
- **Reconnect Interval**: Fixed at 5 seconds between reconnection attempts
- **Max Reconnect Attempts**: Fixed at 12 attempts (1 minute total)

### Server Configuration

- **Port**: Specify the port to listen on when creating the server
- **Processing Threads**: Specify the number of threads to use for processing requests (defaults to available processors)

## Requirements

- Java 8 or higher
- Netty 4.1.x
- Jackson 2.x for JSON serialization/deserialization
- SLF4J and Logback for logging

## Contributing

Contributions are welcome! Here's how you can contribute:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/my-new-feature`
5. Submit a pull request

### Coding Standards

- Follow Java coding conventions
- Write unit tests for new features
- Update documentation for changes

## License

This project is licensed under the MIT License - see the LICENSE file for details.
