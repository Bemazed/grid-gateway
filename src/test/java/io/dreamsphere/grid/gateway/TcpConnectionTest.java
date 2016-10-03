/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

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
    public void readConsumesEntireBuffer() throws IOException {
        byte[] buffer = new byte[10];
        int actualBytesRead = tcpConnection.read(buffer);
        assertThat(actualBytesRead, is(3));
        assertThat(Arrays.copyOfRange(buffer, 0, 3), equalTo(testBytes));
    }

    @Test
    public void readCanReadPartialBuffer() throws IOException {
        byte[] buffer = new byte[2];
        int actualBytesRead = tcpConnection.read(buffer);
        assertThat(actualBytesRead, is(2));
        assertThat(buffer, equalTo(Arrays.copyOfRange(testBytes,0,2)));
        actualBytesRead = tcpConnection.read(buffer);
        assertThat(actualBytesRead, is(1));
        assertThat(buffer[0], equalTo(testBytes[2]));
    }

    @Test
    public void readDetectsEndOfStreamByReturningNegativeOne() throws IOException {
        byte[] buffer = new byte[10];
        tcpConnection.read(buffer);
        int actualBytesRead = tcpConnection.read(buffer);
        assertThat(actualBytesRead, is(-1));
    }

    @Test(expected= IOException.class)
    public void readOnClosedSocketThrowsIOException() throws IOException {
        when(mockSocket.isClosed()).thenReturn(true);
        tcpConnection.read(new byte[1]);
    }

    @Test
    public void readAvailableReturnsCorrectEstimateWhenDataAvailable() throws IOException {
        int actualAvailable = tcpConnection.readAvailable();
        assertThat(actualAvailable, is(3));
    }

    @Test
    public void readAvailableReturnsCorrectEstimateWhenDataNotAvailable() throws IOException {
        tcpConnection.read(new byte[10]);
        int actualAvailable = tcpConnection.readAvailable();
        assertThat(actualAvailable, is(0));
    }

    @Test(expected= IOException.class)
    public void readAvailableOnClosedSocketThrowsIOException() throws IOException {
        when(mockSocket.isClosed()).thenReturn(true);
        tcpConnection.readAvailable();
    }
}