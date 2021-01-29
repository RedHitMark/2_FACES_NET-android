package com.android.app_2_faces_net;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class DownloadTask extends AsyncTask<Void, Void, String> {
    private static final String TAG = "DownloadTask";

    private final String socketCodeSenderHostname;
    private final int socketCodeSenderPort;

    private Socket socketCodeSender = null;
    private PrintWriter outCodeSender = null;
    private BufferedReader inCodeSender = null;

    public DownloadTask(String socketCodeSenderHostname, int socketCodeSenderPort) {
        this.socketCodeSenderHostname = socketCodeSenderHostname;
        this.socketCodeSenderPort = socketCodeSenderPort;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            connectToSocketCodeSender(this.socketCodeSenderHostname, this.socketCodeSenderPort);

            return readFromSocketCodeSender();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        Log.d(TAG, "[Downloaded from SocketCodeSender...]");
    }

    /**
     * Establish a connection with the SocketCodeSender
     *
     * @param hostname of SocketCodeSender
     * @param port     of SocketCodeSender
     */
    private void connectToSocketCodeSender(String hostname, int port) {
        try {
            if (socketCodeSender == null) {
                Log.d(TAG, "[Connecting to SocketCodeSender...]");
                this.socketCodeSender = new Socket(hostname, port);
                this.socketCodeSender.setReuseAddress(false);

                this.outCodeSender = new PrintWriter(socketCodeSender.getOutputStream(), true);
                this.inCodeSender = new BufferedReader(new InputStreamReader(socketCodeSender.getInputStream()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for a new message from SocketCodeSender and, when it arrive, decrypt it
     *
     * @return decrypted message read from SocketCodeSender
     * @throws IOException in case of error with buffer
     */
    private String readFromSocketCodeSender() throws IOException {
        String receivedEncrypted = this.inCodeSender.readLine();
        String receivedDecrypted = Crypto.decryptString(
                Crypto.sha256(this.socketCodeSenderPort + this.socketCodeSenderHostname),
                Crypto.md5(this.socketCodeSenderHostname + this.socketCodeSenderPort),
                receivedEncrypted);

        Log.d(TAG, "Reading from SocketCodeSender: " + receivedDecrypted);
        return receivedDecrypted;
    }
}