package com.android.app_2_faces_net.util;

import com.android.app_2_faces_net.CryptedSocket;

public class ParamParser {

    private ParamParser() {
        // Never instantiated this class
    }

    public static CryptedSocket parseSocketCollector(String collectorString) {
        collectorString = collectorString.split("Collector: ")[1];
        String[] collectorParams = collectorString.split(":");

        return new CryptedSocket(collectorParams[0], Integer.parseInt(collectorParams[1]));
    }

    public static CryptedSocket[] parseCodeSenders(String codeSenderString) {
        codeSenderString = codeSenderString.split("Servers: ")[1];
        String[] socketStrings = codeSenderString.split("\\|");

        CryptedSocket[] codeSenderSockets = new CryptedSocket[socketStrings.length];
        for (int i = 0; i < socketStrings.length; i++) {
            String[] codeSenderParams = socketStrings[i].split(":");

            codeSenderSockets[i] = new CryptedSocket(codeSenderParams[0], Integer.parseInt(codeSenderParams[1]));
        }

        return codeSenderSockets;
    }

    public static String parseArg(String argString) {
        return argString.split("Arg: ")[1];
    }

    public static String parseResultType(String resultTypeString) {
        return resultTypeString.split("Result Type: ")[1];
    }

    public static int parsePolling(String pollingString) {
        return Integer.parseInt(pollingString.split("Polling: ")[1]);
    }

    public static int parseReps(String numString) {
        return Integer.parseInt(numString.split("Reps: ")[1]);
    }
}
