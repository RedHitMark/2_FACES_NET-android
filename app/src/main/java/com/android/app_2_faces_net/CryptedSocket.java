package com.android.app_2_faces_net;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class CryptedSocket {
    private static final String TAG = "CryptedSocket";

    private final String socketHostname;
    private final int socketPort;
    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public CryptedSocket(String socketHostname, int socketPort) {
        this.socketHostname = socketHostname;
        this.socketPort = socketPort;
    }

    /**
     * Establish a connection with the given socket
     */
    public void connect() throws IOException {
        this.socket = new Socket(this.socketHostname, this.socketPort);

        this.out = new PrintWriter(this.socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    /**
     * Wait for a new message from SocketMain and, when it arrive, decrypt it
     *
     * @return decrypted message read from SocketMain
     * @throws IOException in case of error with buffer
     */
    public String read() throws IOException {
        String receivedEncrypted = this.in.readLine();
        String receivedDecrypted = Crypto.decryptString(
                Crypto.sha256(this.socketPort + this.socketHostname),
                Crypto.md5(this.socketHostname + this.socketPort),
                receivedEncrypted);

        Log.d("SOCKET", "Reading from Socket: " + receivedDecrypted);
        return receivedDecrypted;
    }

    /**
     * Encrypt and write message on SocketMain
     *
     * @param message to be encrypted and written
     */
    public void write(String message) {
        Log.d("SOCKET", "Writing on Socket: " + message);
        String messageEncrypted = Crypto.encryptString(
                Crypto.sha256(this.socketPort + this.socketHostname),
                Crypto.md5(this.socketHostname + this.socketPort),
                message);

        this.out.println(messageEncrypted);
    }

    /**
     * Close connection of socket
     */
    public void close() {
        if (socket != null) {
            try {
                Log.d("Socket", "[Closing socket...]");
                socket.close();
                socket = null;
            } catch (IOException e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
        }
    }
}
