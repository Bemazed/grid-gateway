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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TelnetConnectionTest {
    @Mock Socket mockSocket;
    @Mock InputStream mockInputStream;
    private ByteArrayOutputStream fakeOutputStream = new ByteArrayOutputStream();

    private TelnetConnection createTestConnection(byte[] buffer, int telnetBuffer) throws IOException {
        InputStream fakeInputStream = new ByteArrayInputStream(buffer);
        when(mockSocket.getInputStream()).thenReturn(fakeInputStream);
        when(mockSocket.getOutputStream()).thenReturn(fakeOutputStream);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);
        return new TelnetConnection(new TcpConnection(mockSocket), telnetBuffer);
    }

    private TelnetConnection createTestConnectionWithMockInput(int telnetBuffer) throws IOException {
        when(mockSocket.getInputStream()).thenReturn(mockInputStream);
        when(mockSocket.getOutputStream()).thenReturn(fakeOutputStream);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);
        return new TelnetConnection(new TcpConnection(mockSocket), telnetBuffer);
    }

    @Test
    public void readCanStreamNormalTextWithSmallBuffer() throws IOException {
        byte[] readBuffer = new byte[100];
        byte[] testMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,10);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(testMessage.length));
        assertThat(Arrays.copyOfRange(readBuffer, 0, testMessage.length), equalTo(testMessage));
    }

    @Test
    public void readCanStreamNormalTextWithLargeBuffer() throws IOException {
        byte[] readBuffer = new byte[100];
        byte[] testMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,100);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(testMessage.length));
        assertThat(Arrays.copyOfRange(readBuffer, 0, testMessage.length), equalTo(testMessage));
    }

    @Test
    public void readWillBlockIfUnderlyingStreamWouldToo() throws IOException {
        TelnetConnection telnetConnection = createTestConnectionWithMockInput(100);
        when(mockInputStream.available()).thenReturn(0);
        when(mockInputStream.read(any())).thenReturn(5);
        int actualBytesRead = telnetConnection.read(new byte[10]);
        assertThat(actualBytesRead, is(5));
    }

    @Test
    public void readWillHandleUnexpectedEndOfStream() throws IOException {
        byte[] readBuffer = new byte[10];
        TelnetConnection telnetConnection = createTestConnectionWithMockInput(100);
        when(mockInputStream.available()).thenReturn(5);
        when(mockInputStream.read(any())).thenReturn(5).thenReturn(-1);
        telnetConnection.read(readBuffer);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(-1));
    }

    @Test
    public void readAvailableReturnsCorrectEstimate() throws IOException {
        byte[] testMessage = "abc123".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,100);
        int actualAvailable = telnetConnection.readAvailable();
        assertThat(actualAvailable, is(testMessage.length));
    }

    @Test
    public void readDetectsEndOfStreamNotOnBufferBoundary() throws IOException {
        byte[] readBuffer = new byte[10];
        byte[] testMessage = "abc123".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,100);
        telnetConnection.read(readBuffer);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(-1));
    }

    @Test
    public void readDetectsEndOfStreamOnBufferBoundary() throws IOException {
        byte[] readBuffer = new byte[6];
        byte[] testMessage = "abc123".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,100);
        telnetConnection.read(readBuffer);
        int actualBytesRead = telnetConnection.read(readBuffer);
        assertThat(actualBytesRead, is(-1));
    }


    @Test
    public void readAvailableReturnsCorrectEstimateWhileBuffering() throws IOException {
        byte[] testMessage = "abc123".getBytes("UTF-8");
        TelnetConnection telnetConnection = createTestConnection(testMessage,100);
        telnetConnection.read(new byte[2]);
        int actualAvailable = telnetConnection.readAvailable();
        assertThat(actualAvailable, is(testMessage.length - 2));
    }

    @Test
    public void sendCanStreamNormalText() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{},10);
        byte[] expectedMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        telnetConnection.send(expectedMessage);
        byte[] actualMessage = fakeOutputStream.toByteArray();
        assertThat(actualMessage, equalTo(expectedMessage));
    }

    @Test
    public void sendCanSendPartialBuffer() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{},10);
        byte[] expectedMessage = "abc123[]!@#$%^&()-=_+ \t\nàèìòùÀÈÌÒÙáéíóúýÁÉÍÓÚÝ".getBytes("UTF-8");
        telnetConnection.send(expectedMessage, 10);
        byte[] actualMessage = fakeOutputStream.toByteArray();
        assertThat(actualMessage, equalTo(Arrays.copyOfRange(expectedMessage, 0, 10)));
    }


    @Test
    public void closeClosesSocket() throws IOException {
        TelnetConnection telnetConnection = createTestConnection(new byte[]{},10);
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

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetConfirmsUnsupportedClientOption() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresRedundantWontResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WILL,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresDuplicateWillResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WILL,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WILL,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresDuplicateWontResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetDeniesUnsupportedServerOption() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DO,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetConfirmsUnsupportedServerOption() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresRedundantDontResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DO,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresDuplicateDoResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DO,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DO,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }

    @Test
    public void telnetIgnoresDuplicateDontResponse() throws IOException {
        byte[] command = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES,
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_DONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };
        byte[] expectedResponse = {
                TelnetCodes.CMD_IAC,
                TelnetCodes.CMD_WONT,
                TelnetCodes.OPT_ENVIRONMENT_VARIABLES
        };

        TelnetConnection telnetConnection = createTestConnection(command,2);
        int bytesRead = telnetConnection.read(new byte[10]);
        assertThat(bytesRead, is(-1));
        assertThat(fakeOutputStream.toByteArray(), equalTo(expectedResponse));
    }
}
