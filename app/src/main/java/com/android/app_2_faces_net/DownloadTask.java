package com.android.app_2_faces_net;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.Callable;

public class DownloadTask implements Callable<String> {
    private static final String TAG = "DownloadTask";

    private CryptedSocket codeSenderSocket;

    public DownloadTask(CryptedSocket codeSenderSocket) {
        this.codeSenderSocket = codeSenderSocket;
    }

    @Override
    public String call() {
        try {
            Log.d(TAG, "Starting downloadTask...");

            this.codeSenderSocket.connect();

            String piece = this.codeSenderSocket.read();

            this.codeSenderSocket.close();

            return piece;
        } catch (IOException e) {
            Log.d(TAG, Log.getStackTraceString(e));
            this.codeSenderSocket.close();
        }
        this.codeSenderSocket.close();
        return null;
    }
}
