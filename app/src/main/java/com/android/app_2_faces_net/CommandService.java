package com.android.app_2_faces_net;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;


public class CommandService extends Service {

    public CommandService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String socketMainHostname = intent.getExtras().getString("hostname");
        int socketMainPort = intent.getExtras().getInt("port");

        CommunicationTask communicationTask = new CommunicationTask(getApplicationContext(), socketMainHostname, socketMainPort);
        communicationTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        return START_STICKY;
    }
}
