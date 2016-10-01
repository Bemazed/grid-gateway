package io.dreamsphere.grid.gateway;

import java.io.IOException;

interface Listener {
    void open(int port) throws IOException;
    void close() throws IOException;
    Connection getNextConnection() throws IOException;
}
