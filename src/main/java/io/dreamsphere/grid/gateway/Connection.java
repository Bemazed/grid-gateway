/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.io.IOException;

interface Connection {
    void send(byte[] message) throws IOException;
    void send(byte[] message, int length) throws IOException;
    int read(byte[] buffer) throws IOException;
    void close() throws IOException;
}
