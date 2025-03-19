package org.eucalyptus.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * A data packet that can be sent between client and server.
 * This class is serializable to allow for easy transmission over the network.
 */
public class DataPacket implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String operation;
    private List<Object> data;
    private boolean requiresResponse;
    
    // Default constructor for serialization
    public DataPacket() {
        this.id = UUID.randomUUID();
    }
    
    public DataPacket(String operation, List<Object> data, boolean requiresResponse) {
        this.id = UUID.randomUUID();
        this.operation = operation;
        this.data = data;
        this.requiresResponse = requiresResponse;
    }
    
    // Getters and setters
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public List<Object> getData() {
        return data;
    }
    
    public void setData(List<Object> data) {
        this.data = data;
    }
    
    public boolean isRequiresResponse() {
        return requiresResponse;
    }
    
    public void setRequiresResponse(boolean requiresResponse) {
        this.requiresResponse = requiresResponse;
    }
    
    @Override
    public String toString() {
        return "DataPacket{" +
                "id=" + id +
                ", operation='" + operation + '\'' +
                ", data=" + data +
                ", requiresResponse=" + requiresResponse +
                '}';
    }
}
