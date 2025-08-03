package com.university.auctionsystem.shared.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;
    private RequestType originalRequestType;
    private String correlationId;

    public Response(boolean success, String message, Object data, RequestType originalRequestType) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.originalRequestType = originalRequestType;
    }

    public Response(boolean success, String message, Object data, RequestType originalRequestType, String correlationId) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.originalRequestType = originalRequestType;
        this.correlationId = correlationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }

    public RequestType getOriginalRequestType() {
        return originalRequestType;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}