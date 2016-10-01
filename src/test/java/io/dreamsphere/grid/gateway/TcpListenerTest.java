package io.dreamsphere.grid.gateway;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TcpListenerTest {
    @Mock private Socket mockSocket;
    @Mock private Connection mockConnection;
    @Mock private ServerSocket mockServerSocket;
    @Mock private ConnectionProvider mockConnectionProvider;

    private TcpListener tcpListener;

    private class TestModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(ServerSocket.class).toInstance(mockServerSocket);
            bind(ConnectionProvider.class).toInstance(mockConnectionProvider);
        }
    }

    @Before
    public void createTcpListener() {
        Injector injector = Guice.createInjector(new TestModule());
        tcpListener = injector.getInstance(TcpListener.class);
    }

    @Test
    public void openBindsServerSocket() throws IOException {
        int port = 1234;
        tcpListener.open(port);
        SocketAddress expectedSocketAddress = new InetSocketAddress(port);
        verify(mockServerSocket).bind(eq(expectedSocketAddress));
    }

    @Test(expected = IOException.class)
    public void openWhenSocketIsBoundThrowsIOException() throws IOException {
        when(mockServerSocket.isBound()).thenReturn(true);
        int port = 1234;
        tcpListener.open(port);
    }

    @Test
    public void closeClosesServerSocket() throws IOException {
        tcpListener.close();
        verify(mockServerSocket).close();
    }

    @Test
    public void closeWhenSocketIsClosedDoesNothing() throws IOException {
        when(mockServerSocket.isClosed()).thenReturn(true);
        tcpListener.close();
        verify(mockServerSocket, never()).close();
    }

    @Test
    public void getNextConnectionCallsServerSocketAccept() throws IOException {
        when(mockServerSocket.isBound()).thenReturn(true);
        when(mockServerSocket.isClosed()).thenReturn(false);
        tcpListener.getNextConnection();
        verify(mockServerSocket).accept();
    }

    @Test(expected = IOException.class)
    public void getNextConnectionThrowsIOExceptionIfSocketIsNotBound() throws IOException {
        when(mockServerSocket.isBound()).thenReturn(false);
        when(mockServerSocket.isClosed()).thenReturn(false);
        tcpListener.getNextConnection();
    }

    @Test(expected = IOException.class)
    public void getNextConnectionThrowsIOExceptionIfSocketIsClosed() throws IOException {
        when(mockServerSocket.isBound()).thenReturn(true);
        when(mockServerSocket.isClosed()).thenReturn(true);
        tcpListener.getNextConnection();
    }

    @Test
    public void getNextConnectionUsesConnectionProviderToCreateConnection() throws IOException {
        when(mockServerSocket.isBound()).thenReturn(true);
        when(mockServerSocket.isClosed()).thenReturn(false);
        when(mockServerSocket.accept()).thenReturn(mockSocket);
        when(mockConnectionProvider.get(mockSocket)).thenReturn(mockConnection);
        Connection actualConnection = tcpListener.getNextConnection();
        assertThat(actualConnection, is(mockConnection));
    }
}
