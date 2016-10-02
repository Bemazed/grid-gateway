/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.io.IOException;

class TelnetConnection implements Connection {
    private final Connection connection;

    TelnetConnection(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void send(byte[] message) throws IOException {
        connection.send(message);
    }

    @Override
    public void send(byte[] message, int length) throws IOException {
        connection.send(message, length);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return connection.read(buffer);
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}
