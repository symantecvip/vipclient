package com.symantec.vip.client;

/**
 * Created by shantanu_gattani
 */
public class PortT<T> {
    private T port;
    private UCPortType type;

    public T getPort() {
        return port;
    }

    public void setPort(T port) {
        this.port = port;
    }

    public UCPortType getType() {
        return type;
    }

    public void setType(UCPortType type) {
        this.type = type;
    }
}
