package io.dreamsphere.grid.gateway;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TcpConnectionTest {
    @Mock private Socket mockSocket;
    private final byte[] testBytes = { 1, 2, 3 };
    private ByteArrayInputStream testInputStream = new ByteArrayInputStream(testBytes);
    private ByteArrayOutputStream testOutputStream = new ByteArrayOutputStream();

    private TcpConnection tcpConnection;

    @Before
    public void setupMocks() throws IOException {
        when(mockSocket.getInputStream()).thenReturn(testInputStream);
        when(mockSocket.getOutputStream()).thenReturn(testOutputStream);
        when(mockSocket.isClosed()).thenReturn(false);
        tcpConnection = new TcpConnection(mockSocket);
    }

    @Test
    public void sendWritesToOutputStream() throws IOException {
        String testMessage = "abc123[]!@#$%^&()-=_+\n \tàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ";
        byte[] rawMessage = testMessage.getBytes("UTF-8");
        tcpConnection.send(rawMessage);
        byte[] actualMessage = testOutputStream.toByteArray();
        assertThat(actualMessage, equalTo(rawMessage));
    }

    @Test
    public void sendCanWritePartialBuffer() throws IOException {
        String testMessage = "abc123[]!@#$";
        byte[] rawMessage = testMessage.getBytes("UTF-8");
        tcpConnection.send(rawMessage,5);
        byte[] actualMessage = testOutputStream.toByteArray();
        assertThat(actualMessage, equalTo("abc12".getBytes()));
    }

    @Test
    public void closeClosesSocket() throws IOException {
        tcpConnection.close();
        verify(mockSocket).close();
    }

    @Test(expected= IOException.class)
    public void sendOnClosedSocketThrowsIOException() throws IOException {
        when(mockSocket.isClosed()).thenReturn(true);
        tcpConnection.send(new byte[1]);
    }

    @Test
    public void closingAnAlreadyClosedConnectionDoesNothing() throws IOException {
        when(mockSocket.isClosed()).thenReturn(true);
        tcpConnection.close();
        verify(mockSocket, never()).close();
    }

    @Test
    public void readConsumesSmallerBuffer() throws IOException {
        byte[] buffer = new byte[10];
        int actualBytesRead = tcpConnection.read(buffer);
        assertThat(actualBytesRead, is(3));
        assertThat(Arrays.copyOfRange(buffer, 0, 3), equalTo(testBytes));
    }

    @Test(expected= IOException.class)
    public void readOnClosedSocketThrowsIOException() throws IOException {
        when(mockSocket.isClosed()).thenReturn(true);
        tcpConnection.read(new byte[1]);
    }
}
