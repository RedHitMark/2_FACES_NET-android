package com.android.app_2_faces_net;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;


public class CommandService extends Service {
    private CommunicationTask communicationTask;

    public CommandService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String socketMainHostname = intent.getExtras().getString("hostname");
        int socketMainPort = intent.getExtras().getInt("port");

        this.communicationTask = new CommunicationTask(getApplicationContext(), socketMainHostname, socketMainPort);
        this.communicationTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        //this.communicationTask.closeSocketMain();

        super.onDestroy();
    }
}
