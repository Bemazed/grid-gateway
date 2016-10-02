/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class TcpConnection implements Connection {
    private final Socket socket;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;

    TcpConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void send(byte[] rawMessage) throws IOException {
        send(rawMessage, rawMessage.length);
    }

    @Override
    public void send(byte[] message, int length) throws IOException {
        synchronized(socket) {
            getOutputStream().write(message, 0, length);
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        synchronized(socket) {
            return getInputStream().read(buffer);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized(socket) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
    }

    private OutputStream getOutputStream() throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Attempt to get output stream of a closed socket");
        }
        if (outputStream == null) {
            outputStream = socket.getOutputStream();
        }

        return outputStream;
    }

    private InputStream getInputStream() throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Attempt to get input stream of a closed socket");
        }
        if (inputStream == null) {
            inputStream = socket.getInputStream();
        }

        return inputStream;
    }
}
