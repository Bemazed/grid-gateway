/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import java.io.IOException;

public class Gateway {
    private static final Injector INJECTOR = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
            bind(ConnectionProvider.class).to(TcpConnectionProvider.class);
            bind(Listener.class).to(TcpListener.class);
        }
    });

    public static void main(String[] args) throws IOException {
        Gateway gateway = INJECTOR.getInstance(Gateway.class);
        gateway.run();
    }

    private final Listener listener;

    @Inject
    private Gateway(Listener listener) {
        this.listener = listener;
    }

    private void run() throws IOException {
        listener.open(9000);
        TelnetConnection telnetConnection = new TelnetConnection(listener.getNextConnection());
        System.out.println("Connection established");
        telnetConnection.send("Welcome!\n\r".getBytes("UTF-8"));
        byte[] inputBuffer = new byte[100];
        int bytesRead;

        do {
            bytesRead = telnetConnection.read(inputBuffer);

            if (bytesRead > 0) {
                String input = new String(inputBuffer, 0, bytesRead, "UTF-8");
                System.out.println("Received: [" + input + "]");
                telnetConnection.send("I agree!\n\r".getBytes("UTF-8"));
            }
        } while(bytesRead >= 0);

        System.out.println("Closing connection");
        telnetConnection.close();
    }
}
