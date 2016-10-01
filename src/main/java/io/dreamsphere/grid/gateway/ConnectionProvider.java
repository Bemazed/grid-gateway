package io.dreamsphere.grid.gateway;

import java.net.Socket;

interface ConnectionProvider {
    Connection get(Socket socket);
}