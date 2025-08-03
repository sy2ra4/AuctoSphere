package com.university.auctionsystem.shared.protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private RequestType type;
    private Object payload;
    private String correlationId;

    public Request(RequestType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public RequestType getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}