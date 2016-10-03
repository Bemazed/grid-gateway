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
    private final OutputStream outputStream;
    private final InputStream inputStream;

    TcpConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
    }

    @Override
    public void send(byte[] rawMessage) throws IOException {
        send(rawMessage, rawMessage.length);
    }

    @Override
    public void send(byte[] message, int length) throws IOException {
        synchronized(outputStream) {
            verifySocketOpen();
            outputStream.write(message, 0, length);
        }
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        synchronized(inputStream) {
            verifySocketOpen();
            return inputStream.read(buffer);
        }
    }

    @Override
    public int readAvailable() throws IOException {
        synchronized(inputStream) {
            verifySocketOpen();
            return inputStream.available();
        }
    }

    @Override
    public void close() throws IOException {
        synchronized(socket) {
            synchronized(inputStream) {
                synchronized (outputStream) {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                }
            }
        }
    }

    private void verifySocketOpen() throws IOException {
        if (socket.isClosed()) {
            throw new IOException("Attempt to interact with a closed socket");
        }
    }
}
