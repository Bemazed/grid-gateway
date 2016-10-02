/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.io.IOException;

interface Listener {
    void open(int port) throws IOException;
    void close() throws IOException;
    Connection getNextConnection() throws IOException;
}
