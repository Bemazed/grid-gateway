package io.dreamsphere.grid.gateway;

import java.io.IOException;

public class TelnetConnection implements Connection {
    TelnetConnection(Connection connection) {

    }

    @Override
    public void send(byte[] message) throws IOException {

    }

    @Override
    public void send(byte[] message, int length) throws IOException {

    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return 0;
    }

    @Override
    public void close() throws IOException {

    }
}
