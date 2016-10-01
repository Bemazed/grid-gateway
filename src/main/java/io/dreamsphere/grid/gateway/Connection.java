package io.dreamsphere.grid.gateway;

import java.io.IOException;

interface Connection {
    void send(byte[] message) throws IOException;
    void send(byte[] message, int length) throws IOException;
    void close() throws IOException;
    int read(byte[] buffer) throws IOException;
}
