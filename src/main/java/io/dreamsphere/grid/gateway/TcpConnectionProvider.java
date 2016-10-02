/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.net.Socket;

class TcpConnectionProvider implements ConnectionProvider {
    @Override
    public Connection get(Socket socket) {
        return new TcpConnection(socket);
    }
}
