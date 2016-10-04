/* Copyright (C) Ben Maizels - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Ben Maizels <ben@dreamsphere.io>, October 2016
 */

package io.dreamsphere.grid.gateway;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class TelnetConnection implements Connection {
    private static final int DEFAULT_READ_BUFFER_SIZE = 1024;

    private enum State {
        READY,
        IAC_RECEIVED,
        DO_RECEIVED,
        DONT_RECEIVED,
        WILL_RECEIVED,
        WONT_RECEIVED
    }

    private final Connection connection;
    private final Map<Byte, Boolean> telnetClientOptions = new HashMap<>();
    private final Map<Byte, Boolean> telnetServerOptions = new HashMap<>();
    private final byte[] readBuffer;
    private int readBufferPosMark = 0;
    private int readBufferPos = 0;
    private State state = State.READY;
    private boolean subnegotation = false;

    TelnetConnection(Connection connection, int readBufferSize) {
        this.connection = connection;
        readBuffer = new byte[readBufferSize];
    }

    TelnetConnection(Connection connection) {
        this(connection, DEFAULT_READ_BUFFER_SIZE);
    }

    @Override
    public void send(byte[] message) throws IOException {
        connection.send(message);
    }

    @Override
    public void send(byte[] message, int length) throws IOException {
        connection.send(message, length);
    }

    /**
     * Attempts to read bytes from the input stream of the connection. Any bytes that are part of
     * the Telnet Protocol will be stripped off and processed by the Telnet state engine. This means
     * that it is possible that the method call will block even though their appeared to be data
     * available on the stream as the interface contract requires the method to block until at least
     * one byte is successfully read. This method will never read more bytes than the size allocated
     * to the buffer.
     *
     * @param buffer pre-allocated buffer to store the bytes read from the stream into.
     * @return returns the number of bytes written into the buffer; -1 means the end of the stream
     * has ben reached.
     * @throws IOException If something goes wrong with the stream.
     */
    @Override
    public int read(byte[] buffer) throws IOException {
        int result;

        synchronized (readBuffer) {
            // We already reached the end of the stream last time this method was called, no need to do anything
            if (readBufferPosMark == -1) {
                return -1;
            }

            int bufferPos = 0;

            do {
                // If the readBuffer is empty, top it up from the underlying stream
                if (readBufferPos >= readBufferPosMark) {
                    readBufferPosMark = connection.read(readBuffer);
                    readBufferPos = 0;
                }

                // Process the next byte in the read buffer if there is one available
                if (readBufferPos < readBufferPosMark) {
                    byte nextByte = readBuffer[readBufferPos++];
                    Optional<Byte> byteToStream = processByteFromInputStream(nextByte);

                    if (byteToStream.isPresent()) {
                        buffer[bufferPos++] = byteToStream.get();
                    }
                }

                // Keep going while there is still data either in the readBuffer or the underlying
                // inputStream or we haven't written anything yet and we still have space in the
                // result buffer
            } while (bufferPos < buffer.length && (readBufferPos < readBufferPosMark ||
                    (readBufferPosMark > 0 && (connection.readAvailable() > 0 || bufferPos == 0))));

            // If the read call returned 0 or -1 and we had no buffered data to send, send that result instead
            result = bufferPos > 0 ? bufferPos : readBufferPosMark;
        }

        return result;
    }

    private Optional<Byte> processByteFromInputStream(byte byteRead) throws IOException {
        switch (state) {
            case READY:
                return processByteFromReadyState(byteRead);
            case IAC_RECEIVED:
                return processByteFromIacReceivedState(byteRead);
            case WILL_RECEIVED:
                receivedWill(byteRead);
                break;
            case WONT_RECEIVED:
                receivedWont(byteRead);
                break;
            case DO_RECEIVED:
                receivedDo(byteRead);
                break;
            case DONT_RECEIVED:
                receivedDont(byteRead);
                break;
        }

        state = State.READY;
        return Optional.empty();
    }

    private Optional<Byte> processByteFromReadyState(byte byteRead) {
        switch (byteRead) {
            case TelnetCodes.CMD_IAC:
                state = State.IAC_RECEIVED;
                return Optional.empty();
            default:
                return subnegotation ? Optional.empty() : Optional.of(byteRead);
        }
    }

    private Optional<Byte> processByteFromIacReceivedState(byte byteRead) throws IOException {
        switch (byteRead) {
            case TelnetCodes.CMD_WILL:
                state = State.WILL_RECEIVED;
                return Optional.empty();
            case TelnetCodes.CMD_WONT:
                state = State.WONT_RECEIVED;
                return Optional.empty();
            case TelnetCodes.CMD_DO:
                state = State.DO_RECEIVED;
                return Optional.empty();
            case TelnetCodes.CMD_DONT:
                state = State.DONT_RECEIVED;
                return Optional.empty();
            case TelnetCodes.CMD_AYT:
                sendNul();
                state = State.READY;
                return Optional.empty();
            case TelnetCodes.CMD_EC:
                state = State.READY;
                return Optional.of(TelnetCodes.NVT_BS);
            case TelnetCodes.CMD_EL:
                state = State.READY;
                return Optional.of(TelnetCodes.NVT_NAK);
            case TelnetCodes.CMD_SB:
                subnegotation = true;
                state = State.READY;
                return Optional.empty();
            case TelnetCodes.CMD_SE:
                subnegotation = false;
                state = State.READY;
                return Optional.empty();
            case TelnetCodes.CMD_NOP:
            case TelnetCodes.CMD_DM:
            case TelnetCodes.CMD_BRK:
            case TelnetCodes.CMD_IP:
            case TelnetCodes.CMD_AO:
            case TelnetCodes.CMD_GA:
                state = State.READY;
                return Optional.empty();
            default:
                return subnegotation ? Optional.empty() : Optional.of(byteRead);
        }
    }

    private void receivedWill(byte option) throws IOException {
        synchronized(telnetClientOptions) {
            if (telnetClientOptions.get(option) == null) {
                telnetClientOptions.put(option, false);
                sendDont(option);
            }
        }
    }

    private void receivedWont(byte option) throws IOException {
        synchronized(telnetClientOptions) {
            if (telnetClientOptions.get(option) == null) {
                telnetClientOptions.put(option, false);
                sendDont(option);
            }
        }
    }

    private void receivedDo(byte option) throws IOException {
        synchronized(telnetServerOptions) {
            if (telnetServerOptions.get(option) == null) {
                telnetServerOptions.put(option, false);
                sendWont(option);
            }
        }
    }

    private void receivedDont(byte option) throws IOException {
        synchronized(telnetServerOptions) {
            if (telnetServerOptions.get(option) == null) {
                telnetServerOptions.put(option, false);
                sendWont(option);
            }
        }
    }

    private void sendNul() throws IOException {
        final byte[] response = {TelnetCodes.NVT_NUL};
        connection.send(response);
    }

    /*
    private void sendWill(byte option) throws IOException {
        final byte[] response = {TelnetCodes.CMD_IAC, TelnetCodes.CMD_WILL, option};
        connection.send(response);
    }
    */

    private void sendWont(byte option) throws IOException {
        System.out.println(String.format("<- WONT [%d]", option));

        final byte[] response = {TelnetCodes.CMD_IAC, TelnetCodes.CMD_WONT, option};
        connection.send(response);
    }

    /*
    private void sendDo(byte option) throws IOException {
        final byte[] response = {TelnetCodes.CMD_IAC, TelnetCodes.CMD_DO, option};
        connection.send(response);
    }
    */

    private void sendDont(byte option) throws IOException {
        System.out.println(String.format("<- DONT [%d]", option));

        final byte[] response = {TelnetCodes.CMD_IAC, TelnetCodes.CMD_DONT, option};
        connection.send(response);
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }

    @Override
    public int readAvailable() throws IOException {
        int result;

        synchronized (readBuffer) {
            result = connection.readAvailable() + (readBufferPosMark - readBufferPos);
        }

        return result;
    }
}
