/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import com.google.inject.Inject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class TcpListener implements Listener {
    private final ServerSocket serverSocket;
    private final ConnectionProvider connectionProvider;

    @Inject
    TcpListener(ServerSocket serverSocket, ConnectionProvider connectionProvider) {
        this.serverSocket = serverSocket;
        this.connectionProvider = connectionProvider;
    }

    @Override
    public void open(int port) throws IOException {
        synchronized(serverSocket) {
            if (serverSocket.isBound()) {
                throw new IOException("ServerSocket is already bound");
            }

            serverSocket.bind(new InetSocketAddress(port));
        }
    }

    @Override
    public void close() throws IOException {
        synchronized(serverSocket) {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    @Override
    public Connection getNextConnection() throws IOException {
        synchronized(serverSocket) {
            if (!serverSocket.isBound()) {
                throw new IOException("Attempt to listen on an unbound socket");
            }

            if (serverSocket.isClosed()) {
                throw new IOException("Attempt to listen on a closed socket");
            }

            Socket socket = serverSocket.accept();
            return connectionProvider.get(socket);
        }
    }
}
