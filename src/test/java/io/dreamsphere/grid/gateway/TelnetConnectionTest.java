/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TelnetConnectionTest {
    @Mock
    Socket mockSocket;
    private ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream();

    private TelnetConnection createTestConnection(byte[] buffer) throws IOException {
        InputStream fakeInputStream = new ByteArrayInputStream(buffer);
        when(mockSocket.getInputStream()).thenReturn(fakeInputStream);
        when(mockSocket.getOutputStream()).thenReturn(fakeOutputStream);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);
        return new TelnetConnection(new TcpConnection(mockSocket));
    }

    @Test
    public void readCanStreamNormalText() throws IOException {
        byte[] readBuffer = new byte[100];
        byte[] testMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(testMessage.length));
        assertThat(Arrays.copyOfRange(readBuffer, 0, testMessage.length), equalTo(testMessage));
    }

    @Test
    public void sendCanStreamNormalText() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{});
        byte[] expectedMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        telnetConnection.send(expectedMessage);
        byte[] actualMessage = fakeOutputStream.toByteArray();
        assertThat(actualMessage, equalTo(expectedMessage));
    }

    @Test
    public void sendCanSendPartialBuffer() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{});
        byte[] expectedMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        telnetConnection.send(expectedMessage, 10);
        byte[] actualMessage = fakeOutputStream.toByteArray();
        assertThat(actualMessage, equalTo(Arrays.copyOfRange(expectedMessage, 0, 10)));
    }


    @Test
    public void closeClosesSocket() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{});
        telnetConnection.close();
        verify(mockSocket).close();
    }

    @Test
    public void telnetDeniesUnsupportedClientOption() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WILL,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(0));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }
}
