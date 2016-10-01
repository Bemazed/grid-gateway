package io.dreamsphere.grid.gateway;

import java.net.Socket;

class TcpConnectionProvider implements ConnectionProvider {
    @Override
    public Connection get(Socket socket) {
        return new TcpConnection(socket);
    }
}
